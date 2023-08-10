(ns lmgrep.predefined-analyzers
  (:require [clojure.string :as str])
  (:import (java.util ServiceLoader)
           (org.apache.lucene.analysis Analyzer)))

(set! *warn-on-reflection* true)

(defn load-analyzers []
  (let [iterator (-> Analyzer (ServiceLoader/load) (.iterator))]
    (reduce (fn [acc ^Analyzer analyzer]
              (let [^Class class (.getClass analyzer)
                    class-name (.getName class)
                    simple-name (.getSimpleName class)
                    short-name (str/replace simple-name #"Analyzer$" "")]
                (assoc acc (str/lower-case class-name) analyzer
                           (str/lower-case simple-name) analyzer
                           (str/lower-case short-name) analyzer))) {} (iterator-seq iterator))))

(def analyzers (load-analyzers))
