(ns clj-ce.test-runner
  (:require [clojure.test :as t]
            [clj-ce.http-test]
            [clj-ce.core-test]))

; this file is entry point for cljs tests

#?(:cljs
   (defmethod t/report [::t/default :end-run-tests] [m]
     (when-let [exit (if (exists? js/process) js/process.exit)]
       (exit (+ (:fail m) (:error m))))))

#?(:cljs
   (t/run-tests
    'clj-ce.http-test
    'clj-ce.core-test))