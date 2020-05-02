(ns clj-ce.spec
  (:require [clojure.spec.alpha :as s]))

(defmulti event-version :ce/spec-version)
(defmethod event-version "0.3" [_]
  (s/keys :req [:ce/id :ce/spec-version :ce/source :ce/type]
          :opt [:ce/data-content-type :ce/schema-url :ce/subject :ce/data-content-encoding :ce/time :ce/data :ce/extensions]))
(defmethod event-version "1.0" [_]
  (s/keys :req [:ce/id :ce/spec-version :ce/source :ce/type]
          :opt [:ce/data-content-type :ce/data-schema :ce/subject :ce/time :ce/data :ce/extensions]))


(s/def :ce/event (s/multi-spec event-version :ce/spec-version))