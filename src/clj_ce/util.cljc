(ns clj-ce.util
  #?(:clj (:import (java.io Writer)
                   (java.net URI))))

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