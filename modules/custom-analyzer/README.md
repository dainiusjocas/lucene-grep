# custom-analyzer

(Micro)Library to build [Lucene](https://lucene.apache.org) analyzers in a data-driven fashion.

Why's:
- Current Clojure Lucene libraries (e.g. [jaju/lucene-clj](https://github.com/jaju/lucene-clj), [federkasten/clucie](https://github.com/federkasten/clucie)) doesn't expose a mechanism to build your custom Lucene Analyzer.
- Data-driven.
- Allows for extensibility using standard [Lucene SPI](https://lucene.apache.org/core/9_1_0/core/org/apache/lucene/analysis/AnalysisSPILoader.html), i.e. just put a JAR in the CLASSPATH.
- Allows to specify a directory from which resources will be loaded, e.g. synonyms dictionaries.

## Quickstart

Dependencies:

```clojure
{:deps
 {lt.jocas/custom-analyzer {:local/root "./modules/custom-analyzer"}}}
```

Code:

```clojure
(require '[lmgrep.lucene.custom-analyzer :as custom-analyzer])

(custom-analyzer/create
  {:tokenizer              {:standard {:maxTokenLength 4}}
   :char-filters           [{:patternReplace {:pattern     "foo"
                                              :replacement "foo"}}]
   :token-filters          [{:uppercase nil}
                            {:reverseString nil}]
   :offset-gap             2
   :position-increment-gap 3
   :config-dir             "."})
;; =>
;; #object[org.apache.lucene.analysis.custom.CustomAnalyzer
;;         0x4686f87d
;;         "CustomAnalyzer(org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory@2f1300,org.apache.lucene.analysis.standard.StandardTokenizerFactory@7e71a244,org.apache.lucene.analysis.core.UpperCaseFilterFactory@54e9f0d6,org.apache.lucene.analysis.reverse.ReverseStringFilterFactory@3e494ba7)"]
```

If no options are provided then an Analyzer with just the standard tokenizer is created:

```clojure
(custom-analyzer/create {})
;; =>
;; #object[org.apache.lucene.analysis.custom.CustomAnalyzer
;;         0x456fe86
;;         "CustomAnalyzer(org.apache.lucene.analysis.standard.StandardTokenizerFactory@5703f5b3)"]
```

If you want to check which analysis components are available run:

```clojure
(lmgrep.lucene.custom-analyzer/char-filter-factories)
(lmgrep.lucene.custom-analyzer/tokenizer-factories)
(lmgrep.lucene.custom-analyzer/token-filter-factories)
```

## Design

Under the hood this library uses the factory classes `TokenizerFactory`, `TokenFilterFactory`, and `CharFilterFactory`.
The actual factories are loaded with `java.util.ServiceLoader`.
All the available classes are automatically discovered.

If you want to include additional factory classes, e.g. your implementation of the `TokenFilterFactory,` you need to add it to the classpath 2 things:
 1. The implementation class of one of the Factory classes
 2. Under the `META-INF/services` add/change a file named `org.apache.lucene.analysis.TokenFilterFactory` that lists the classes from the step 1.

An example can be found [here](https://github.com/dainiusjocas/lucene-grep/tree/main/modules/raudikko).

## Future work

- [ ] Conditional token filters

## License

Copyright &copy; 2022 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
