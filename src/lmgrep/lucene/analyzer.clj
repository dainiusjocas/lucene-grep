(ns lmgrep.lucene.analyzer
  (:require [clojure.string :as str]
            [lmgrep.features :as features]
            [lmgrep.lucene.predefined-analyzers :as lucene.predefined])
  (:import (java.util HashMap Map)
           (java.io File)
           (java.nio.file Path)
           (org.apache.lucene.analysis.custom CustomAnalyzer CustomAnalyzer$Builder)
           (org.apache.lucene.analysis.util TokenizerFactory TokenFilterFactory CharFilterFactory)
           (org.apache.lucene.analysis Analyzer)))

; https://lucene.apache.org/core/8_8_0/analyzers-common/constant-values.html#org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_WORD_PARTS

(set! *warn-on-reflection* true)

(defn namify
  "Normalizes analysis component name."
  [component-name]
  (str/lower-case component-name))

(defn stringify [m]
  (reduce (fn [acc [k v]] (assoc acc (name k) (str v))) {} m))

(def tokenizer-name->class
  (reduce (fn [acc ^String tokenizer-name]
            (assoc acc (namify tokenizer-name) (TokenizerFactory/lookupClass tokenizer-name)))
          {} (TokenizerFactory/availableTokenizers)))

(def char-filter-name->class
  (reduce (fn [acc ^String char-filter-name]
            (assoc acc (namify char-filter-name) (CharFilterFactory/lookupClass char-filter-name)))
          {} (CharFilterFactory/availableCharFilters)))

(def default-token-filters
  (reduce (fn [acc ^String token-filter-name]
            (assoc acc (namify token-filter-name) (TokenFilterFactory/lookupClass token-filter-name)))
          {} (TokenFilterFactory/availableTokenFilters)))

(def token-filter-name->class
  (cond-> default-token-filters
          features/raudikko? (assoc (namify "raudikko")
                                    (import 'org.apache.lucene.analysis.fi.RaudikkoTokenFilterFactory))))

(def analyzers
  (reduce (fn [acc [analyzer-name analyzer-class]]
            (assoc acc (namify (str/replace analyzer-name "Analyzer" "")) analyzer-class))
          {}
          lucene.predefined/analyzers))

(def DEFAULT_TOKENIZER_NAME "standard")

(defn ^Path config-dir->path [config-dir]
  (let [^String dir (or config-dir ".")]
    (.toPath (File. dir))))

(defn get-component-or-exception [factories name component-type]
  (if-let [component (get factories (namify name))]
    component
    (throw
      (Exception.
        (format "%s '%s' is not available. Choose one of: %s"
                component-type
                name
                (sort (keys factories)))))))

(defn custom-analyzer
  ([opts]
   (custom-analyzer opts char-filter-name->class tokenizer-name->class token-filter-name->class))
  ([{:keys [config-dir char-filters tokenizer token-filters]}
    char-filter-factories tokenizer-factories token-filter-factories]
   (let [^CustomAnalyzer$Builder builder (CustomAnalyzer/builder ^Path (config-dir->path config-dir))]
     (.withTokenizer builder
                     ^Class (get-component-or-exception tokenizer-factories
                                                        (get tokenizer :name DEFAULT_TOKENIZER_NAME)
                                                        "Tokenizer")
                     ^Map (HashMap. ^Map (stringify (get tokenizer :args))))

     (doseq [{:keys [name args]} char-filters]
       (.addCharFilter builder
                       ^Class (get-component-or-exception char-filter-factories name "Char filter")
                       ^Map (HashMap. ^Map (stringify args))))

     (doseq [{:keys [name args]} token-filters]
       (.addTokenFilter builder
                        ^Class (get-component-or-exception token-filter-factories name "Token filter")
                        ^Map (HashMap. ^Map (stringify args))))

     (.build builder))))

(defn ^Analyzer create
  "Either fetches a predefined analyzer or creates one from the config."
  ([opts] (create opts {}))
  ([{:keys [analyzer] :as opts} custom-analyzers]
   (try
     (let [analyzer-name (get analyzer :name)]
       (or
        (get custom-analyzers analyzer-name)
        (when analyzer-name
          (get-component-or-exception analyzers
                                      (namify (str/replace analyzer-name "Analyzer" ""))
                                      "Analyzer"))
        (custom-analyzer opts)))
     (catch Exception e
       (when (System/getenv "DEBUG_MODE")
         (.printStackTrace e))
       (throw e)))))

(comment
  (lmgrep.lucene.analyzer/create
    {:tokenizer {:name "standard"
                 :args {:maxTokenLength 4}}
     :char-filters [{:name "patternReplace"
                     :args {:pattern "joc"
                            :replacement "foo"}}]
     :token-filters [{:name "uppercase"}
                     {:name "reverseString"}]})

  (lmgrep.lucene.analyzer/create
    {:tokenizer {:name "standard"}
     :char-filters [{:name "patternReplace"
                     :args {:pattern "foo"
                            :replacement "bar"}}]}))
