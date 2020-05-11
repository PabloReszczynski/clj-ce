(ns clj-ce.test-data-structured
  (:require [clj-ce.util :refer [parse-uri]]))

(def data
  [{:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"schemaurl\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"data\": {},
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :schema-url (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"schemaurl\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"data\": {},
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\",
            \"astring\": \"aaa\",
            \"aboolean\": true,
            \"anumber\": 10}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :schema-url (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :extensions {:astring "aaa", :aboolean true, :anumber 10}
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"schemaurl\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"datacontentencoding\": \"base64\",
            \"data\": \"e30=\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :schema-url (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"schemaurl\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"datacontentencoding\": \"base64\",
            \"data\": \"e30=\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\",
            \"astring\": \"aaa\",
            \"aboolean\": true,
            \"anumber\": 10}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :schema-url (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :extensions {:astring "aaa", :aboolean true, :anumber 10}
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"application/xml\",
            \"data\": \"<stuff></stuff>\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "application/xml"
                :data "<stuff></stuff>"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"application/xml\",
            \"datacontentencoding\": \"base64\",
            \"data\": \"PHN0dWZmPjwvc3R1ZmY+\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "application/xml"
                :data "<stuff></stuff>"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"text/plain\",
            \"data\": \"Hello World Vašek!\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "text/plain"
                :data "Hello World Vašek!"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"0.3\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"text/plain\",
            \"datacontentencoding\": \"base64\",
            \"data\": \"SGVsbG8gV29ybGQgVmHFoWVrIQ==\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "0.3"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "text/plain"
                :data "Hello World Vašek!"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"dataschema\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"data\": {},
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :data-schema (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"dataschema\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"data\": {},
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\",
            \"astring\": \"aaa\",
            \"aboolean\": true,
            \"anumber\": 10}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :data-schema (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :extensions {:astring "aaa", :aboolean true, :anumber 10}
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"dataschema\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"data_base64\": \"e30=\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :data-schema (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"dataschema\": \"http://localhost/schema\",
            \"datacontenttype\": \"application/json\",
            \"data_base64\": \"e30=\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\",
            \"astring\": \"aaa\",
            \"aboolean\": true,
            \"anumber\": 10}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :data-schema (parse-uri "http://localhost/schema")
                :subject "sub"
                :data-content-type "application/json"
                :extensions {:astring "aaa", :aboolean true, :anumber 10}
                :data "{}"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"application/xml\",
            \"data\": \"<stuff></stuff>\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "application/xml"
                :data "<stuff></stuff>"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"application/xml\",
            \"data_base64\": \"PHN0dWZmPjwvc3R1ZmY+\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "application/xml"
                :data "<stuff></stuff>"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"text/plain\",
            \"data\": \"Hello World Vašek!\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "text/plain"
                :data "Hello World Vašek!"}}

   {:headers {"content-type" "application/cloudevents+json"}
    :body "{\"specversion\": \"1.0\",
            \"id\": \"1\",
            \"type\": \"mock.test\",
            \"source\": \"http://localhost/source\",
            \"datacontenttype\": \"text/plain\",
            \"data_base64\": \"SGVsbG8gV29ybGQgVmHFoWVrIQ==\",
            \"subject\": \"sub\",
            \"time\": \"2018-04-26T14:48:09+02:00\"}"
    :event #:ce{:id "1"
                :source (parse-uri "http://localhost/source")
                :type "mock.test"
                :spec-version "1.0"
                :time #inst "2018-04-26T14:48:09+02:00"
                :subject "sub"
                :data-content-type "text/plain"
                :data "Hello World Vašek!"}}])