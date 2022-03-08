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
  "Constructs query parser and parses the query.
  Params:
  - query: lucene query string
  - query-parser-name: Lucene query parser id, one of #{:classic :complex-phrase :surround :simple :standard}, default: :classic
  - query-parser-conf: map with query parser configuration
  - field-name: default field for terms query, defaults \"\"
  - analyzer: Lucene analyzer to apply on query terms, defaults StandardAnalyzer"
  ([^String query]
   (parse query :classic {} "" (StandardAnalyzer.)))
  ([^String query query-parser-name]
   (parse query query-parser-name {} "" (StandardAnalyzer.)))
  ([^String query query-parser-name query-parser-conf]
   (parse query query-parser-name query-parser-conf "" (StandardAnalyzer.)))
  ([^String query query-parser-name query-parser-conf ^String field-name]
   (parse query query-parser-name query-parser-conf field-name (StandardAnalyzer.)))
  ([^String query query-parser-name query-parser-conf ^String field-name ^Analyzer analyzer]
   (let [qp (query-parser/create query-parser-name query-parser-conf field-name analyzer)]
     (parse* qp query field-name))))

(comment
  (lmgrep.lucene.query/parse "foo bar baz" :classic {} "field-name" (StandardAnalyzer.)))
