(ns lmgrep.print
  (:import (java.io PrintWriter)))

(defn to-err [^String s]
  (.println System/err s))

(defn to-writer
  ([^PrintWriter writer]
   (.println writer))
  ([^PrintWriter writer ^String s]
   (.println writer s)))
