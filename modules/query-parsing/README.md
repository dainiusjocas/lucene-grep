# lucene-query-parsing

Library to build [Lucene](https://lucene.apache.org) query parsers and parse queries in the data-driven fashion.

## Quickstart

Dependencies:
```clojure
{:deps
 {lt.jocas/query-parsing {:local/root "modules/query-parsing"}}}
```
Code:
```clojure
(require '[lmgrep.lucene.query :as q])

(q/parse "foo bar baz")
;; => #object[org.apache.lucene.search.BooleanQuery 0x650526d1 "foo bar baz"]

(q/parse "foo bar baz" :classic {} "field-name")
;; => #object[org.apache.lucene.search.BooleanQuery 0x3218294 "field-name:foo field-name:bar field-name:baz"]
```

## Available Query Parsers

Currently, 5 [Lucene query parsers](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html) are supported:

- classic: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- complex-phrase: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- simple: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- standard: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- surround: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)

These query parsers can be further configured with these parameters that were extracted from the Lucene source code.
The configuration options compatibility table for each supported query parser:

|                                         :option | :classic | :complex-phrase | :simple | :standard | :surround |
|-------------------------------------------------|----------|-----------------|---------|-----------|-----------|
|                         :allow-leading-wildcard |     true |            true |   false |      true |     false |
| :auto-generate-multi-term-synonyms-phrase-query |     true |            true |    true |     false |     false |
|                   :auto-generate-phrase-queries |     true |            true |   false |     false |     false |
|                                :date-resolution |     true |            true |   false |      true |     false |
|                               :default-operator |     true |            true |    true |      true |     false |
|                               :default-operator |     true |            true |    true |      true |     false |
|                               :default-operator |     true |            true |    true |      true |     false |
|                         :determinize-work-limit |     true |            true |   false |     false |     false |
|                           :enable-graph-queries |     true |            true |    true |     false |     false |
|                     :enable-position-increments |     true |            true |    true |      true |     false |
|                     :enable-position-increments |     true |            true |    true |      true |     false |
|                                  :fuzzy-min-sim |     true |            true |   false |      true |     false |
|                            :fuzzy-prefix-length |     true |            true |   false |      true |     false |
|                                       :in-order |    false |            true |   false |     false |     false |
|                                         :locale |     true |            true |   false |      true |     false |
|                      :multi-term-rewrite-method |     true |            true |   false |      true |     false |
|                                    :phrase-slop |     true |            true |   false |      true |     false |
|                            :split-on-whitespace |     true |            true |   false |     false |     false |
|                                      :time-zone |     true |            true |   false |      true |     false |

## Defaults

Default values for every Lucene query parser configuration option:

|                                         :option |               :classic |        :complex-phrase | :simple |              :standard | :surround |
|-------------------------------------------------|------------------------|------------------------|---------|------------------------|-----------|
|                         :allow-leading-wildcard |                  false |                  false |         |                  false |           |
| :auto-generate-multi-term-synonyms-phrase-query |                  false |                  false |   false |                        |           |
|                   :auto-generate-phrase-queries |                  false |                  false |         |                        |           |
|                                :date-resolution |                        |                        |         |                        |           |
|                               :default-operator |                     OR |                     OR |  should |                     OR |           |
|                               :default-operator |                     OR |                     OR |  should |                     OR |           |
|                               :default-operator |                     OR |                     OR |  should |                     OR |           |
|                         :determinize-work-limit |                  10000 |                  10000 |         |                        |           |
|                           :enable-graph-queries |                   true |                   true |    true |                        |           |
|                     :enable-position-increments |                  false |                  false |    true |                  false |           |
|                     :enable-position-increments |                  false |                  false |    true |                  false |           |
|                                  :fuzzy-min-sim |                    2.0 |                    2.0 |         |                    2.0 |           |
|                            :fuzzy-prefix-length |                      0 |                      0 |         |                      0 |           |
|                                       :in-order |                        |                   true |         |                        |           |
|                                         :locale |                     en |                     en |         |                     en |           |
|                      :multi-term-rewrite-method | CONSTANT_SCORE_REWRITE | CONSTANT_SCORE_REWRITE |         | CONSTANT_SCORE_REWRITE |           |
|                                    :phrase-slop |                      0 |                      0 |         |                      0 |           |
|                            :split-on-whitespace |                   true |                   true |         |                        |           |
|                                      :time-zone |                        |                        |         |                        |           |

In case you're lost: those cells that in the compatibility table states true but in the default values is empty
mean that the default value is nil.

For further details consult the [Lucene docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html).

## License

Copyright &copy; 2022 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
