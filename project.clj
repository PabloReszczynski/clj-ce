(defproject clj-ce "0.1.0-SNAPSHOT"
  :description "Clojure(Script) library designed to handle CloudEvents."
  :url "https://github.com/matejvasek/clj-ce"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.753"]]
  :global-vars {*warn-on-reflection* true}
  :repl-options {:init-ns clj-ce.core}
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {:builds
              [{:id "test"
                :source-paths ["src" "test"]
                :compiler {:main clj-ce.test-runner
                           :optimizations :simple
                           :target :nodejs
                           :output-dir "resources/public/js/out/test"
                           :output-to "resources/public/js/test.js"}}]})