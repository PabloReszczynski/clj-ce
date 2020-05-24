(defproject clj-ce "0.1.0-SNAPSHOT"
  :description "Clojure(Script) library designed to handle CloudEvents."
  :url "https://github.com/matejvasek/clj-ce"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.753"]
                 [org.clojure/data.json "1.0.0"]]
  :profiles {:dev {:dependencies [[viebel/codox-klipse-theme "0.0.5"]]
                   :plugins [[lein-codox "0.10.7"]]}}
  :global-vars {*warn-on-reflection* true}
  :repl-options {:init-ns clj-ce.core}
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-cljfmt "0.6.7"]]
  :codox {:namespaces [clj-ce.http clj-ce.spec clj-ce.json clj-ce.core]
          :metadata {:doc/format :markdown}
          :output-path "docs"
          :themes [:default [:klipse
                             {:klipse/external-libs
                              "https://raw.githubusercontent.com/matejvasek/clj-ce/master/src/"
                              :klipse/require-statement
                              "(ns my.test
                              (:require [clj-ce.http :as ce-http :refer [binary-msg->event
                                                                         event->binary-msg
                                                                         structured-msg->event
                                                                         event->structured-msg]]
                                        [clj-ce.json :as ce-json :refer [json->cloudevent
                                                                         cloudevent->json]]))"}]]}
  :cljsbuild {:builds
              [{:id "test"
                :source-paths ["src" "test"]
                :compiler {:main clj-ce.test-runner
                           :optimizations :simple
                           :target :nodejs
                           :output-dir "resources/public/js/out/test"
                           :output-to "resources/public/js/test.js"}}]})