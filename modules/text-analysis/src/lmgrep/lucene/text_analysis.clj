(ns lmgrep.lucene.text-analysis
  (:import (java.io StringReader PrintWriter StringWriter)
           (org.apache.lucene.analysis Analyzer TokenStream TokenStreamToDot)
           (org.apache.lucene.analysis.tokenattributes CharTermAttribute OffsetAttribute
                                                       PositionIncrementAttribute TypeAttribute
                                                       PositionLengthAttribute)
           (org.apache.lucene.analysis.standard StandardAnalyzer)))

(def ^:private ^String FIELD_NAME "not-important")

(defn text->token-strings
  "Given a text (and an optional analyzer) returns a vector of tokens as strings."
  ([^String text] (text->token-strings text (StandardAnalyzer.)))
  ([^String text ^Analyzer analyzer]
   (if (nil? text)
     []
     (let [^TokenStream token-stream (.tokenStream analyzer FIELD_NAME (StringReader. text))
           ^CharTermAttribute char-term-attribute (.addAttribute token-stream CharTermAttribute)]
       (.reset token-stream)
       (loop [acc (transient [])]
         (if (.incrementToken token-stream)
           (recur (conj! acc (.toString char-term-attribute)))
           (do
             (.end token-stream)
             (.close token-stream)
             (persistent! acc))))))))

(defn text->graph
  "Given a text (and an optional analyzer) turns the text into a TokenStream
  that will be converted to the `dot` language program as a string, e.g.:
  `digraph tokens {
     graph [ fontsize=30 labelloc=\\\"t\\\" label=\\\"\\\" splines=true overlap=false rankdir = \\\"LR\\\" ];
     // A2 paper size
     size = \\\"34.4,16.5\\\";
     edge [ fontname=\\\"Helvetica\\\" fontcolor=\\\"red\\\" color=\\\"#606060\\\" ]
     node [ style=\\\"filled\\\" fillcolor=\\\"#e8e8f0\\\" shape=\\\"Mrecord\\\" fontname=\\\"Helvetica\\\" ]

     0 [label=\\\"0\\\"]
     -1 [shape=point color=white]
     -1 -> 0 []
     0 -> 1 [ label=\\\"foobarbazs / fooBarBazs\\\"]
     -2 [shape=point color=white]
     1 -> -2 []
   }`"
  ([^String text] (text->graph text (StandardAnalyzer.)))
  (^String [^String text ^Analyzer analyzer]
   (let [text (or text "")
         ^TokenStream token-stream (.tokenStream analyzer FIELD_NAME (StringReader. text))
         ^StringWriter string-writer (StringWriter.)]
     (.toDot (TokenStreamToDot. text token-stream (PrintWriter. string-writer)))
     (.end token-stream)
     (.close token-stream)
     (.toString string-writer))))

(defrecord TokenRecord [^String token
                        ^String type
                        ^int start_offset
                        ^int end_offset
                        ^int position
                        ^int positionLength])

(defn text->tokens
  "Given a text (and an optional analyzer) returns a list of tokens as maps of shape:
  {:token \"pre\",
   :type \"<ALPHANUM>\",
   :start_offset 0,
   :end_offset 3,
   :position 0,
   :positionLength 1}"
  ([^String text] (text->tokens text (StandardAnalyzer.)))
  ([^String text ^Analyzer analyzer]
   (if (nil? text)
     []
     (let [ONE (int 1)
           ^TokenStream token-stream (.tokenStream analyzer FIELD_NAME (StringReader. text))
           ^CharTermAttribute char-term-attribute (.addAttribute token-stream CharTermAttribute)
           ^OffsetAttribute offset-attribute (.addAttribute token-stream OffsetAttribute)
           ^PositionIncrementAttribute position-attribute (.addAttribute token-stream PositionIncrementAttribute)
           ^TypeAttribute type-attriubte (.addAttribute token-stream TypeAttribute)
           ^PositionLengthAttribute position-length-attribute (.addAttribute token-stream PositionLengthAttribute)]
       (.reset token-stream)
       (loop [acc (transient [])
              position (int 0)]
         (if (.incrementToken token-stream)
           (recur (conj! acc (->TokenRecord (.toString char-term-attribute)
                                            (.type type-attriubte)
                                            (.startOffset offset-attribute)
                                            (.endOffset offset-attribute)
                                            position
                                            (.getPositionLength position-length-attribute)))
                  (if (< ONE (.getPositionLength position-length-attribute))
                    position
                    (unchecked-add-int position (Math/max (.getPositionIncrement position-attribute) ONE))))
           (do
             (.end token-stream)
             (.close token-stream)
             (persistent! acc))))))))

(comment
  (text->token-strings "foo text bar BestClass fooo name")
  (text->token-strings nil)
  (text->token-strings "foo text bar BestClass fooo name" (StandardAnalyzer.))
  (text->graph "fooBarBazs" (StandardAnalyzer.))
  (text->tokens "pre BestClass post" (StandardAnalyzer.)))
