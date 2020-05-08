(ns clj-ce.http-test
  (:require [clojure.test :refer [is deftest]]
            [clj-ce.test-data-binary :refer [data]]
            [clj-ce.http :refer [binary-http->event event->binary-http]]))

#?(:cljs
   (extend-protocol IEquiv
     js/goog.Uri
     (-equiv [o other]
       (= (.toString o)
          (.toString other)))))

(deftest binary-http->event-test
  (doseq [arguments data]
    (let [{:keys [headers body event]} arguments
          e (binary-http->event {:headers headers :body body})]
      (is (= event e)))))

(deftest event->binary-http&back-test
  (doseq [arguments data]
    (let [{:keys [event]} arguments]
      (is (= event (-> event
                       (event->binary-http)
                       (binary-http->event)))))))