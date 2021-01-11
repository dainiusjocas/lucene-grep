# lucene-grep
Grep-like utility based on [Lucene Monitor](https://lucene.apache.org/core/8_7_0/monitor/index.html) compiled with GraalVM native-image.

## Features

- Supports various text tokenizers
- Supports various stemmers for multiple languages
- Matches phrases
- Phrases can be enforced to be matched in order
- Output is colored
- Supports [STDIN](https://en.wikipedia.org/wiki/Standard_streams#Standard_input_(stdin)) as text input
- Supports [GLOB](https://en.wikipedia.org/wiki/Glob_(programming)) file pattern
- Compiled with [GraalVM native-image](https://www.graalvm.org/reference-manual/native-image/) utility
- Fast startup which makes it usable as CLI utility

Startup and memory as measured with `time` utility on my Linux laptop:
<img src="docs/time-memory-usage.png"
alt="Startup time and memory usage" title="Startup time and memory usage" />

The output has a format: `[FILE_PATH]:[LINE_NUMBER]:[LINE_WITH_A_COLORED_HIGHLIGHT]`

NOTE: Not compatible with `grep`. When compared with `grep` the functionality is limited in most aspects.

## Examples 

Example of the `lmgrep`:
```shell
./lmgrep "main" "*.{clj,edn}"
=>
./src/core.clj:44:(defn -main [& args]
./deps.edn:22:   :main-opts   ["-m" "cognitect.test-runner"]}
./deps.edn:24:  {:main-opts  ["-m" "clj-kondo.main --lint src test"]
./deps.edn:28:  {:main-opts  ["-m clj.native-image core"
```

The output is somewhat similar to grep, example:
```shell
grep -n -R --include=\*.{edn,clj} "main" ./
=>
./deps.edn:22:   :main-opts   ["-m" "cognitect.test-runner"]}
./deps.edn:24:  {:main-opts  ["-m" "clj-kondo.main --lint src test"]
./deps.edn:26:   :jvm-opts   ["-Dclojure.main.report=stderr"]}
./deps.edn:28:  {:main-opts  ["-m clj.native-image core"
```

Supports input from STDIN:
```shell
cat README.md | ./lmgrep --slop=4 "monitor lucene"
```

```shell
./lmgrep --case-sensitive\?=false --ascii-fold\?=true --stem\?=true --slop=4 --tokenizer=whitespace "lucene" **/*.md
```

# Supported options
```shell
Lucene Monitor based grep-like utility.
Usage: lmgrep [OPTIONS] PHRASE [FILES]
Supported options:
      --tokenizer TOKENIZER                    Tokenizer to use
      --case-sensitive? CASE_SENSITIVE  false  If text should be case sensitive
      --ascii-fold? ASCII_FOLDED        true   If text should be ascii folded
      --stem? STEMMED                   true   If text should be stemmed
      --stemmer STEMMER                        Which stemmer to use for stemming
      --slop SLOP                       0      How far can be words from each other
      --in-order? IN_ORDER              false  Should the phrase be ordered in matches with a non-zero slop
  -h, --help
```

# Supported tokenizers

Tokenizers are the Lucene tokenizers:
- keyword
- letter
- standard (default)
- unicode-whitespace
- whitespace

Example:
```shell
echo "one.two" | ./lmgrep --tokenizer=letter "one" 
*STDIN*:1:one.two
```

# Supported stemmers

Unless `--stem?=false` is provided these Lucene stemmers are available:
- arabic
- armenian
- basque
- catalan
- danish
- dutch
- english (default)
- estonian
- finnish
- french
- german2
- german
- hungarian
- irish
- italian
- kp
- lithuanian
- lovins
- norwegian
- porter
- portuguese
- romanian
- russian
- spanish
- swedish
- turkish

Example:
```shell
echo "labai gerai" | ./lmgrep --stemmer=lithuanian "labas"                
=>
*STDIN*:1:labai gerai
```

## Development

Requirements: 
- Clojure CLI, 
- GraalVM with the `native-image` tool installed and on `$PATH`
- Docker (just for rebuilding the linux native image).

Build executable on your platform:
```shell
make build
```
This will create a file named `lmgrep`.


## Future work

- [ ] Automate builds for [multiple platforms](https://github.com/dainiusjocas/lucene-grep/issues/9)
- [ ] Optimize highlighting of [multiple lines in batches](https://github.com/dainiusjocas/lucene-grep/issues/3)
- [ ] Exclude files with [GLOB](https://github.com/dainiusjocas/lucene-grep/issues/5)
- [ ] Support other [output formats](https://github.com/dainiusjocas/lucene-grep/issues/8)
- [ ] [Custom coloring](https://github.com/dainiusjocas/lucene-grep/issues/7) would be nice

## License

Copyright &copy; 2021 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
