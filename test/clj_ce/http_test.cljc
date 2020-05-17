(ns clj-ce.http-test
  (:require [clojure.test :refer [is deftest]]
            [clj-ce.test-data-binary :as bin-data]
            [clj-ce.test-data-structured :as struct-data]
            [clj-ce.http :as ce-http]
            [clj-ce.json :as j]))

#?(:cljs
   (extend-protocol IEquiv
     js/goog.Uri
     (-equiv [o other]
       (= (.toString o)
          (.toString other)))))

(deftest binary-http->event-test
  (doseq [arguments bin-data/data]
    (let [{:keys [headers body event]} arguments
          e (ce-http/binary-msg->event {:headers headers :body body})]
      (is (= event e)))))

(deftest event->binary-http&back-test
  (doseq [arguments bin-data/data]
    (let [{:keys [event]} arguments]
      (is (= event (-> event
                       (ce-http/event->binary-msg)
                       (ce-http/binary-msg->event)))))))

(deftest structured-http->event&back-test-utf8
  (doseq [arguments struct-data/data]
    (let [{:keys [event]} arguments]
      (is (= event (-> event
                       (ce-http/event->structured-msg "json" j/cloudevent->json "utf-8")
                       (ce-http/structured-msg->event {"json" j/json->cloudevent})))))))

#?(:clj
   (do (deftest structured-http->event-test-utf8
         (doseq [arguments struct-data/data]
           (let [{:keys [headers body event]} arguments
                 body (.getBytes ^String body "UTF-8")
                 headers (update headers "content-type" #(str % "; charset=utf-8"))
                 e (ce-http/structured-msg->event {:headers headers :body body}
                                                  {"json" j/json->cloudevent})]
             (is (= event e)))))

       (deftest structured-http->event-test-iso-8859-2
         (doseq [arguments struct-data/data]
           (let [{:keys [headers body event]} arguments
                 body (.getBytes ^String body "ISO-8859-2")
                 headers (update headers "content-type" #(str % "; charset=iso-8859-2"))
                 e (ce-http/structured-msg->event {:headers headers :body body}
                                                  {"json" j/json->cloudevent})]
             (is (= event e))))))

   :cljs
   (do (deftest structured-http->event-test-str-body
         (doseq [arguments struct-data/data]
           (let [{:keys [headers body event]} arguments
                 headers (update headers "content-type" #(str % "; charset=utf-8"))
                 e (ce-http/structured-msg->event {:headers headers :body body}
                                                  {"json" j/json->cloudevent})]
             (is (= event e)))))

       (deftest structured-http->event-test-utf8-Uint8Array
         (doseq [arguments struct-data/data]
           (let [{:keys [headers body event]} arguments
                 body (.encode (js/TextEncoder. "utf-8") body)
                 headers (update headers "content-type" #(str % "; charset=utf-8"))
                 e (ce-http/structured-msg->event {:headers headers :body body}
                                                  {"json" j/json->cloudevent})]
             (is (= event e)))))

       (deftest structured-http->event-test-utf8-ArrayBuffer
         (doseq [arguments struct-data/data]
           (let [{:keys [headers body event]} arguments
                 body (.-buffer (.encode (js/TextEncoder. "utf-8") body))
                 headers (update headers "content-type" #(str % "; charset=utf-8"))
                 e (ce-http/structured-msg->event {:headers headers :body body}
                                                  {"json" j/json->cloudevent})]
             (is (= event e)))))

       (when (resolve 'js/Buffer)
         (deftest structured-http->event-test-utf8-Buffer
           (doseq [arguments struct-data/data]
             (let [{:keys [headers body event]} arguments
                   body (js/Buffer.from body "utf8")
                   headers (update headers "content-type" #(str % "; charset=utf-8"))
                   e (ce-http/structured-msg->event {:headers headers :body body}
                                                    {"json" j/json->cloudevent})]
               (is (= event e))))))))