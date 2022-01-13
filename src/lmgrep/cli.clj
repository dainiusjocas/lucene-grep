(ns lmgrep.cli
  (:require [clojure.tools.cli :as cli]
            [lmgrep.cli.parser :as parser]
            [lmgrep.lucene.analysis-conf :as ac]))

(defn handle-args [args]
  (let [opts (cli/parse-opts args parser/cli-options)]
    (if (empty? (get-in opts [:options :analysis]))
      (assoc-in opts [:options :analysis] (ac/prepare-analysis-configuration ac/default-text-analysis opts))
      opts)))

(comment
  (lmgrep.cli/handle-args ["--tokenizer=standard" "--stem?=false" "--stemmer=english" "--case-sensitive?=true"]))
