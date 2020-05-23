(ns clj-ce.test-runner
  (:require [clojure.test :as t]
            [clj-ce.core-test]
            [clj-ce.json-test]
            [clj-ce.http-test]))

; this file is entry point for cljs tests

#?(:cljs
   (defmethod t/report [::t/default :end-run-tests] [m]
     (when-let [exit (if (exists? js/process) js/process.exit)]
       (exit (+ (:fail m) (:error m))))))

#?(:cljs
   (t/run-tests
     'clj-ce.core-test
     'clj-ce.json-test
     'clj-ce.http-test))