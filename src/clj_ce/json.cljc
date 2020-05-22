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
            [clojure.set :refer [map-invert]]
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

(def ^:private js-field->clj-field-common
  {"id"              :ce/id
   "specversion"     :ce/spec-version
   "source"          :ce/source
   "type"            :ce/type
   "subject"         :ce/subject
   "datacontenttype" :ce/data-content-type
   "time"            :ce/time
   "data"            :ce/data
   "data_base64"     :ce/data})

(def ^:private js-field->clj-field-v1
  (conj js-field->clj-field-common
        ["dataschema" :ce/data-schema]))

(def ^:private js-field->clj-field-v03
  (conj js-field->clj-field-common
        ["schemaurl" :ce/schema-url]
        ["datacontentencoding" ::transient]))

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
  [field-value js-obj]
  (let [dce (js-obj "datacontentencoding")]
    (cond

      (and (= dce "base64")
           (string? field-value))
      (decode-base-64 field-value)

      (string? field-value)
      field-value

      :else
      #?(:clj  (json/write-str field-value)
         :cljs (js/JSON.stringify (clj->js field-value))))))

(defn- deser-data-base-64
  [field-value _]
  (decode-base-64 field-value))

(def ^:private js-field->deser-fn
  {"time"        deser-time
   "source"      deser-uri
   "dataschema"  deser-uri
   "schemaurl"   deser-uri
   "data_base64" deser-data-base-64
   "data"        deser-data})

(def ^:private js-field->clj-field-by-version
  {"1.0" js-field->clj-field-v1
   "0.3" js-field->clj-field-v03})

(defn- js-field->clj-field&deser-fn#by-version
  "Returns a function that maps JSON field to a pair [field, deser-fn],
  where `field` is a field of CloudEvent (keyword) to which the JSON field is mapped to, and
  `deser-fn` is a function used to deserialize the JSON value to the field."
  [version]
  (let [js-field->clj-field (js-field->clj-field-by-version version)]
    (fn [js-field]
      (let [clj-field (js-field->clj-field js-field)
            deser-fn (js-field->deser-fn js-field (fn [x & _] x))]
        (when clj-field
          [clj-field deser-fn])))))

(def ^:private clj-field->js-field-v1
  (map-invert js-field->clj-field-v1))

(def ^:private clj-field->js-field-v03
  (map-invert js-field->clj-field-v03))

(defn- ser-uri
  [clj-field-value _]
  (str clj-field-value))

(defn- ser-time
  [clj-field-value _]
  (util/ser-time clj-field-value))

#?(:clj
   (defn is->base64-str [is]
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

(defn- ser-data
  [data event]
  (let [{:ce/keys [data-content-type]} event
        charset (charset-from-data-content-type data-content-type)]
    (cond
      (starts-with? data-content-type "text/")
      data

      (or (nil? data-content-type)
          (starts-with? data-content-type "application/json"))
      data

      (starts-with? data-content-type "application/octet-stream")
      (encode-base-64 (->binary data))

      :else
      data)))

(def ^:private clj-field->ser-fn
  #:ce{:time        ser-time
       :source      ser-uri
       :data-schema ser-uri
       :schema-url  ser-uri
       :data        ser-data})

(def ^:private clj-field->js-field#by-version
  {"1.0" clj-field->js-field-v1
   "0.3" clj-field->js-field-v03})

(defn- clj-field->js-field&ser-fn#by-version
  "Returns a function that maps CloudEvent field to pair [field, ser-fn],
  where `field` is a JSON field to which the CloudEvent field (keyword) is mapped to, and
  `ser-fn` is a function used to serialize the field to the JSON value."
  [version]
  (let [clj-field->js-field (clj-field->js-field#by-version version)]
    (fn [clj-field]
      (let [js-field (clj-field->js-field clj-field)
            ser-fn (clj-field->ser-fn clj-field (fn [x & _] x))]
        (when js-field
          [js-field ser-fn])))))

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
        js-field->clj-field&deser-fn (js-field->clj-field&deser-fn#by-version (js-obj "specversion"))
        rf (fn [event [js-field js-value]]
             (if-let [[clj-field deser-fn] (js-field->clj-field&deser-fn js-field)]
               (if (= clj-field ::transient)
                 event
                 (assoc event clj-field (deser-fn js-value js-obj)))
               (assoc-in event [:ce/extensions (keyword js-field)] js-value)))]
    (reduce rf {} js-obj)))

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
        clj-field->js-field&ser-fn (clj-field->js-field&ser-fn#by-version (:ce/spec-version event))
        fields (->> event
                    (keep (fn [[clj-field clj-value]]
                            (if-let [[js-field ser-fn] (clj-field->js-field&ser-fn clj-field)]
                              [js-field (ser-fn clj-value event)])))
                    (into {}))
        extensions (->> (:ce/extensions event)
                        (map (fn [[k v]] [(name k) v]))
                        (into {}))]
    (#?(:clj  #(write-json % charset)
        :cljs #(js/JSON.stringify (clj->js %)))
      (merge fields extensions))))

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
     (->binary [data] (jio/input-stream data))
     InputStream
     (->binary [stream] stream)
     String
     (->binary [s] (jio/input-stream (.getBytes s StandardCharsets/UTF_8) "UTF-8"))
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
