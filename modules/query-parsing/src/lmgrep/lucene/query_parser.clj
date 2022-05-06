(ns lmgrep.lucene.query-parser
  (:require [lmgrep.lucene.query-parser.parsers :as parsers])
  (:import (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.standard StandardAnalyzer)))

(defn create
  "Constructs an Object that can be used for later query parsing.
   Defaults to the classic query parser.
   Params:
   - query-parser-name: Lucene query parser id, one of #{:classic :complex-phrase :surround :simple :standard}, default: :classic
   - conf: a map with query parser configuration
   - field-name: default field for terms query, defaults \"\"
   - analyzer: Lucene analyzer to apply on query terms, defaults StandardAnalyzer
   https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html"
  ([]
   (create :classic {} "" (StandardAnalyzer.)))
  ([query-parser-name]
   (create query-parser-name {} "" (StandardAnalyzer.)))
  ([query-parser-name conf]
   (create query-parser-name conf "" (StandardAnalyzer.)))
  ([query-parser-name conf ^String field-name]
   (create query-parser-name conf field-name (StandardAnalyzer.)))
  ([query-parser-name conf ^String field-name ^Analyzer analyzer]
   (case (keyword query-parser-name)
     :classic (parsers/classic conf field-name analyzer)
     :complex-phrase (parsers/complex-phrase conf field-name analyzer)
     :surround (parsers/surround conf)
     :simple (parsers/simple conf field-name analyzer)
     :standard (parsers/standard conf analyzer)
     (parsers/classic conf field-name analyzer))))

(comment
  (create)
  (create "classic" {} "field-name" (StandardAnalyzer.)))
