# custom-analyzer

Library to build [Lucene](https://lucene.apache.org) analyzers in the data-driven fashion.

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

If nothing is provided then an Anlyzer with just the standard tokenizer is created:

```clojure
(custom-analyzer/create {})
;; =>
;; #object[org.apache.lucene.analysis.custom.CustomAnalyzer
;;         0x456fe86
;;         "CustomAnalyzer(org.apache.lucene.analysis.standard.StandardTokenizerFactory@5703f5b3)"]
```

## Design

Under the hood the library uses the factory classes `TokenizerFactory`, `TokenFilterFactory`, and `CharFilterFactory`.
The factories are loaded with `java.util.ServiceLoader`.
All the available classes are automatically discovered.

If you want to include additional factory classes you need to add to the classpath two things:
 1. Implementation class of one of the Factory classes
 2. Under the `META-INF/services` add e.g. `org.apache.lucene.analysis.TokenFilterFactory` that lists the classes from step 1.

## Future work

- [ ] Conditional token filters

## License

Copyright &copy; 2022 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
