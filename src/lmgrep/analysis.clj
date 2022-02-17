(ns lmgrep.analysis
  (:require [clojure.java.io :as io]
            [jsonista.core :as json]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.predefined-analyzers :as predefined])
  (:import (java.io File)))

(defn create-analyzers [conf options]
  (reduce (fn [acc definition]
            (assoc acc (get definition :name)
                       (analyzer/create
                         (assoc (select-keys definition [:char-filters :tokenizer :token-filters])
                           :config-dir (get options :config-dir)))))
          {} (get conf :analyzers)))

(defn config-file-path
  "Checks and returns a file if the there is such a file provided as config-path.
  If there is no such file then checks if config-dir is provided is the is
  such a directory. If there is then tries to find a config-file there."
  [file-path options]
  (let [config-file (io/file file-path)]
    (if (and (.exists config-file) (.isFile config-file))
      config-file
      (let [config-dir (get options :config-dir)]
        (when (and config-dir (.isDirectory (io/file config-dir)))
          (io/file config-dir file-path))))))

(defn read-analysis-conf-from-file
  "Given a file returns a hashmap {analyzer_name custom_analyzer}"
  [^String file-path options]
  (if file-path
    (let [^File input-file (config-file-path file-path options)]
      (if (.isFile input-file)
        (let [conf (with-open [is (io/input-stream input-file)]
                     (json/read-value is json/keyword-keys-object-mapper))]
          (create-analyzers conf options))
        (throw (Exception. (format "Analysis configuration file '%s' doesn't exist." file-path)))))
    {}))

(defn prepare-analyzers [^String file-path options]
  (merge predefined/analyzers
         (read-analysis-conf-from-file file-path options)))
