(ns clj-ce.json
  "This namespace contains functions for (de)serializing CloudEvents from/to JSON.
  It's mainly intended for handling http messages in structured mode with JSON.

  Examples:
  ```
  (clj-ce.http/event->structured-msg event
                                     \"json\"
                                     clj-ce.json/cloudevent->json
                                     \"utf-8\")

  (ce-clj-ce.http/structured-msg->event http-msg
                                        {\"json\" clj-ce.json/json->cloudevent})
  ```"

  {:doc/format :markdown}
  (:require [clj-ce.util :as util]
            [clojure.set]
            [clojure.string :refer [starts-with? index-of]]
            #?@(:clj ([clojure.data.json :as json]
                      [clojure.java.io :as jio]))
            #?(:cljs [goog.crypt.base64 :as b64]))
  #?(:clj (:import (java.io InputStream
                            ByteArrayInputStream
                            InputStreamReader
                            PushbackReader
                            StringReader
                            ByteArrayOutputStream
                            OutputStreamWriter Reader)
                   (java.util Base64)
                   (java.nio.charset StandardCharsets))))

(defprotocol ^{:doc/format :markdown} CharacterData
  "Abstract various object types that can be interpreted
   as a sequence of characters (e.g. js/Uint8Array, java.io.InputStream or java.lang.String).

  This protocol has default implementations for:

  * java.io.Reader
  * java.lang.String
  * java.io.InputStream
  * byte[]
  * js/string
  * js/ArrayBuffer
  * js/Uint8Array
  * nil

  The `:body` of `http message` should satisfy this protocol when used with this module."

  (^{:doc/format :markdown} ->text [this charset]
    "Transforms data to sequence of characters.

    For Clojure returns `java.io.Reader`.

    For ClojureScript returns `string`."))

(defprotocol ^{:doc/format :markdown} BinaryData
  "Abstract various object types that can be interpreted
  as a sequence of bytes (js/Uint8Array or java.io.InputStream).

  This protocol has default implementations for:

  * byte[]
  * java.io.InputStream
  * java.lang.String
  * java.io.Reader
  * js/string
  * js/Uint8Array
  * js/ArrayBuffer
  * nil

  The `:ce/data` of `CloudEvent` should satisfy this protocol when used with this module."
  (^{:doc/format :markdown} ->binary [this]
    "Transforms data to sequence of bytes.

    For Clojure this should return `java.io.InputStream`.

    For ClojureScript this should return `js/Uint8Array`."))

(def ^:private js-field->attribute-common
  {"id"              :ce/id
   "specversion"     :ce/spec-version
   "source"          :ce/source
   "type"            :ce/type
   "subject"         :ce/subject
   "datacontenttype" :ce/data-content-type
   "time"            :ce/time
   "data"            :ce/data
   "data_base64"     :ce/data})

(def ^:private js-field->attribute-v1
  (conj js-field->attribute-common
        ["dataschema" :ce/data-schema]))

(def ^:private js-field->attribute-v03
  (conj js-field->attribute-common
        ["schemaurl" :ce/schema-url]
        ["datacontentencoding" ::transient]))

(def ^:private standard-attributes-by-version
  {"1.0" (into #{} (map (fn [[k v]] k)) js-field->attribute-v1)
   "0.3" (into #{} (map (fn [[k v]] k)) js-field->attribute-v03)})

(defn- create#standard-attribute?
  [version]
  (let [standard-attributes (standard-attributes-by-version version)]
    (fn [[k _]] (boolean (standard-attributes k)))))

(defn- deser-uri
  [js-field-value _]
  (util/parse-uri js-field-value))

(defn- deser-time
  [js-field-value _]
  (util/deser-time js-field-value))

(defn- decode-base-64
  "Decodes string containing data encoded as base64 into byte array.

  For Clojure returns byte[].

  For ClojureScript returns Uint8Array."
  [s]
  #?(:clj  (.decode (Base64/getDecoder) ^String s)
     :cljs (b64/decodeStringToUint8Array s)))

(defn- deser-data
  [attribute-value js-obj]
  (let [dce (js-obj "datacontentencoding")]
    (cond

      (and (= dce "base64")
           (string? attribute-value))
      (decode-base-64 attribute-value)

      (string? attribute-value)
      attribute-value

      :else
      #?(:clj  (json/write-str attribute-value)
         :cljs (js/JSON.stringify (clj->js attribute-value))))))

(defn- deser-data-base-64
  [attribute-value _]
  (decode-base-64 attribute-value))

(def ^:private js-field->deser-fn
  {"time"        deser-time
   "source"      deser-uri
   "dataschema"  deser-uri
   "schemaurl"   deser-uri
   "data_base64" deser-data-base-64
   "data"        deser-data})

(def ^:private js-field->attribute-by-version
  {"1.0" js-field->attribute-v1
   "0.3" js-field->attribute-v03})

(defn- create#js-field->attribute
  "Returns a function of type: ([js-field:string, js-value:any] -> [attribute:keyword, value:any]),
  where: `js-field` is a name of JSON field,
         `js-value` is the value of the field,
         `attribute` is an attribute of a CloudEvent, and
         `value` is the value of the attribute."
  [js-obj]
  (let [js-field->attribute (js-field->attribute-by-version (js-obj "specversion"))]
    (fn [[js-field js-value]]
      (let [attribute (js-field->attribute js-field)
            deser-fn (js-field->deser-fn js-field (fn [x & _] x))]
        (when (and attribute
                   (not= attribute ::transient))
          [attribute (deser-fn js-value js-obj)])))))

(defn- ser-uri
  [attribute-value _]
  (str attribute-value))

(defn- ser-time
  [attribute-value _]
  (util/ser-time attribute-value))

#?(:clj
   (defn- is->base64-str [is]
     (let [bos (ByteArrayOutputStream.)]
       (with-open [os (.wrap (Base64/getEncoder) bos)]
         (jio/copy is os))
       (String. (.toByteArray bos) StandardCharsets/US_ASCII))))

(defn- encode-base-64
  "Encodes binary data (java.io.InputStream, js/Uint8Array)
  into base64 string."
  [bin]
  #?(:clj  (is->base64-str bin)
     :cljs (b64/encodeByteArray bin)))

(defn- charset-from-data-content-type
  [data-content-type]
  (when data-content-type
    (-> (re-find #"^.*;\s*charset=(.+).*$" data-content-type)
        (second)
        (or "utf-8"))))


(defn- create#val->js-fields
  "Helper function that creates a function of type: (attr-value:any -> [[js-field:string, js-value:any]]).

  The function returns a collection containing single pair [js-field js-value],
  where `js-field` is name of a JSON attribute and `js-value` is value of the attribute."
  [js-field & [ser-fn]]
  (let [ser-fn (or ser-fn (fn [x & _] x))]
    (fn [clj-value & [clj-obj]]
      [[js-field (ser-fn clj-value clj-obj)]])))

(def ^:private attribute->js-fields#common
  #:ce{:data-content-type (create#val->js-fields "datacontenttype"),
       :id                (create#val->js-fields "id"),
       :subject           (create#val->js-fields "subject"),
       :time              (create#val->js-fields "time" ser-time),
       :spec-version      (create#val->js-fields "specversion"),
       :source            (create#val->js-fields "source" ser-uri),
       :type              (create#val->js-fields "type")})

(defn- data->js-fields-v1
  [data & [clj-obj]]
  (let [{:ce/keys [data-content-type]} clj-obj
        charset (charset-from-data-content-type data-content-type)]
    (cond
      (starts-with? data-content-type "text/")
      [["data" data]]
      (or (nil? data-content-type)
          (starts-with? data-content-type "application/json"))
      [["data" data]]
      (starts-with? data-content-type "application/octet-stream")
      [["data_base64" (encode-base-64 (->binary data))]]
      :else
      [["data" data]])))

(def ^:private attribute->js-fields#v1
  (conj attribute->js-fields#common
        [:ce/data-schema (create#val->js-fields "dataschema" ser-uri)]
        [:ce/data data->js-fields-v1]))

(defn- data->js-fields-v03
  [data & [clj-obj]]
  (let [{:ce/keys [data-content-type]} clj-obj]
    (cond
      (starts-with? data-content-type "text/")
      [["data" data]]
      (or (nil? data-content-type)
          (starts-with? data-content-type "application/json"))
      [["data" data]]
      (starts-with? data-content-type "application/octet-stream")
      [["data" (encode-base-64 (->binary data))]
       ["datacontentencoding" "base64"]]
      :else
      [["data" data]])))

(def ^:private attribute->js-fields#v03
  (conj attribute->js-fields#common
        [:ce/schema-url (create#val->js-fields "schemaurl" ser-uri)]
        [:ce/data data->js-fields-v03]))

(def ^:private attribute->js-fields#by-version
  {"1.0" attribute->js-fields#v1
   "0.3" attribute->js-fields#v03})

(defn- create#attribute->js-fields
  "Creates a function of type: ([attribute:keyword, attr-val:any] -> [[js-field:string, js-value:any]]),
  where: `attribute` is an attribute of a CloudEvent,
         `attr-val` is a value of the attribute,
         `js-field` is a name of a JSON field, and
         `js-value` is a value of the field."
  [event]
  (let [attribute->js-fields (attribute->js-fields#by-version (:ce/spec-version event))]
    (fn [[attribute clj-value]]
      (when-let [val->js-fields (attribute->js-fields attribute)]
        (val->js-fields clj-value event)))))

(defn- data->text
  [data & [charset]]
  (->text data charset))

(defn- data->obj
  "Transforms data to a clojure map representing JS object."
  [data & [charset]]
  #?(:clj  (json/read (PushbackReader. (data->text data charset)))
     :cljs (js->clj (js/JSON.parse (data->text data charset)))))

(defn json->cloudevent
  "Converts JSON to CloudEvent.

  The `data` parameter must satisfy the [[CharacterData]] protocol.

  See also [[clj-ce.http/structured-msg->event]]."
  {:doc/format :markdown}
  [data & [charset]]
  {:pre [(satisfies? CharacterData data) (or (nil? charset) (string? charset))]}
  (let [js-obj (data->obj data charset)
        standard-attribute? (create#standard-attribute? (js-obj "specversion"))
        {attributes true
         extensions false} (group-by standard-attribute? js-obj)
        js-field->attribute (create#js-field->attribute js-obj)
        extensions (into {} (map (fn [[k v]] [(keyword k) v])) extensions)
        attributes (into {} (map js-field->attribute) attributes)]
    (if (not (empty? extensions))
      (assoc attributes :ce/extensions extensions)
      attributes)))

#?(:clj
   (defn- write-json
     [m charset]
     (let [bos (ByteArrayOutputStream.)]
       (with-open [writer (OutputStreamWriter. bos ^String charset)]
         (json/write m writer :escape-unicode true))
       (.toByteArray bos))))

(defn cloudevent->json
  "Converts CloudEvent to JSON.

  If binary data is carried by the event then
  the `:ce/data` of the `event` parameter must satisfy the [[BinaryData]] protocol.

  See also [[clj-ce.http/event->structured-msg]]."
  {:doc/format :markdown}
  [event & options]
  (let [{:keys [charset]
         :or   {charset "utf-8"}} options
        extensions (into {} (map (fn [[k v]] [(name k) v])) (:ce/extensions event))
        attributes (into extensions (mapcat (create#attribute->js-fields event)) event)]
    (#?(:clj  #(write-json % charset)
        :cljs #(js/JSON.stringify (clj->js %)))
      attributes)))

#?(:clj
   (extend-protocol CharacterData
     (Class/forName "[B")
     (->text [arr charset] (jio/reader arr :encoding charset))
     InputStream
     (->text [is charset] (jio/reader is :encoding charset))
     String
     (->text [s _] (StringReader. ^String s))
     Reader (->text [reader _] reader)
     nil
     (->text [_ _] (StringReader. "")))

   :cljs
   (extend-protocol CharacterData
     string
     (->text [data _] data)
     js/Uint8Array
     (->text [data charset] (.decode (js/TextDecoder. charset) data))
     js/ArrayBuffer
     (->text [data charset] (.decode (js/TextDecoder. charset) (js/Uint8Array. data)))
     nil
     (->text [_ _] "")))

#?(:clj
   (extend-protocol BinaryData
     (Class/forName "[B")
     (->binary [data] (jio/input-stream data :encoding "UTF-8"))
     InputStream
     (->binary [stream] stream)
     String
     (->binary [s] (jio/input-stream (.getBytes s StandardCharsets/UTF_8) :encoding "UTF-8"))
     Reader
     (->binary [reader] (jio/input-stream (.getBytes (slurp reader "UTF-8") StandardCharsets/UTF_8)))
     nil
     (->binary [_] (jio/input-stream (byte-array 0))))

   :cljs
   (extend-protocol BinaryData
     js/Uint8Array
     (->binary [arr] arr)
     js/ArrayBuffer
     (->binary [arr-buff] (.-buffer arr-buff))))
