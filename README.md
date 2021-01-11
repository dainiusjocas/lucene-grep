# lucene-grep
Grep-like utility based on [Lucene Monitor](https://lucene.apache.org/core/8_3_0/monitor/index.html).

Very limited. Not compatible with `grep`.
Example in grep:
```shell
grep -n -R --include=\*.{edn,clj} "main" ./
=>
./deps.edn:22:   :main-opts   ["-m" "cognitect.test-runner"]}
./deps.edn:24:  {:main-opts  ["-m" "clj-kondo.main --lint src test"]
./deps.edn:26:   :jvm-opts   ["-Dclojure.main.report=stderr"]}
./deps.edn:28:  {:main-opts  ["-m clj.native-image core"
```

Example in lgrep:
```shell
clojure -M -m core main "*.{clj,edn}"
=>
./src/core.clj:44:(defn -main [& args]
./deps.edn:22:   :main-opts   ["-m" "cognitect.test-runner"]}
./deps.edn:24:  {:main-opts  ["-m" "clj-kondo.main --lint src test"]
./deps.edn:28:  {:main-opts  ["-m clj.native-image core"
```

Supports input from STDIN:
```shell
cat README.md | clojure -M -m core --slop=4 -h "monitor lucene"
```

```shell
clojure -M -m core --case-sensitive\?=false --ascii-fold\?=true --stem\?=true --slop=4 --tokenizer=whitespace "lucene" **/*.md
```

# Supported options
```shell
Lucene Monitor based grep-like utility. Usage:
      --case-sensitive? CASE_SENSITIVE  false  If text should be case sensitive
      --ascii-fold? ASCII_FOLDED        true   if text should be ascii folded
      --stem? STEMMED                   true   if text should be stemmed
      --slop SLOP                       0      How far can be words from each other
      --stemmer STEMMER                        Which stemmer to use for stemming
      --tokenizer TOKENIZER                    Tokenizer to use
  -h, --help
```

# Supported tokenizers
TODO

# Supported stemmers
TODO
