(ns lmgrep.lucene.text-analysis
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import (org.apache.lucene.analysis Analyzer Analyzer$TokenStreamComponents Tokenizer TokenStream CharArraySet)
           (org.apache.lucene.analysis.core LowerCaseFilter WhitespaceTokenizer LetterTokenizer KeywordTokenizer UnicodeWhitespaceTokenizer)
           (org.apache.lucene.analysis.miscellaneous ASCIIFoldingFilter WordDelimiterGraphFilter)
           (org.apache.lucene.analysis.standard ClassicFilter StandardTokenizer)
           (org.apache.lucene.analysis.tokenattributes CharTermAttribute)
           (org.apache.lucene.analysis.snowball SnowballFilter)
           (org.tartarus.snowball.ext LithuanianStemmer ArabicStemmer ArmenianStemmer BasqueStemmer EnglishStemmer CatalanStemmer DanishStemmer DutchStemmer EstonianStemmer FinnishStemmer FrenchStemmer German2Stemmer GermanStemmer HungarianStemmer IrishStemmer ItalianStemmer KpStemmer LovinsStemmer NorwegianStemmer PorterStemmer PortugueseStemmer RomanianStemmer RussianStemmer SpanishStemmer SwedishStemmer TurkishStemmer)
           (org.tartarus.snowball SnowballProgram)
           (java.io StringReader)))

(defn ^SnowballProgram stemmer
  "Creates a stemmer object given the stemmer keyword.
  Default stemmer is English."
  [stemmer-kw]
  (case stemmer-kw
    :arabic (ArabicStemmer.)
    :armenian (ArmenianStemmer.)
    :basque (BasqueStemmer.)
    :catalan (CatalanStemmer.)
    :danish (DanishStemmer.)
    :dutch (DutchStemmer.)
    :english (EnglishStemmer.)
    :estonian (EstonianStemmer.)
    :finnish (FinnishStemmer.)
    :french (FrenchStemmer.)
    :german2 (German2Stemmer.)
    :german (GermanStemmer.)
    :hungarian (HungarianStemmer.)
    :irish (IrishStemmer.)
    :italian (ItalianStemmer.)
    :kp (KpStemmer.)
    :lithuanian (LithuanianStemmer.)
    :lovins (LovinsStemmer.)
    :norwegian (NorwegianStemmer.)
    :porter (PorterStemmer.)
    :portuguese (PortugueseStemmer.)
    :romanian (RomanianStemmer.)
    :russian (RussianStemmer.)
    :spanish (SpanishStemmer.)
    :swedish (SwedishStemmer.)
    :turkish (TurkishStemmer.)
    (do
      (when stemmer-kw
        (log/debugf "Stemmer '%s' not found! EnglishStemmer is used." stemmer-kw))
      (EnglishStemmer.))))

(defn ^Tokenizer tokenizer [tokenizer-kw]
  (case tokenizer-kw
    :keyword (KeywordTokenizer.)
    :letter (LetterTokenizer.)
    :standard (StandardTokenizer.)
    :unicode-whitespace (UnicodeWhitespaceTokenizer.)
    :whitespace (WhitespaceTokenizer.)
    (do
      (when tokenizer-kw
        (log/debugf "Tokenizer '%s' not found. StandardTokenizer is used." tokenizer-kw))
      (StandardTokenizer.))))

(defn analyzer-constructor [{tokenizer-kw    :tokenizer
                             ascii-fold?     :ascii-fold?
                             case-sensitive? :case-sensitive?
                             stem?           :stem?
                             stemmer-kw      :stemmer
                             wdgf            :word-delimiter-graph-filter}]
  (proxy [Analyzer] []
    (createComponents [^String field-name]
      (let [^Tokenizer tokenizr (tokenizer tokenizer-kw)
            ^TokenStream filters-chain
            (cond-> tokenizr
                    (and (number? wdgf) (pos? wdgf)) (WordDelimiterGraphFilter. wdgf CharArraySet/EMPTY_SET)
                    (not case-sensitive?) (LowerCaseFilter.)
                    ascii-fold? (ASCIIFoldingFilter.))
            token-stream (if stem?
                           (SnowballFilter. filters-chain (stemmer stemmer-kw))
                           (if (instance? Tokenizer filters-chain)
                             (ClassicFilter. tokenizr)
                             filters-chain))]
        (Analyzer$TokenStreamComponents.
          ^Tokenizer tokenizr ^TokenStream token-stream)))))

(defn field-name-constructor [{tokenizer-kw    :tokenizer
                               ascii-fold?     :ascii-fold?
                               case-sensitive? :case-sensitive?
                               stem?           :stem?
                               stemmer-kw      :stemmer
                               wdgf            :word-delimiter-graph-filter}]
  (let [tokenizr (str (name (or tokenizer-kw :standard)) "-tokenizer")
        filters (cond-> []
                        (and (number? wdgf) (pos? wdgf)) (conj (str "wdgf-" wdgf))
                        (not case-sensitive?) (conj "lowercased")
                        ascii-fold? (conj "ascii-folded")
                        stem? (conj (str "stemmed-" (name (or stemmer-kw :english)))))]
    (if (seq filters)
      (str "text" "." tokenizr "." (string/join "-" (sort filters)))
      (str "text" "." tokenizr))))

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

(defn merged-conf [analysis-conf default-analysis-conf]
  (->Conf
    (two-way-merge :tokenizer default-analysis-conf analysis-conf)
    (two-way-merge :case-sensitive? default-analysis-conf analysis-conf)
    (two-way-merge :ascii-fold? default-analysis-conf analysis-conf)
    (two-way-merge :stem? default-analysis-conf analysis-conf)
    (two-way-merge :stemmer default-analysis-conf analysis-conf)
    (two-way-merge :word-delimiter-graph-filter default-analysis-conf analysis-conf)))

(defn ^Analyzer get-string-analyzer [analysis-conf default-analysis-conf]
  (analyzer (merged-conf analysis-conf default-analysis-conf)))

(defn ^String get-field-name [analysis-conf default-analysis-conf]
  (field-name (merged-conf analysis-conf default-analysis-conf)))

(defn text->token-strings
  "Given a text and an analyzer returns a list of tokens as strings."
  [^String text ^Analyzer analyzer]
  (let [^TokenStream token-stream (.tokenStream analyzer "not-important" (StringReader. text))
        ^CharTermAttribute termAtt (.addAttribute token-stream CharTermAttribute)]
    (.reset token-stream)
    (reduce (fn [acc _]
              (if (.incrementToken token-stream)
                (conj acc (.toString termAtt))
                (do
                  (.end token-stream)
                  (.close token-stream)
                  (reduced acc)))) [] (range))))

(comment
  (text->token-strings
    "foo text bar BestClass name" (analyzer-constructor {:tokenizer       :whitespace
                                                         :case-sensitive? false
                                                         :ascii-fold?     false
                                                         :stem?           true
                                                         :stemmer         :english
                                                         :word-delimiter-graph-filter (+ 1 2 32 64)})))
