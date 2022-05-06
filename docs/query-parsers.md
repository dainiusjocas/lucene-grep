# Query Parsers

5 Lucene query parsers are supported: `[:classic :complex-phrase :simple :standard :surround]`.

## Configuration

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
