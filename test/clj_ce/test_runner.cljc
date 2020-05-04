(ns clj-ce.test-runner
  (:require [clojure.test :refer [run-tests]]
            [clj-ce.http-test]
            [clj-ce.core-test]))

(run-tests
  'clj-ce.http-test
  'clj-ce.core-test)