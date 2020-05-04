(ns clj-ce.util)


#?(:cljs (defn parse-uri
           [s]
           (js/goog.Uri.parse s)))

#?(:clj (defn parse-uri
          [s]
          (java.net.URI/create s)))