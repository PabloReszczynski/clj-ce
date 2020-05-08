(ns clj-ce.core-test
  (:require [clojure.test :refer [is deftest]]
            [clojure.test :refer []]
            [clj-ce.core :refer []]))

(deftest test-ci
         (is (= 1 2) "Fail deliberately."))