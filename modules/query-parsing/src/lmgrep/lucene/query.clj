(ns lmgrep.lucene.query
  (:require [lmgrep.lucene.query-parser :as query-parser])
  (:import (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.search Query)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)))

(defn ^Query parse* [query-parser ^String query-string ^String field-name]
  (cond
    (instance? QueryParser query-parser) (.parse ^QueryParser query-parser query-string)
    (instance? ComplexPhraseQueryParser query-parser) (.parse ^ComplexPhraseQueryParser query-parser query-string)
    (instance? BasicQueryFactory query-parser)
    (.makeLuceneQueryField
      (org.apache.lucene.queryparser.surround.parser.QueryParser/parse query-string)
      field-name ^BasicQueryFactory query-parser)
    (instance? SimpleQueryParser query-parser) (.parse ^SimpleQueryParser query-parser query-string)
    (instance? StandardQueryParser query-parser) (.parse ^StandardQueryParser query-parser query-string field-name)))

(defn ^Query parse
  ([^String query query-parser-name query-parser-conf ^String field-name]
   (parse query query-parser-name query-parser-conf field-name (StandardAnalyzer.)))
  ([^String query query-parser-name query-parser-conf ^String field-name ^Analyzer monitor-analyzer]
   (let [qp (query-parser/create query-parser-name query-parser-conf field-name monitor-analyzer)]
     (parse* qp query field-name))))

(comment
  (lmgrep.lucene.query/parse "foo bar baz" :classic {} "field-name"))
