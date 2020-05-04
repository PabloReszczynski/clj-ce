(defproject clj-ce "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
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