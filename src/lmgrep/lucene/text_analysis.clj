(ns lmgrep.lucene.text-analysis
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lmgrep.lucene.analyzer :as analyzer])
  (:import (org.apache.lucene.analysis Analyzer TokenStream)
           (org.apache.lucene.analysis.tokenattributes CharTermAttribute)
           (java.io StringReader)))

; TODO: convert to hashmap
(defn ^String stemmer
  "Creates a stemmer object given the stemmer keyword.
  Default stemmer is English."
  [stemmer-kw]
  (case stemmer-kw
    :arabic "arabicstem"
    :armenian "armenianSnowballStem"
    :basque "basqueSnowballStem"
    :catalan "catalanSnowballStem"
    :danish "danishSnowballStem"
    :dutch "dutchSnowballStem"
    :english "englishMinimalStem"
    :estonian "basqueSnowballStem"
    :finnish "finnishlightstem"
    :french "frenchLightStem"
    :german2 "germanlightstem"
    :german "germanstem"
    :hungarian "hungarianLightStem"
    :irish "irishSnowballStem"
    :italian "italianlightstem"
    :kp "kpSnowballStem"
    :lithuanian "lithuanianSnowballStem"
    :lovins "lovinsSnowballStem"
    :norwegian "norwegianminimalstem"
    :porter "porterstem"
    :portuguese "portugueselightstem"
    :romanian "romanianSnowballStem"
    :russian "russianlightstem"
    :spanish "spanishlightstem"
    :swedish "swedishlightstem"
    :turkish "turkishSnowballStem"
    (do
      (when stemmer-kw
        (log/debugf "Stemmer '%s' not found! EnglishStemmer is used." stemmer-kw))
      "englishMinimalStem")))

; TODO: convert to hashmap
(defn tokenizer [tokenizer-kw]
  (case tokenizer-kw
    :keyword {:name "keyword"}
    :letter {:name "letter"}
    :standard {:name "standard"}
    :unicode-whitespace {:name "whitespace" :args {:rule "unicode"}}
    :whitespace {:name "whitespace" :args {:rule "java"}}
    (do
      (when tokenizer-kw
        (log/debugf "Tokenizer '%s' not found. StandardTokenizer is used." tokenizer-kw))
      {:name "standard"})))

(defn wdgf->token-filter-args [wdgf]
  (when (pos-int? wdgf)
    (cond-> {}
            (not (zero? (bit-and wdgf 1))) (assoc "generateWordParts" 1)
            (not (zero? (bit-and wdgf 2))) (assoc "generateNumberParts" 1)
            (not (zero? (bit-and wdgf 4))) (assoc "catenateWords" 1)
            (not (zero? (bit-and wdgf 8))) (assoc "catenateNumbers" 1)
            (not (zero? (bit-and wdgf 16))) (assoc "catenateAll" 1)
            (not (zero? (bit-and wdgf 32))) (assoc "preserveOriginal" 1)
            (not (zero? (bit-and wdgf 64))) (assoc "splitOnCaseChange" 1)
            (not (zero? (bit-and wdgf 128))) (assoc "splitOnNumerics" 1)
            (not (zero? (bit-and wdgf 256))) (assoc "stemEnglishPossessive" 1)
            (not (zero? (bit-and wdgf 512))) (assoc "ignoreKeywords" 1))))

(defn flags->analysis-conf [{tokenizer-kw    :tokenizer
                             ascii-fold?     :ascii-fold?
                             case-sensitive? :case-sensitive?
                             stem?           :stem?
                             stemmer-kw      :stemmer
                             wdgf            :word-delimiter-graph-filter}]
  (let [wdgf-args (wdgf->token-filter-args wdgf)
        tokenizr (get tokenizer tokenizer-kw)
        token-filters (cond-> []
                              (pos-int? wdgf) (conj {:name "worddelimitergraph"
                                                     :args wdgf-args})
                              (false? case-sensitive?) (conj {:name "lowercase"})
                              ascii-fold? (conj {:name "asciifolding"})
                              stem? (conj {:name (stemmer stemmer-kw)}))]

    (cond-> {}
            (not (nil? tokenizer-kw)) (assoc :tokenizer tokenizr)
            true (assoc :token-filters token-filters))))

(defn merge-from-flags-with-analysis-conf [analysis analysis-conf]
  (cond-> analysis
          (nil? (get analysis :tokenizer)) (assoc :tokenizer (get analysis-conf :tokenizer))
          true (update :token-filters (fn [val] (concat (get analysis-conf :token-filters) val)))))

(defn analyzer-constructor [analysis-conf]
  (analyzer/create analysis-conf))

(defn field-name-constructor [analysis-conf]
  (let [analyzer-name (get-in analysis-conf [:analyzer :name])
        tokenizr (str (name (or (get-in analysis-conf [:tokenizer :name]) :standard)) "-tokenizer")
        char-filters (sort (map :name (get analysis-conf :char-filters)))
        filters (sort (map :name (get analysis-conf :token-filters)))]
    (if (seq filters)
      (str "text" "." tokenizr "." (or analyzer-name (string/join "-" filters)))
      (str "text" "." (or analyzer-name tokenizr)))))

(def analyzer (memoize analyzer-constructor))
(def field-name (memoize field-name-constructor))

(defrecord Conf [tokenizer case-sensitive? ascii-fold? stem? stemmer word-delimiter-graph-filter])

(defn two-way-merge
  "Given a key and two maps return the value that would appear in the map after merge.
  Semantics is of the default Clojure merge."
  [k m1 m2]
  (if (nil? (get m2 k))
    (get m1 k)
    (get m2 k)))

(defn ^Analyzer get-string-analyzer [analysis-conf]
  (analyzer analysis-conf))

(defn ^String get-field-name [analysis-conf]
  (field-name analysis-conf))

(defn text->token-strings
  "Given a text and an analyzer returns a list of tokens as strings."
  [^String text ^Analyzer analyzer]
  (let [^TokenStream token-stream (.tokenStream analyzer "not-important" (StringReader. text))
        ^CharTermAttribute termAtt (.addAttribute token-stream CharTermAttribute)]
    (.reset token-stream)
    (loop [acc (transient [])]
      (if (.incrementToken token-stream)
        (recur (conj! acc (.toString termAtt)))
        (do
          (.end token-stream)
          (.close token-stream)
          (persistent! acc))))))

(comment
  (text->token-strings
    "foo text bar BestClass fooo name"
    (analyzer/create
      (flags->analysis-conf
        {:tokenizer                   :whitespace
         :case-sensitive?             false
         :ascii-fold?                 false
         :stem?                       true
         :stemmer                     :english
         :word-delimiter-graph-filter (+ 1 2 32 64)})))

  (text->token-strings
    "The quick brown fox jumps over the lazy doggy"
    (analyzer/create
      (flags->analysis-conf
        {:tokenizer       :standard
         :case-sensitive? true
         :ascii-fold?     false
         :stem?           true
         :stemmer         :german}))))
