(ns lmgrep.cli
  (:require [clojure.tools.cli :as cli]
            [lmgrep.cli.parser :as parser]
            [lmgrep.cli.analysis-conf :as ac]))

(defn prepare-analysis [options]
  (if (empty? (get-in options [:options :analysis]))
    (assoc-in options [:options :analysis] (ac/prepare-analysis-configuration ac/default-text-analysis options))
    options))

(defn remove-text-analysis-flags [options]
  (apply dissoc options ac/analysis-keys))

(defn handle-args [args]
  (-> args
      (cli/parse-opts parser/cli-options)
      prepare-analysis
      (update-in [:options] remove-text-analysis-flags)))

(comment
  (lmgrep.cli/handle-args ["--tokenizer=standard" "--stem?=false" "--stemmer=english" "--case-sensitive?=true"]))
