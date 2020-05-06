(ns clj-ce.http
  (:require [clj-ce.util :refer [parse-uri]]
            [clojure.string :refer [starts-with?]]
            [clojure.set :refer [map-invert]]
            #?(:clj [clojure.instant :refer [read-instant-date]]))
  #?(:clj (:import (java.time Instant))))



(defn ^:private deser-time
  [s]
  #?(:clj  (read-instant-date s)
     :cljs (js/Date. s)))

(defn ^:private ser-time
  [inst]
  #?(:clj  (.toString ^Instant (Instant/ofEpochMilli (inst-ms inst)))
     :cljs (.toISOString (js/Date. (inst-ms inst)))))

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
  (merge field->header-common
         #:ce{:data-schema "ce-dataschema"}))

(def ^:private field->header-v03
  (merge field->header-common
         #:ce{:schema-url            "ce-schemaurl"
              :data-content-encoding "ce-datacontentencoding"}))

(def ^:private header->field-v1
  (map-invert field->header-v1))

(def ^:private header->field-v03
  (map-invert field->header-v03))

(def ^:private field->deser-fn
  #:ce{:time        deser-time
       :source      deser-uri
       :data-schema deser-uri
       :schema-url  deser-uri})

(def ^:private field->ser-fn
  #:ce{:time        ser-time
       :source      ser-uri
       :data-schema ser-uri
       :schema-url  ser-uri})

(def ^:private header->field-by-version
  {"0.3" header->field-v03
   "1.0" header->field-v1})

(def ^:private field->header-by-version
  {"0.3" field->header-v03
   "1.0" field->header-v1})


(defn ^:private header->field&deser-fn-by-version
  [version]
  (fn [header]
    (if-let [header->field (header->field-by-version version)]
      (if (header->field header)
        [(header->field header)
         (field->deser-fn (header->field header) identity)]))))

(defn ^:private field->header&ser-fn-by-version
  [version]
  (fn [field]
    (if-let [field->header (field->header-by-version version)]
      (if (field->header field)
        [(field->header field)
         (field->ser-fn field identity)]))))

(defn ^:private structured-http?
  [req]
  (-> req
      (:headers)
      (get "content-type" "")
      (starts-with? "application/cloudevents+")))

(defn ^:private binary-http?
  [req]
  (contains? (:headers req) "ce-id"))

(defn ^:private ce-http?
  [req]
  (or (structured-http? req)
      (binary-http? req)))

(defn binary-http->event
  "Get cloud event from request/response in binary format."
  [{:keys [headers body]}]
  (let [headers (->> headers
                     (map (fn [[k v]]
                            [(.toLowerCase ^String k) v]))
                     (into headers))
        version (headers "ce-specversion")
        header->field&deser-fn (header->field&deser-fn-by-version version)
        rf (fn [event [header-key header-value]]
             (if (header->field&deser-fn header-key)
               (let [[field deser-fn] (header->field&deser-fn header-key)]
                 (assoc event field (deser-fn header-value)))
               (if (and (> (count header-key) 3)
                        (starts-with? header-key "ce-"))
                 (assoc-in event [:ce/extensions (keyword (subs header-key 3))] header-value)
                 event)))
        event (reduce rf {} headers)]
    (if body
      (assoc event :ce/data body)
      event)))

(defn structured-http->event
  "Get cloud event from request/response in structured format."
  [{:keys [headers body]}]
  (throw (#?(:clj  UnsupportedOperationException.
             :cljs js/Error.)
           "Structured messages is not supported at the time.")))

(defn http->event
  [req]
  (cond
    (binary-http? req) (binary-http->event req)
    (structured-http? req) (structured-http->event req)
    :else nil))


(defn event->binary-http
  "Creates http request/response for event in binary format."
  [event]
  (let [field->header&ser-fn (field->header&ser-fn-by-version (:ce/spec-version event))
        headers (->> (:ce/extensions event)
                     (map (fn [[k v]] [(str "ce-" (name k)) v]))
                     (into {}))
        headers (->> (dissoc event :ce/extensions)
                     (keep (fn [[field-key field-value]]
                             (if-let [[header-key ser-fn] (field->header&ser-fn field-key)]
                               [header-key (ser-fn field-value)])))
                     (into headers))]

    {:headers headers
     :body    (:ce/data event)}))

(defn event->structured-http
  "Creates http request/response for event in structured format."
  [event]
  (throw (#?(:clj  UnsupportedOperationException.
             :cljs js/Error.)
           "Structured messages is not supported at the time.")))