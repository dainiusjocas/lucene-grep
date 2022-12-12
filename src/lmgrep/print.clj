(ns lmgrep.print
  (:require [clojure.stacktrace :as cst])
  (:import (java.io PrintWriter)))

(defn to-err [^String s]
  (.println System/err s))

(defn to-writer
  ([^PrintWriter writer]
   (.println writer))
  ([^PrintWriter writer ^String s]
   (.println writer s)))

(defn throwable [^Throwable t]
  (cst/print-stack-trace t))
