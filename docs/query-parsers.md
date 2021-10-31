# Predefined Analyzers

Currently, 5 [Lucene query parsers](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html) are supported:

- classic: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- complex-phrase: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- simple: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- standard: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- surround: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)

These query parsers can be further configured with these parameters that were extracted from the Lucene source code.
List of supported keys:

- allow-leading-wildcard
- auto-generate-multi-term-synonyms-phrase-query
- auto-generate-phrase-queries
- date-resolution
- default-operator
- enable-graph-queries
- enable-position-increments
- flags
- fuzzy-min-sim
- fuzzy-prefix-length
- in-order
- locale
- max-determinized-states
- maxBasicQueries
- multi-term-rewrite-method
- phrase-slop
- split-on-whitespace
- time-zone

For further details consult the [Lucene docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html).
