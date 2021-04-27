(ns lmgrep.lucene.text-analysis
  (:require [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.analysis-conf :as ac])
  (:import (org.apache.lucene.analysis Analyzer TokenStream)
           (org.apache.lucene.analysis.tokenattributes CharTermAttribute FlagsAttribute OffsetAttribute PositionIncrementAttribute TypeAttribute TermFrequencyAttribute PositionLengthAttribute KeywordAttribute)
           (java.io StringReader)))

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
      (ac/prepare-analysis-configuration
        ac/default-text-analysis
        {:tokenizer                   :whitespace
         :case-sensitive?             false
         :ascii-fold?                 false
         :stem?                       true
         :stemmer                     :english
         :word-delimiter-graph-filter (+ 1 2 32 64)}))))

(defrecord TokenRecord [token type start_offset end_offset position positionLength])

(defn text->tokens
  "Given a text and an analyzer returns a list of tokens as strings."
  [^String text ^Analyzer analyzer]
  (let [^TokenStream token-stream (.tokenStream analyzer "not-important" (StringReader. text))
        ^CharTermAttribute termAtt (.addAttribute token-stream CharTermAttribute)
        ^OffsetAttribute offsetAtt (.addAttribute token-stream OffsetAttribute)
        ^PositionIncrementAttribute position (.addAttribute token-stream PositionIncrementAttribute)
        ^TypeAttribute type (.addAttribute token-stream TypeAttribute)
        ^PositionLengthAttribute pla (.addAttribute token-stream PositionLengthAttribute)]
    (.reset token-stream)
    (loop [acc (transient [])
           pos 0]
      (if (.incrementToken token-stream)
        (recur (conj! acc (->TokenRecord (.toString termAtt)
                                         (.type type)
                                         (.startOffset offsetAtt)
                                         (.endOffset offsetAtt)
                                         pos
                                         (.getPositionLength pla)))
               (if (< 1 (.getPositionLength pla))
                 pos
                 (+ pos (max (.getPositionIncrement position) 1))))
        (do
          (.end token-stream)
          (.close token-stream)
          (persistent! acc))))))

(comment
  (clojure.pprint/print-table
    (text->tokens
      "pre BestClass post"
      (analyzer/create
        (ac/prepare-analysis-configuration
          ac/default-text-analysis
          {:tokenizer                   :whitespace
           :case-sensitive?             false
           :ascii-fold?                 false
           :stem?                       true
           :stemmer                     :english
           :word-delimiter-graph-filter (+ 1 2 32 64)})))))
