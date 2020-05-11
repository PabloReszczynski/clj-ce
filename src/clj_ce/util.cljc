(ns clj-ce.util
  (:require #?(:clj [clojure.instant :refer [read-instant-date]]))
  #?(:clj (:import (java.io Writer)
                   (java.net URI)
                   (java.time Instant))))

#?(:clj
   (defmethod print-method URI
     [uri ^Writer w]
     (.write w (str "#uri \"" (.toString ^URI uri) "\""))))

#?(:cljs
   (extend-protocol IPrintWithWriter
     js/goog.Uri
     (-pr-writer [uri writer _]
       (write-all writer "#uri \"" (.toString uri) "\""))))

(defn parse-uri
  [s]
  #?(:clj  (URI/create s)
     :cljs (js/goog.Uri.parse s)))

(defn deser-time
  [s]
  #?(:clj  (read-instant-date s)
     :cljs (js/Date. s)))

(defn ser-time
  [inst]
  #?(:clj  (.toString ^Instant (Instant/ofEpochMilli (inst-ms inst)))
     :cljs (.toISOString (js/Date. (inst-ms inst)))))