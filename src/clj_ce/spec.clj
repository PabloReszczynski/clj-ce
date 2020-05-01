(ns clj-ce.spec
  (:require [clojure.spec.alpha :as s]))

(def required #{:ce/spec-version :ce/id :ce/type :ce/source})

(def optional-v0.3 #{:ce/data-content-type, :ce/data-content-encoding, :ce/schema-url, :ce/subject, :ce/time})
(def optional-v1.0 #{:ce/data-content-type :ce/data-schema :ce/subject :ce/time})

(defmulti event-version :ce/spec-version)
(defmethod event-version "0.3" [_]
  (s/keys :req [:ce/id :ce/spec-version :ce/source :ce/type]
          :opt [:ce/data-content-type :ce/schema-url :ce/subject :ce/data-content-encoding :ce/time]))
(defmethod event-version "1.0" [_]
  (s/keys :req [:ce/id :ce/spec-version :ce/source :ce/type]
          :opt [:ce/data-content-type :ce/data-schema :ce/subject :ce/time]))


(s/def :ce/event (s/multi-spec event-version :ce/spec-version))