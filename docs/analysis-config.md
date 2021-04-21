# Analysis Configuration

## Analysis Configuration Chain

The Lucene analyzer is created exclusively from the analyzer configuration map (ACM).

There are two ways to specify configuration:
1. ACM (the preferred way),
2. Analysis flags.

Priority, from high to low:
3. questionnaire file conf (either flags or ACM),
2. global analysis conf (either flags or ACM),
1. default application conf (ACM). TODO: dainius

Procedure of deriving the analysis configuration:
-| take default application conf ACM -> (1)
-| take global analysis flags (2.1) and convert to ACM -> (2.1.1)
-| take global analysis ACM (2.2) and merge with the (2.1.1) -> (2.2.2)
-| merge (1) with (2.2.2) and call it global-analysis-conf (A)
- take questionnaire entry flags (3.1) and convert them to ACM -> (3.1.1)
- take questionnaire entry ACM (3.2) and merge with (3.1.1) -> (3.2.2)
- merge (A) with (3.2.2) to get the final ACM (B)
  Shortcuts:
  -> When no flags (either (2.1) or (3.1)) are provided then only respective ACM is used
  -> When no global flags (2.1) are provided and (2.2) is provided then use (2.2) (i.e. ignore the (1))
  -> When no global flags (2.1) or ACM (2.2) are provided and questionnaire entry has only ACM (3.2), ignore (1) and use (3.2)

If flags are not provided then ACMs are used.
ACMs are not merged, meaning that:
- if (2.2) and (3.2) are provided then (3.2) is used and (1) is ignored
- if (2.2) is provided and (3.2) is not then (2.2) is used and (1) is ignored
- if neither (2.2) not (3.2) are provided then (1) is used

Flags are interesting only when they specify tokenizer, remove or change token filters.

Flags config (when provided) has this token filter order:
- tokenizer (default: "standard")
- WordDelimiterGraphFilter (default: 0)
- lowercase (default: true)
- ascii folding (default: true)
- stemming (default: true)

ACM is more specific, therefore, always wins.

Collect only provided flags.