(ns clj-ce.json-test
  (:require [clojure.test :refer [is deftest]]
            [clj-ce.json :refer [cloudevent->json cloudevent->json]]
            #?@(:clj ([clojure.data.json :refer [read-str]]))
            [clj-ce.util :as util]))

(defn data->map
  [data]
  #?(:clj
     (-> ^bytes data
         (String.)
         (read-str))
     :cljs
     (-> data
         (js/JSON.parse)
         (js->clj))))

(def ^:private octet-stream-event
  #:ce{:id                "1"
       :source            (util/parse-uri "http://localhost/%20source")
       :type              "mock.test"
       :spec-version      "to be set"
       :data-content-type "application/octet-stream"
       :data              #?(:clj  (byte-array [0xDE 0xAD 0xBE 0xEF])
                             :cljs (js/Uint8Array. [0xDE, 0xAD, 0xBE, 0xEF]))})

(deftest cloudevent->json-test-data-v1
  (let [data (-> (assoc octet-stream-event :ce/spec-version "1.0")
                 (cloudevent->json)
                 (data->map)
                 (get "data_base64"))]

    (is (= "3q2+7w==" data))))

(deftest cloudevent->json-test-data-v03
  (let [js-obj (-> (assoc octet-stream-event :ce/spec-version "0.3")
                   (cloudevent->json)
                   (data->map))]

    (do (is (= "3q2+7w==" (js-obj "data")))
        (is (= "base64" (js-obj "datacontentencoding"))))))