(ns lmgrep.lucene.query
  (:import (org.apache.lucene.search Query)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser)))

(defn ^Query parse [qp ^String query-string query-parser-name ^String field-name]
  (case query-parser-name
    :classic (.parse ^QueryParser qp query-string)
    :complex-phrase (.parse ^ComplexPhraseQueryParser qp query-string)
    :surround (.makeLuceneQueryField
                (org.apache.lucene.queryparser.surround.parser.QueryParser/parse query-string)
                field-name qp)
    :simple (.parse ^SimpleQueryParser qp query-string)
    :standard (.parse ^StandardQueryParser qp query-string field-name)
    (.parse ^QueryParser qp query-string)))
