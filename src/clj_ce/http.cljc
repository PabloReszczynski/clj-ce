(ns clj-ce.http
  "This namespace contains functions for reading/writing
  CloudEvents from/to http messages.

  **Http message** in this context is a map with `:headers` and `:body` keys.
  (Not unlike `ring` request/response.)
  The *http message* can be then used as http request or response.

  **CloudEvent** is represented by a map
  where keys are namespaced keywords of shape *:ce/[field-name]*.

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

(def ^:private field->header-common
  #:ce{:id                "ce-id"
       :spec-version      "ce-specversion"
       :source            "ce-source"
       :type              "ce-type"
       :subject           "ce-subject"
       :data-content-type "content-type"
       :time              "ce-time"})

(def ^:private field->header-v1
  (conj field->header-common
        [:ce/data-schema "ce-dataschema"]))

(def ^:private field->header-v03
  (conj field->header-common
        [:ce/schema-url "ce-schemaurl"]))

(def ^:private header->field-v1
  (map-invert field->header-v1))

(def ^:private header->field-v03
  (map-invert field->header-v03))

(def ^:private header->field-by-version
  {"0.3" header->field-v03
   "1.0" header->field-v1})

(def ^:private field->deser-fn
  #:ce{:time        deser-time
       :source      deser-uri
       :data-schema deser-uri
       :schema-url  deser-uri})

(defn- header->field&deser-fn-by-version
  "Returns a function that maps http header to a pair [field, deser-fn],
  where `field` is a field of CloudEvent (keyword) to which the http header is mapped to and
  `deser-fn` is a function used to deserialize the header to the field."
  [version]
  (fn [header]
    (if-let [header->field (header->field-by-version version)]
      (if (header->field header)
        [(header->field header)
         (field->deser-fn (header->field header) identity)]))))

(def ^:private field->header-by-version
  {"0.3" field->header-v03
   "1.0" field->header-v1})

(def ^:private field->ser-fn
  #:ce{:time        ser-time
       :source      ser-uri
       :data-schema ser-uri
       :schema-url  ser-uri})

(defn- field->header&ser-fn-by-version
  "Returns a function that maps CloudEvent field to pair [header, ser-fn],
  where `header` is a name of a header to which the filed (keyword) is mapped to and
  `ser-fn` is a function used to serialize the field to the header."
  [version]
  (fn [field]
    (if-let [field->header (field->header-by-version version)]
      (if (field->header field)
        [(field->header field)
         (field->ser-fn field identity)]))))

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
  (let [header->field&deser-fn (header->field&deser-fn-by-version version)]
    (fn [event [header-key header-value]]
      (cond
       ;; ce field
        (header->field&deser-fn header-key)
        (let [[field deser-fn] (header->field&deser-fn header-key)]
          (assoc event field (deser-fn header-value)))

       ;; ce extension field
        (and (starts-with? header-key "ce-") (> (count header-key) 3))
       (let [ext-field (subs header-key 3)
             deser-fn (ext-deser-fns ext-field identity)]
         (assoc-in event [:ce/extensions (keyword ext-field)] (deser-fn header-value)))

       ;; non ce header
        :else
        event))))

(defn binary-msg->event
  "Creates CloudEvent from the *http-msg*.

  Options are key-value pairs, valid options are:

  *:extensions-fns*  `map[string,function]`. A map of functions used
  to deserialize extension fields from headers.

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
  "Creates http message in binary mode from the *event*. Options are
  key-value pairs, valid options are:

  *:extensions-fns*  `map[keyword,function]`. A map of functions used to
  serialize extension fields to headers.

  "
  {:doc/format :markdown}
  [event & options]
  (let [{:keys [extensions-fns]
         :or   {extensions-fns {}}} options
        field->header&ser-fn (field->header&ser-fn-by-version (:ce/spec-version event))
        headers (->> (:ce/extensions event)
                     (map (fn [[k v]]
                            [(str "ce-" (name k)) ((extensions-fns k identity) v)]))
                     (into {}))
        headers (->> (dissoc event :ce/extensions)
                     (keep (fn [[field-key field-value]]
                             (if-let [[header-key ser-fn] (field->header&ser-fn field-key)]
                               [header-key (ser-fn field-value)])))
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
  "Creates CloudEvent from http message in structured mode."
  {:doc/format :markdown}
  [http-msg deserializers]
  (let [{:keys [headers body]} http-msg
        [format encoding] (parse-content-type (headers "content-type"))
        deserialize-fn (deserializers format)]
    (deserialize-fn body encoding)))

(defn event->structured-msg
  "Creates http message in structured mode from an event."
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