(ns lmgrep.cli.parser
  (:require [clojure.string :as str]
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

(def query-parsers #{:classic :complex-phrase :surround :simple :standard})

(def presearchers #{:no-filtering :term-filtered :multipass-term-filtered})

(defn read-json [json-string]
  (json/read-value json-string json/keyword-keys-object-mapper))

(def cli-options
  [["-q" "--query QUERY"
    "Lucene query string(s). If specified then all the positional arguments are interpreted as files."
    :multi true
    :update-fn conj]

   [nil "--query-parser QUERY_PARSER" (str "Which query parser to use, one of: " (options-to-str query-parsers))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? query-parsers %) (str "Query parser must be one of: " (options-to-str query-parsers))]]

   [nil "--queries-file QUERIES_FILE"
    "A file path to the Lucene query strings with their config. If specified then all the positional arguments are interpreted as files."]
   [nil "--queries-index-dir QUERIES_INDEX_DIR"
    "A directory where Lucene Monitor queries are stored."]
   [nil "--tokenizer TOKENIZER" (str "Tokenizer to use, one of: " (options-to-str tokenizers))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? tokenizers %) (str "Tokenizer must be one of: " (options-to-str tokenizers))]]
   [nil "--case-sensitive? CASE_SENSITIVE" "If text should be case sensitive"
    :parse-fn #(Boolean/parseBoolean %)]
   [nil "--ascii-fold? ASCII_FOLDED" "If text should be ascii folded"
    :parse-fn #(Boolean/parseBoolean %)]
   [nil "--stem? STEMMED" "If text should be stemmed"
    :parse-fn #(Boolean/parseBoolean %)]
   [nil "--stemmer STEMMER" (str "Which stemmer to use for token stemming, one of: " (options-to-str stemmers))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? stemmers %) (str "Stemmer must be one of: " (options-to-str stemmers))]]
   [nil "--presearcher PRESEARCHER" (str "Which Lucene Monitor Presearcher to use, one of: " (options-to-str presearchers))
    :parse-fn #(keyword (str/lower-case %))
    :validate [#(contains? presearchers %) (str "Query parser must be one of: " (options-to-str presearchers))]
    :default "no-filtering"]
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
   [nil "--[no-]hidden" "Search in hidden files. Default: true."
    :default true]
   [nil "--max-depth N" "In case of a recursive GLOB, how deep to search for input files."
    :parse-fn #(Integer/parseInt %)]
   [nil "--with-empty-lines" "When provided on the input that does not match write an empty line to STDOUT."
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
    :parse-fn #(Integer/parseInt %)]
   [nil "--show-analysis-components" "Just print-out the available analysis components in JSON."
    :default false]
   [nil "--only-analyze" "When provided output will be analyzed text."
    :default false]
   [nil "--explain" "Modifies --only-analyze. Output is detailed token info, similar to Elasticsearch Analyze API."]
   [nil "--graph" "Modifies --only-analyze. Output is a string that can be fed to the `dot` program."]
   [nil "--analysis ANALYSIS" "The analysis chain configuration"
    :parse-fn read-json
    :default {}]
   [nil "--query-parser-conf CONF" "The configuration for the query parser."
    :parse-fn read-json]
   [nil "--concurrency CONCURRENCY" "How many concurrent threads to use for processing."
    :parse-fn #(Integer/parseInt %)
    :validate [(fn [value] (< 0 value)) "Must be > 0"]
    :default 8]
   [nil "--queue-size SIZE" "Number of lines read before being processed"
    :parse-fn #(Integer/parseInt %)
    :validate [(fn [value] (< 0 value)) "Must be > 0"]
    :default 1024]
   [nil "--reader-buffer-size BUFFER_SIZE" "Buffer size of the BufferedReader in bytes."
    :parse-fn #(Integer/parseInt %)]
   [nil "--writer-buffer-size BUFFER_SIZE" "Buffer size of the BufferedWriter in bytes."
    :parse-fn #(Integer/parseInt %)]
   [nil "--[no-]preserve-order" "If the input order should be preserved."
    :default true]
   [nil "--config-dir DIR" "A base directory from which to load text analysis resources, e.g. synonym files. Default: current dir."]
   [nil "--analyzers-file FILE"
    "A file that contains definitions of text analyzers. Works in combinations with --config-dir flag."
    :multi true
    :update-fn conj]
   [nil "--query-update-buffer-size NUMBER" "Number of queries to be buffered in memory before being committed to the queryindex. Default 100000."]
   [nil "--streamed" "Listens on STDIN for json with both query and a piece of text to be analyzed" :default false]
   ["-h" "--help"]])
