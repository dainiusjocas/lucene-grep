(ns lmgrep.cli
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]))

(def format-options #{:edn :json :string})

(def tokenizers #{:keyword :letter :standard :unicode-whitespace :whitespace})

(def stemmers #{:kp
                :portuguese
                :lithuanian
                :german2
                :porter
                :danish
                :norwegian
                :catalan
                :irish
                :romanian
                :basque
                :russian
                :dutch
                :estonian
                :finnish
                :turkish
                :italian
                :english
                :lovins
                :swedish
                :german
                :spanish
                :french
                :arabic
                :hungarian
                :armenian})

(defn options-to-str [options]
  (print-str (mapv name (sort options))))

(def cli-options
  [[nil "--tokenizer TOKENIZER" (str "Tokenizer to use, one of: " (options-to-str tokenizers))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? format-options %) (str "Tokenizer must be one of: " (options-to-str tokenizers))]]
   [nil "--case-sensitive? CASE_SENSITIVE" "If text should be case sensitive"
    :parse-fn #(Boolean/parseBoolean %)
    :default false]
   [nil "--ascii-fold? ASCII_FOLDED" "If text should be ascii folded"
    :parse-fn #(Boolean/parseBoolean %)
    :default true]
   [nil "--stem? STEMMED" "If text should be stemmed"
    :parse-fn #(Boolean/parseBoolean %)
    :default true]
   [nil "--stemmer STEMMER" (str "Which stemmer to use for token stemming, one of: " (options-to-str stemmers))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? format-options %) (str "Stemmer must be one of: " (options-to-str stemmers))]]
   [nil "--format FORMAT" (str "How the output should be formatted, one of: " (options-to-str format-options))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? format-options %) (str "Format must be one of: " (options-to-str format-options))]]
   [nil "--template TEMPLATE" "The template for the output string, e.g.: file={{file}} line-number={{line-number}} line={{line}}"]
   [nil "--pre-tags PRE_TAGS" "A string that the highlighted text is wrapped in, use in conjunction with --post-tags"]
   [nil "--post-tags POST_TAGS" "A string that the highlighted text is wrapped in, use in conjunction with --pre-tags"]
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
  (lmgrep.cli/handle-args ["test"])
  (lmgrep.cli/handle-args ["--format=edn"]))
