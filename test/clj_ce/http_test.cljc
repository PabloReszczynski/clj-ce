(ns clj-ce.http-test
  (:require [clojure.test :refer [is deftest]]
            [clj-ce.http :refer [binary-http->event event->binary-http]]
            [clj-ce.util :refer [parse-uri]]))

#?(:cljs
   (extend-protocol IEquiv
     js/goog.Uri
     (-equiv [o other]
       (= (.toString o)
          (.toString other)))))

(def test-data-for-binary-format
  [{
    :headers {
              "ce-specversion" "0.3"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "ignored"        "ignored"
              }
    :body    nil
    :event   #:ce{
                  :id           "1"
                  :source       (parse-uri "http://localhost/source")
                  :type         "mock.test"
                  :spec-version "0.3"
                  }
    }
   {
    :headers {
              "ce-specversion" "0.3"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "ce-schemaurl"   "http://localhost/schema"
              "content-type"   "application/json"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ignored"        "ignored"
              }
    :body    "{}"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "0.3"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :schema-url        (parse-uri "http://localhost/schema")
                  :subject           "sub"
                  :data-content-type "application/json"
                  :data              "{}"
                  }
    }
   {
    :headers {
              "ce-specversion" "0.3"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "ce-schemaurl"   "http://localhost/schema"
              "content-type"   "application/json"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ce-astring"     "aaa"
              "ce-aboolean"    "true"
              "ce-anumber"     "10"
              "ignored"        "ignored"
              }
    :body    "{}"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "0.3"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :schema-url        (parse-uri "http://localhost/schema")
                  :subject           "sub"
                  :data-content-type "application/json"
                  :extensions        {:astring "aaa", :aboolean "true", :anumber "10"}
                  :data              "{}"
                  }
    }
   {
    :headers {
              "ce-specversion" "0.3"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "content-type"   "application/xml"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ignored"        "ignored"
              }
    :body    "<stuff></stuff>"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "0.3"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :subject           "sub"
                  :data-content-type "application/xml"
                  :data              "<stuff></stuff>"
                  }
    }
   {
    :headers {
              "ce-specversion" "0.3"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "content-type"   "text/plain"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ignored"        "ignored"
              }
    :body    "Hello World Lorena!"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "0.3"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :subject           "sub"
                  :data-content-type "text/plain"
                  :data              "Hello World Lorena!"
                  }
    }
   {
    :headers {
              "ce-specversion" "1.0"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "ignored"        "ignored"
              }
    :body    nil
    :event   #:ce{
                  :id           "1"
                  :source       (parse-uri "http://localhost/source")
                  :type         "mock.test"
                  :spec-version "1.0"
                  }
    }
   {
    :headers {
              "ce-specversion" "1.0"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "ce-dataschema"  "http://localhost/schema"
              "content-type"   "application/json"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ignored"        "ignored"
              }
    :body    "{}"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "1.0"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :data-schema       (parse-uri "http://localhost/schema")
                  :subject           "sub"
                  :data-content-type "application/json"
                  :data              "{}"
                  }
    }
   {
    :headers {
              "ce-specversion" "1.0"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "ce-dataschema"  "http://localhost/schema"
              "content-type"   "application/json"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ce-astring"     "aaa"
              "ce-aboolean"    "true"
              "ce-anumber"     "10"
              "ignored"        "ignored"
              }
    :body    "{}"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "1.0"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :data-schema       (parse-uri "http://localhost/schema")
                  :subject           "sub"
                  :data-content-type "application/json"
                  :extensions        {:astring "aaa", :aboolean "true", :anumber "10"}
                  :data              "{}"
                  }
    }
   {
    :headers {
              "ce-specversion" "1.0"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "content-type"   "application/xml"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ignored"        "ignored"
              }
    :body    "<stuff></stuff>"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "1.0"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :subject           "sub"
                  :data-content-type "application/xml"
                  :data              "<stuff></stuff>"
                  }
    }
   {
    :headers {
              "ce-specversion" "1.0"
              "ce-id"          "1"
              "ce-type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "content-type"   "text/plain"
              "ce-subject"     "sub"
              "ce-time"        "2018-04-26T14:48:09+02:00"
              "ignored"        "ignored"
              }
    :body    "Hello World Lorena!"
    :event   #:ce{
                  :id                "1"
                  :source            (parse-uri "http://localhost/source")
                  :type              "mock.test"
                  :spec-version      "1.0"
                  :time              #inst "2018-04-26T14:48:09+02:00"
                  :subject           "sub"
                  :data-content-type "text/plain"
                  :data              "Hello World Lorena!"
                  }
    }
   {
    :headers {
              "Ce-sPecversion" "0.3"
              "cE-id"          "1"
              "CE-Type"        "mock.test"
              "ce-source"      "http://localhost/source"
              "ignored"        "ignored"
              "ab"             "should-not-break-anything"
              }
    :body    nil
    :event   #:ce{
                  :id           "1"
                  :source       (parse-uri "http://localhost/source")
                  :type         "mock.test"
                  :spec-version "0.3"
                  }
    }])

(deftest binary-http->event-test
  (doseq [arguments test-data-for-binary-format]
    (let [{:keys [headers body event]} arguments
          e (binary-http->event {:headers headers :body body})]
      (is (= event e)))))

(deftest event->binary-http&back-test
  (doseq [arguments test-data-for-binary-format]
    (let [{:keys [event]} arguments]
      (is (= event (-> event
                       (event->binary-http)
                       (binary-http->event)))))))