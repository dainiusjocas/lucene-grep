(ns lmgrep.lucene.query-parser.parsers
  (:require [lmgrep.lucene.query-parser.conf :as query-parser.conf])
  (:import (clojure.lang Indexed)
           (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)))

(defn set-conf [query-parser conf defaults]
  (doseq [config-key-and-attrs defaults]
    (when-not (nil? (get conf (.nth ^Indexed config-key-and-attrs 0)))
      ((:handler (.nth ^Indexed config-key-and-attrs 1)) query-parser conf))))

(defn configure [query-parser conf]
  (when-not (empty? conf)
    (doseq [class-and-defaults query-parser.conf/query-parser-class->attrs]
      (when (instance? ^Class (.nth ^Indexed class-and-defaults 0) query-parser)
        (set-conf query-parser conf (.nth ^Indexed class-and-defaults 1)))))
  query-parser)

(defn classic ^QueryParser [conf field-name analyzer]
  (configure (QueryParser. field-name analyzer) conf))

(defn complex-phrase ^ComplexPhraseQueryParser [conf field-name analyzer]
  (configure (ComplexPhraseQueryParser. field-name analyzer) conf))

(defn standard ^StandardQueryParser [conf analyzer]
  (configure (StandardQueryParser. analyzer) conf))

(defn simple ^SimpleQueryParser [conf ^String field-name ^Analyzer analyzer]
  (let [flags (int (get conf :flags -1))
        weights {field-name (float 1)}
        simple-query-parser (SimpleQueryParser. analyzer weights flags)]
    (configure simple-query-parser conf)))

(defn surround ^BasicQueryFactory [conf]
  (let [max-basic-queries (int (get conf :max-basic-queries 1024))]
    (BasicQueryFactory. max-basic-queries)))

(comment
  (simple {} "field-name" (StandardAnalyzer.)))
