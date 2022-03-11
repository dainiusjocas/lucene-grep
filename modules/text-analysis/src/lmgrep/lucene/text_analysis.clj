(ns lmgrep.lucene.text-analysis
  (:import (java.io StringReader PrintWriter StringWriter)
           (org.apache.lucene.analysis Analyzer TokenStream TokenStreamToDot)
           (org.apache.lucene.analysis.tokenattributes CharTermAttribute OffsetAttribute
                                                       PositionIncrementAttribute TypeAttribute
                                                       PositionLengthAttribute)
           (org.apache.lucene.analysis.standard StandardAnalyzer)))

(defn text->token-strings
  "Given a text and an analyzer returns a list of tokens as strings."
  ([^String text] (text->token-strings text (StandardAnalyzer.)))
  ([^String text ^Analyzer analyzer]
   (let [^TokenStream token-stream (.tokenStream analyzer "not-important" (StringReader. text))
         ^CharTermAttribute termAtt (.addAttribute token-stream CharTermAttribute)]
     (.reset token-stream)
     (loop [acc (transient [])]
       (if (.incrementToken token-stream)
         (recur (conj! acc (.toString termAtt)))
         (do
           (.end token-stream)
           (.close token-stream)
           (persistent! acc)))))))

(defn text->graph
  "Given a text turns into a TokenStream that will be writen out to dot language."
  [^String text ^Analyzer analyzer]
  (let [^TokenStream token-stream (.tokenStream analyzer "not-important" (StringReader. text))
        ^StringWriter sw (StringWriter.)]
    (.toDot (TokenStreamToDot. text token-stream (PrintWriter. sw)))
    (.end token-stream)
    (.close token-stream)
    (.toString sw)))

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
  (import 'org.apache.lucene.analysis.standard.StandardAnalyzer)

  (text->token-strings "foo text bar BestClass fooo name" (StandardAnalyzer.))
  (text->graph "fooBarBazs" (StandardAnalyzer.))
  (text->tokens "pre BestClass post" (StandardAnalyzer.)))
