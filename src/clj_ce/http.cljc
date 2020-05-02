(ns clj-ce.http
  (:require [clojure.string :as clj-str]
            [clojure.set :refer [map-invert]]))

(def ^:private required #{:ce/spec-version
                          :ce/id
                          :ce/type
                          :ce/source})

(def ^:private optional-v03 #{:ce/data-content-type
                              :ce/data-content-encoding
                              :ce/schema-url
                              :ce/subject
                              :ce/time})

(def ^:private optional-v1 #{:ce/data-content-type
                             :ce/data-schema
                             :ce/subject
                             :ce/time})

(defn ^:private kw->map-entry [kw]
  [kw (str "ce-" (clj-str/replace (name kw) "-" ""))])

(def ^:private ce-v03-field->http-header
  (as-> (clojure.set/union required optional-v03) data
        (map kw->map-entry data)
        (into {} data)
        (assoc data :ce/data-content-type "content-type")))

(def ^:private http-header->ce-field-ce-v03 (map-invert ce-v03-field->http-header))

(def ^:private ce-v1-field->http-header
  (as-> (clojure.set/union required optional-v1) data
        (map kw->map-entry data)
        (into {} data)
        (assoc data :ce/data-content-type "content-type")))

(def ^:private http-header->ce-field-ce-v1 (map-invert ce-v1-field->http-header))

(defn ^:private header->field-by-version
  [version]
  (case version
    "0.3" http-header->ce-field-ce-v03
    "1.0" http-header->ce-field-ce-v1
    nil))

(defn ^:private field->header-by-version
  [version]
  (case version
    "0.3" ce-v03-field->http-header
    "1.0" ce-v1-field->http-header
    nil))

(defn ^:private structured-request?
  [req]
  (-> req
      (:headers)
      (get "content-type" "")
      (clj-str/starts-with? "application/cloudevents+")))

(defn ^:private binary-request?
  [req]
  (contains? (:headers req) "ce-id"))

(defn ^:private ce-request?
  [req]
  (or (structured-request? req)
      (binary-request? req)))

(defn binary-request->event
  "Get cloud event from request in binary format."
  [{:keys [headers body]}]
  (let [version (headers "ce-specversion")
        header->field (header->field-by-version version)
        ce-headers (filter (fn [[header-key]] (and (> (count header-key) 3)
                                                   (clj-str/starts-with? header-key "ce-")))
                           headers)
        rf (fn [event [header-key header-value]]
             (if (contains? header->field header-key)
               (assoc event (header->field header-key) header-value)
               (assoc-in event [:ce/extensions (keyword (subs header-key 3))] header-value)))
        event (reduce rf {} ce-headers)]
    (assoc event :ce/data body)))

(defn structured-request->event
  "Get cloud event from request in structured format."
  [{:keys [headers body]}]
  (throw (#?(:clj UnsupportedOperationException.
             :cljs js/Error. )
           "Structured response is not supported at the time.")))

(defn request->event
  [req]
  (cond
    (binary-request? req)     (binary-request->event req)
    (structured-request? req) (structured-request->event req)
    :else                     nil))


(defn event->binary-request
  "Creates http response for event in binary format."
  [event]
  (let [field->header (field->header-by-version (:ce/spec-version event))
        headers (->> (:ce/extensions event)
                     (map (fn [[k v]] [(str "ce-" (name k)) v]))
                     (into {}))
        headers (->> (dissoc event :ce/extensions)
                     (map (fn [[k v]] [(field->header k) v]))
                     (filter (fn [[k]] k))
                     (into headers))]

    {:headers headers
     :body    (:ce/data event)}))

(defn event->structured-request
  "Creates http response for event in structured format."
  [event]
  (throw (#?(:clj UnsupportedOperationException.
             :cljs js/Error. )
           "Structured response is not supported at the time.")))