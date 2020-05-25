(ns clj-ce.http
  "This namespace contains functions for reading/writing
  CloudEvents from/to http messages.

  **Http message** in this context is a map with `:headers` and `:body` keys.
  (Not unlike `ring` request/response.)
  The *http message* can be then used as http request or response.

  **CloudEvent** is represented by a map
  where keys are namespaced keywords of shape *:ce/[attribute-name]*.

  For instance
  ``` clojure
  #:ce{:id \"42\",
       :spec-version \"1.0\",
       :type \"my.type\",
       :source \"http://example.com/\"}
  ```

  Examples:
  ``` clojure
  (clj-http.client/post \"http://localhost/\"
                        (event->binary-msg #:ce{:id \"42\"
                                                :spec-version \"1.0\"
                                                :type \"my.type\"
                                                :source \"http://example.com/\"}))

  (defn ring-echo-handler
    [req]
    (-> req
        (binary-msg->event)
        (event->binary-msg)
        (assoc :status 200)))
  ```"
  {:doc/format :markdown}
  (:require [clj-ce.util :refer [parse-uri ser-time deser-time]]
            [clojure.string :refer [starts-with? index-of trim split lower-case]]
            [clojure.set :refer [map-invert]])
  #?(:clj (:import (java.time Instant))))

(def ^:private ser-uri str)

(def ^:private deser-uri parse-uri)

(def ^:private attribute->header-common
  #:ce{:id                "ce-id"
       :spec-version      "ce-specversion"
       :source            "ce-source"
       :type              "ce-type"
       :subject           "ce-subject"
       :data-content-type "content-type"
       :time              "ce-time"})

(def ^:private attribute->header-v1
  (conj attribute->header-common
        [:ce/data-schema "ce-dataschema"]))

(def ^:private attribute->header-v03
  (conj attribute->header-common
        [:ce/schema-url "ce-schemaurl"]))

(def ^:private header->attribute-v1
  (map-invert attribute->header-v1))

(def ^:private header->attribute-v03
  (map-invert attribute->header-v03))

(def ^:private header->attribute-by-version
  {"0.3" header->attribute-v03
   "1.0" header->attribute-v1})

(def ^:private attribute->deser-fn
  #:ce{:time        deser-time
       :source      deser-uri
       :data-schema deser-uri
       :schema-url  deser-uri})

(defn- header->attribute&deser-fn-by-version
  "Returns a function of type: (header:string -> [attribute:keyword, deser-fn:(string -> any)]),
  where `attribute` is an attribute of CloudEvent) to which the http header is mapped to, and
  `deser-fn` is a function used to deserialize the header to the attribute."
  [version]
  (fn [header]
    (if-let [header->attribute (header->attribute-by-version version)]
      (if (header->attribute header)
        [(header->attribute header)
         (attribute->deser-fn (header->attribute header) identity)]))))

(def ^:private attribute->header-by-version
  {"0.3" attribute->header-v03
   "1.0" attribute->header-v1})

(def ^:private attribute->ser-fn
  #:ce{:time        ser-time
       :source      ser-uri
       :data-schema ser-uri
       :schema-url  ser-uri})

(defn- attribute->header&ser-fn-by-version
  "Returns a function of type: (attribute:keyword -> [header:string, ser-fn:(any -> string)]),
  where `header` is a name of a header to which the attribute (keyword) is mapped to, and
  `ser-fn` is a function used to serialize the attribute to the header."
  [version]
  (fn [attribute]
    (if-let [attribute->header (attribute->header-by-version version)]
      (if (attribute->header attribute)
        [(attribute->header attribute)
         (attribute->ser-fn attribute identity)]))))

(defn- structured-msg?
  [http-msg]
  (let [content-type (get-in http-msg [:headers "content-type"] "")]
    (starts-with? content-type "application/cloudevents+")))

(defn- binary-msg?
  [http-msg]
  (contains? (:headers http-msg) "ce-id"))

(defn- ce-msg?
  [http-msg]
  (or (structured-msg? http-msg)
      (binary-msg? http-msg)))

(defn- create-rf
  "Creates a reduce function that that reduces headers into a CloudEvent."
  [version ext-deser-fns]
  (let [header->attribute&deser-fn (header->attribute&deser-fn-by-version version)]
    (fn [event [header-key header-value]]
      (cond
        ;; ce attribute
        (header->attribute&deser-fn header-key)
        (let [[attribute deser-fn] (header->attribute&deser-fn header-key)]
          (assoc event attribute (deser-fn header-value)))

        ;; ce extension attribute
        (and (starts-with? header-key "ce-") (> (count header-key) 3))
        (let [ext-attribute (subs header-key 3)
              deser-fn (ext-deser-fns ext-attribute identity)]
          (assoc-in event [:ce/extensions (keyword ext-attribute)] (deser-fn header-value)))

        ;; non ce header
        :else
        event))))

(defn binary-msg->event
  "Creates CloudEvent from the *http-msg*.
  Options are key-value pairs, valid options are:

  *:extensions-fns*  `map[string,function]`. A map of functions used
  to deserialize extension attributes from headers.

  ~~~klipse
  (binary-msg->event {:headers {\"ce-specversion\" \"0.3\"
                                \"ce-id\"          \"1\"
                                \"ce-type\"        \"mock.test\"
                                \"ce-source\"      \"http://localhost/source\"
                                \"ce-answer\"      \"42\"}
                      :body    \"Hello World!\"}
                      :extensions-fns {\"answer\" js/parseInt})
  ~~~

  "
  {:doc/format :markdown}
  [http-msg & options]
  (let [{:keys [headers body]} http-msg
        {:keys [extensions-fns]
         :or   {extensions-fns {}}} options
        headers (->> headers
                     (map (fn [[k v]]
                            [(lower-case k) v]))
                     (into headers))
        version (headers "ce-specversion")
        rf (create-rf version extensions-fns)
        event (reduce rf {} headers)]
    (if body
      (assoc event :ce/data body)
      event)))

(defn event->binary-msg
  "Creates http message in binary mode from the *event*.
  Options are key-value pairs, valid options are:

  *:extensions-fns*  `map[keyword,function]`. A map of functions used to
  serialize extension attributes to headers.

  ~~~klipse
  (event->binary-msg #:ce{:spec-version \"0.3\",
                          :id           \"1\",
                          :type         \"mock.test\",
                          :source       (js/goog.Uri. \"http://localhost/source\"),
                          :extensions   {:answer 42},
                          :data         \"Hello World!\"}
                     :extensions-fns {:answer #(.toString %)})
  ~~~

  "
  {:doc/format :markdown}
  [event & options]
  (let [{:keys [extensions-fns]
         :or   {extensions-fns {}}} options
        attribute->header&ser-fn (attribute->header&ser-fn-by-version (:ce/spec-version event))
        headers (->> (:ce/extensions event)
                     (map (fn [[k v]]
                            [(str "ce-" (name k)) ((extensions-fns k identity) v)]))
                     (into {}))
        headers (->> (dissoc event :ce/extensions)
                     (keep (fn [[attribute-key attribute-value]]
                             (if-let [[header-key ser-fn] (attribute->header&ser-fn attribute-key)]
                               [header-key (ser-fn attribute-value)])))
                     (into headers))]

    {:headers headers
     :body    (:ce/data event)}))

(defn- parse-content-type
  "Returns format and charset of CloudEvent
  from content-type header of http message in structured mode.

  For instance for \"application/cloudevents+json; charset=utf-8\"
  returns [\"json\", \"utf-8\"]."
  [content-type]
  (let [[format-part charset-part] (split content-type #";")
        format-start (some-> (index-of format-part "+") inc)
        type (if (and format-part format-start)
               (subs format-part format-start)
               nil)
        charset (second (split (or charset-part "charset=ISO-8859-1") #"="))
        format (some-> type trim)
        charset (some-> charset trim)]
    (if format
      [format charset]
      ["application/octet-stream" nil])))

(defn structured-msg->event
  "Creates CloudEvent from http message in structured mode.

  ~~~klipse
  (structured-msg->event {:headers {\"content-type\" \"application/cloudevents+json\"}
                          :body    \"{\\\"specversion\\\": \\\"0.3\\\",
                                      \\\"id\\\": \\\"1\\\",
                                      \\\"type\\\": \\\"mock.test\\\",
                                      \\\"source\\\": \\\"http://localhost/source\\\"}\"}
                         {\"json\" clj-ce.json/json->cloudevent})
  ~~~

  "
  {:doc/format :markdown}
  [http-msg deserializers]
  (let [{:keys [headers body]} http-msg
        [format encoding] (parse-content-type (headers "content-type"))
        deserialize-fn (deserializers format)]
    (deserialize-fn body encoding)))

(defn event->structured-msg
  "Creates http message in structured mode from an event.

  ~~~klipse
  (event->structured-msg #:ce{:id                \"1\"
                          :source            (js/goog.Uri. \"http://localhost/source\")
                          :type              \"mock.test\"
                          :spec-version      \"0.3\"
                          :time              #inst \"2018-04-26T14:48:09+02:00\"
                          :schema-url        (js/goog.Uri. \"http://localhost/schema\")
                          :subject           \"sub\"
                          :data-content-type \"application/json\"
                          :data              {:message \"Hello!\"}}
                       \"json\"
                        clj-ce.json/cloudevent->json
                       \"utf-8\")
  ~~~
  "
  {:doc/format :markdown}
  [event format-name serialize-fn charset]
  {:headers {"content-type" (str "application/cloudevents+" format-name "; charset=" charset)}
   :body    (serialize-fn event)})

(defn msg->event
  "Creates CloudEvent from http message in either binary or structured mode."
  {:doc/format :markdown}
  [http-msg serializers]
  (cond
    (binary-msg? http-msg) (binary-msg->event http-msg)
    (structured-msg? http-msg) (structured-msg->event http-msg serializers)
    :else nil))