(ns clj-ce.util)


#?(:cljs (defn parse-uri
           [s]
           s))

#?(:clj (defn parse-uri
          [s]
          (java.net.URI/create s)))