(ns lmgrep.lucene.query.impl
  (:import (org.apache.lucene.search Query)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)))

(defn parse* ^Query
  [query-parser ^String query-string ^String field-name]
  (cond
    (instance? QueryParser query-parser) (.parse ^QueryParser query-parser query-string)
    (instance? ComplexPhraseQueryParser query-parser) (.parse ^ComplexPhraseQueryParser query-parser query-string)
    (instance? BasicQueryFactory query-parser)
    (.makeLuceneQueryField
      (org.apache.lucene.queryparser.surround.parser.QueryParser/parse query-string)
      field-name ^BasicQueryFactory query-parser)
    (instance? SimpleQueryParser query-parser) (.parse ^SimpleQueryParser query-parser query-string)
    (instance? StandardQueryParser query-parser) (.parse ^StandardQueryParser query-parser query-string field-name)))
