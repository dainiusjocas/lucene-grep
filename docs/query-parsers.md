# Predefined Analyzers

Currently, 5 [Lucene query parsers](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html) are supported:

- classic: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- complex-phrase: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- simple: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- standard: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)
- surround: [docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html)

These query parsers can be further configured with these parameters that were extracted from the Lucene source code.
List of supported keys:

- allow-leading-wildcard: applies to [`classic`, `complex-phrase`, `standard`]
- auto-generate-multi-term-synonyms-phrase-query: applies to [`classic`, `complex-phrase`, `standard`, `simple`]
- auto-generate-phrase-queries: applies to [`classic`, `complex-phrase`]
- date-resolution: applies to [`classic`, `complex-phrase`, `standard`]
- default-operator: applies to [`simple`]
- determinize-work-limit: applies to [`classic`, `complex-phrase`]
- enable-graph-queries: applies to [`classic`, `complex-phrase`, `standard`, `simple`]
- enable-position-increments: applies to [`classic`, `complex-phrase`, `standard`, `simple`]
- flags: applies to [`simple`]
- fuzzy-min-sim: applies to [`classic`, `complex-phrase`, `standard`]
- fuzzy-prefix-length: applies to [`classic`, `complex-phrase`, `standard`]
- in-order: applies to [`complex-phrase`]
- locale: applies to [`classic`, `complex-phrase`, `standard`]
- max-basic-queries: applies to [`surround`]
- multi-term-rewrite-method: applies to [`classic`, `complex-phrase`, `standard`]
- phrase-slop: applies to [`classic`, `complex-phrase`, `standard`]
- split-on-whitespace: applies to [`classic`, `complex-phrase`]
- time-zone: applies to [`classic`, `complex-phrase`, `standard`]

For further details consult the [Lucene docs](https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html).
