(ns clj-ce.spec
  "This namespace contains clojure.spec for CloudEvent (registered as `:ce/event`).

  Example:
  ```
  (defn foo
    [event]
    {:pre [(s/valid? :ce/event event)]}
    nil)
  ```"
  {:doc/format :markdown}
  (:require [clojure.spec.alpha :as s]))

(s/def :ce/data-schema uri?)
(s/def :ce/time inst?)
(s/def :ce/data-content-type string?)
(s/def :ce/schema-url uri?)
(s/def :ce/data-content-encoding string?)
(s/def :ce/type string?)
(s/def :ce/source uri?)
(s/def :ce/id string?)
(s/def :ce/extensions map?)
(s/def :ce/spec-version #{"0.3" "1.0"})
(s/def :ce/subject string?)
(s/def :ce/data any?)

(defmulti ^:private event-version :ce/spec-version)
(defmethod event-version "0.3" [_]
  (s/keys :req [:ce/id :ce/spec-version :ce/source :ce/type]
          :opt [:ce/data-content-type :ce/schema-url :ce/subject :ce/data-content-encoding :ce/time :ce/data :ce/extensions]))
(defmethod event-version "1.0" [_]
  (s/keys :req [:ce/id :ce/spec-version :ce/source :ce/type]
          :opt [:ce/data-content-type :ce/data-schema :ce/subject :ce/time :ce/data :ce/extensions]))

(s/def :ce/event (s/multi-spec event-version :ce/spec-version))