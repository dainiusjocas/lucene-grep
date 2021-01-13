(ns lmgrep.cli
  (:require [clojure.tools.cli :as cli]))

(def cli-options
  [[nil "--tokenizer TOKENIZER" "Tokenizer to use, one of: [keyword, letter, standard, unicode-whitespace, whitespace]"
    :parse-fn #(keyword %)]
   [nil "--case-sensitive? CASE_SENSITIVE" "If text should be case sensitive"
    :parse-fn #(Boolean/parseBoolean %)
    :default false]
   [nil "--ascii-fold? ASCII_FOLDED" "If text should be ascii folded"
    :parse-fn #(Boolean/parseBoolean %)
    :default true]
   [nil "--stem? STEMMED" "If text should be stemmed"
    :parse-fn #(Boolean/parseBoolean %)
    :default true]
   [nil "--stemmer STEMMER" "Which stemmer to use for token stemming, one of: [arabic, armenian, basque, catalan, danish, dutch, english (default), estonian, finnish, french, german2, german, hungarian, irish, italian, kp, lithuanian, lovins, norwegian, porter, portuguese, romanian, russian, spanish, swedish, turkish]"
    :parse-fn #(keyword %)]
   ;[nil "--slop SLOP" "How far can be words from each other"
   ; :parse-fn #(Integer/parseInt %)
   ; :default 0]
   ;[nil "--in-order? IN_ORDER" "Should the phrase be ordered in matches with a non-zero slop"
   ; :parse-fn #(Boolean/parseBoolean %)
   ; :default false]
   ["-h" "--help"]])

(defn handle-args [args]
  (cli/parse-opts args cli-options))

(comment
  (lmgrep.cli/handle-args ["--tokenizer=standard" "--stem?=false" "--stemmer=english" "--case-sensitive?=true"])
  (lmgrep.cli/handle-args ["test"]))
