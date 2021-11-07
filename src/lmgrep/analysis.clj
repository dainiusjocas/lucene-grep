(ns lmgrep.analysis
  (:require [clojure.java.io :as io]
            [jsonista.core :as json]
            [lmgrep.lucene.analyzer :as analyzer])
  (:import (java.io File)))

(defn create-analyzers [conf options]
  (reduce (fn [acc definition]
            (assoc acc (get definition :name)
                       (analyzer/custom-analyzer
                         (assoc (select-keys definition [:char-filters :tokenizer :token-filters])
                           :config-dir (get options :config-dir)))))
          {} (get conf :analyzers)))

(defn read-analysis-conf-from-file
  "Given a file returns a hashmap {analyzer_name custom_analyzer}"
  [^String file-path options]
  (let [^File input-file (io/file file-path)]
    (if (.isFile input-file)
      (let [conf (with-open [is (io/input-stream input-file)]
                   (json/read-value is json/keyword-keys-object-mapper))]
        (create-analyzers conf options))
      (throw (Exception. (format "Analysis configuration file '%s' doesn't exist." file-path))))))
