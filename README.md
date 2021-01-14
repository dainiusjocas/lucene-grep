# lucene-grep
Grep-like utility based on [Lucene Monitor](https://lucene.apache.org/core/8_7_0/monitor/index.html) compiled with GraalVM native-image.

## Features

- Supports Lucene query syntax as described [here](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html)
- Supports various text tokenizers
- Supports various term stemmers for multiple languages
- Text output is colored
- Output can be formatted as JSON of EDN
- Supports [STDIN](https://en.wikipedia.org/wiki/Standard_streams#Standard_input_(stdin)) as text input
- Supports [GLOB](https://en.wikipedia.org/wiki/Glob_(programming)) [file pattern](https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-)
- Compiled with [GraalVM native-image](https://www.graalvm.org/reference-manual/native-image/) tool
- Supports Linux, MacOS, and Windows
- Fast startup which makes it usable as CLI utility

Startup and memory as measured with `time` utility on my Linux laptop:
<img src="docs/time-memory-usage.png"
alt="Startup time and memory usage" title="Startup time and memory usage" />

The output has a format: `[FILE_PATH]:[LINE_NUMBER]:[LINE_WITH_A_COLORED_HIGHLIGHT]`

NOTE: Not compatible with `grep`. When compared with `grep` the functionality is limited in most aspects.

## Quickstart

Grab a binary from [Github releases](https://github.com/dainiusjocas/lucene-grep/releases), extract, and place it anywhere on the path.

In case you're running MacOS then give run permissions for the executable binary:
```shell
sudo xattr -r -d com.apple.quarantine lmgrep
```

Then run it:
```shell
echo "Lucene is awesome" | time ./lmgrep "Lucene"
```

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

The output is somewhat similar to `grep`, example:
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
cat README.md | ./lmgrep "monitor lucene"
```
TIP: write your Lucene query within double quotes.

Various options with GLOB file pattern example:
```shell
./lmgrep --case-sensitive\?=false --ascii-fold\?=true --stem\?=true --tokenizer=whitespace "lucene" "**/*.md"
```
TIP: write GLOB file patterns within double quotes.

## Deviations from Lucene query syntax

- The field names are not supported because there are no field names in a line of text.

# Supported options
```shell
Lucene Monitor based grep-like utility.
Usage: lmgrep [OPTIONS] LUCENE_QUERY [FILES]
Supported options:
      --tokenizer TOKENIZER                    Tokenizer to use, one of: [keyword letter standard unicode-whitespace whitespace]
      --case-sensitive? CASE_SENSITIVE  false  If text should be case sensitive
      --ascii-fold? ASCII_FOLDED        true   If text should be ascii folded
      --stem? STEMMED                   true   If text should be stemmed
      --stemmer STEMMER                        Which stemmer to use for token stemming, one of: [arabic armenian basque catalan danish dutch english estonian finnish french german german2 hungarian irish italian kp lithuanian lovins norwegian porter portuguese romanian russian spanish swedish turkish]
      --format FORMAT                          How the output should be formatted, one of: [edn json string]
  -h, --help
```

NOTE: question marks in `zsh` shell must be escaped, e.g. `--case-sensitive\?=true` or within double quotes e.g. `"--case-sensitive?=true"` 

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

## Phrase Matching with Slop

To match a phrase you need to put it in double quotes:
```shell
echo "GraalVM is awesome" | ./lmgrep "\"graalvm is\""
=>
*STDIN*:1:GraalVM is awesome
```

By default, when phrase terms are not exactly one after another there is no match, e.g.:
```shell
echo "GraalVM is awesome" | ./lmgrep "\"graalvm awesome\""
=>
```

We can provide a slop parameter i.e. `~2` to allow some number of "substitutions" of terms in the document text, e.g.:
```shell
echo "GraalVM is awesome" | ./lmgrep "\"graalvm awesome\"~2"
=>
*STDIN*:1:GraalVM is awesome
```

As a side effect, when the slop is big enough terms can match out of order, e.g.:
```shell
echo "GraalVM is awesome" | ./lmgrep "\"awesome graalvm\"~3"
=>
*STDIN*:1:GraalVM is awesome
```
However, if order is important there is no way to enforce it Lucene query syntax.

## Development

Requirements: 
- Clojure CLI
- Maven
- GraalVM with the `native-image` tool installed and on `$PATH`
- GNU Make
- Docker (just for rebuilding the linux native image).

Install the Quarkus Lucene extension from source:
```shell
git clone https://github.com/gunnarmorling/search.morling.dev.git
cd search.morling.dev/quarkus-lucene-extension
mvn install
```

Build executable for your platform:
```shell
make build
```
It will create an executable binary file named `lmgrep` stored at the root directory of the repository.

Run the tests:
```shell
make test
```

Lint the core with clj-kondo:
```shell
make lint
```

## Future work

- [ ] Automate builds for [multiple platforms](https://github.com/dainiusjocas/lucene-grep/issues/9)
- [ ] Optimize highlighting of [multiple lines in batches](https://github.com/dainiusjocas/lucene-grep/issues/3)
- [ ] Exclude files with [GLOB](https://github.com/dainiusjocas/lucene-grep/issues/5)
- [ ] Support other [output formats](https://github.com/dainiusjocas/lucene-grep/issues/8)
- [ ] [Custom coloring](https://github.com/dainiusjocas/lucene-grep/issues/7) would be nice

## License

Copyright &copy; 2021 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
