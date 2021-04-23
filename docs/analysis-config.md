# Analysis Configuration

## Analysis Configuration Chain

The Lucene analyzer is created from the analyzer configuration map (ACM).

There are two ways to provide configuration for analyzer constructor:
1. ACM (preferred way),
2. Analysis flags (legacy).

Priority, from high to low:
3. questionnaire file conf (either FLAGS3 or ACM2),
2. global analysis conf (either FLAGS2 or ACM2),
1. default application conf (ACM1).

ACM math and merging is not a thing.

Algorithm to calculate ACM for analyzer constructor:
```clojure
(let [override (fn [acm flags] (if (empty? flags) acm (override-acm acm flags)))
      global-analysis-acm (if ACM2 ACM2 (override ACM1 FLAGS2))]
  (if ACM3
    AMC3
    (override global-analysis-acm FLAGS3)))
```

In plain English the algorithm says that when an ACM is provided it is used. 
Otherwise, the ACM from the lower level is overridden with flags of the current level.

Flags config (when provided) has this order:
- tokenizer (default: "standard")
- WordDelimiterGraphFilter (default: 0)
- lowercase (default: true)
- ascii folding (default: true)
- stemming (default: true)
