(ns lmgrep.cli
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [jsonista.core :as json]))

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
  [["-q" "--query QUERY"
    "Lucene query string(s). If specified then all the positional arguments are interpreted as files."
    :multi true
    :update-fn conj]
   [nil "--queries-file QUERIES_FILE"
    "A file path to the Lucene query strings with their config. If specified then all the positional arguments are interpreted as files."]
   [nil "--tokenizer TOKENIZER" (str "Tokenizer to use, one of: " (options-to-str tokenizers))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? tokenizers %) (str "Tokenizer must be one of: " (options-to-str tokenizers))]]
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
    :validate [#(contains? stemmers %) (str "Stemmer must be one of: " (options-to-str stemmers))]]
   [nil "--with-score" "If the matching score should be computed"]
   [nil "--format FORMAT" (str "How the output should be formatted, one of: " (options-to-str format-options))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? format-options %) (str "Format must be one of: " (options-to-str format-options))]]
   [nil "--template TEMPLATE" "The template for the output string, e.g.: file={{file}} line-number={{line-number}} line={{line}}"]
   [nil "--pre-tags PRE_TAGS" "A string that the highlighted text is wrapped in, use in conjunction with --post-tags"]
   [nil "--post-tags POST_TAGS" "A string that the highlighted text is wrapped in, use in conjunction with --pre-tags"]
   [nil "--excludes EXCLUDES" "A GLOB that filters out files that were matched with a GLOB"]
   [nil "--skip-binary-files" "If a file that is detected to be binary should be skipped. Available for Linux and MacOS only."
    :default false]
   [nil "--with-empty-lines" "When provided on the input that doesn't match write an empty line to STDOUT."
    :default false]
   [nil "--with-scored-highlights" "ALPHA: Instructs to highlight with scoring."
    :default false]
   [nil "--[no-]split" "If a file (or STDIN) should be split by newline."
    :default true]
   [nil "--hyperlink" "If a file should be printed as hyperlinks."
    :default false]
   [nil "--with-details" "For JSON and EDN output adds raw highlights list."
    :default false]
   [nil "--word-delimiter-graph-filter WDGF" "WordDelimiterGraphFilter configurationFlags as per https://lucene.apache.org/core/7_4_0/analyzers-common/org/apache/lucene/analysis/miscellaneous/WordDelimiterGraphFilter.html"
    :parse-fn #(Integer/parseInt %)
    :default 0]
   [nil "--only-analyze" "When provided output will be analyzed text."
    :default false]
   [nil "--analysis ANALYSIS" "The analysis chain configuration"
    :parse-fn #(json/read-value % json/keyword-keys-object-mapper)
    :default {}]
   ["-h" "--help"]])

(defn handle-args [args]
  (cli/parse-opts args cli-options))

(comment
  (lmgrep.cli/handle-args ["--tokenizer=standard" "--stem?=false" "--stemmer=english" "--case-sensitive?=true"])
  (lmgrep.cli/handle-args ["test"])
  (lmgrep.cli/handle-args ["test" "-q" "foo" "--query=bar"])
  (lmgrep.cli/handle-args ["test" "-q" "foo" "--queries-file=README.md"])
  (lmgrep.cli/handle-args ["--format=edn"])
  (lmgrep.cli/handle-args ["--excludes=**.edn"])
  (lmgrep.cli/handle-args ["--with-score"]))
