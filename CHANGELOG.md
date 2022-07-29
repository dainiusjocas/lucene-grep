# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## Unreleased

### Fixed/enhanced

- Upgrade to Lucene 9.3.0
- Upgrade GraalVM to 22.2.0

## v2022.07.27

### Fixed/enhanced

- Upgrade to Lucene 9.2.0

## v2022.05.12 

### Fixed/enhanced

- Lucene custom analyzer as an external library
- Lucene custom text analysis as an external library 
- Lucene query parsing as an external library
- Deploying to the DockerHub
- `--presearcher` flag to specify [Lucene Monitor Presearcher](https://lucene.apache.org/core/9_1_0/monitor/org/apache/lucene/monitor/Presearcher.html)
- more efficient `MonitorQuerySerializer`

## v2022.05.01

### Fixed/enhanced

- Native build for the Apple Silicon
- Upgrade to Lucene 9.1.0
- Graalvm Native image "quick build mode" for dev
- Update to GraalVM 22.1.0
- Text analysis as a separate module
- Eager processing of Lucene Monitor matches which makes ~20% throughput improvement.
- Change the default presearcher to NO_FILTERING, less RAM

## v2022.02.19

- Streamed matching with a flag `--streamed`

## v2022.02.18

- Fixed flushing behaviour when consuming from STDIN in the ruby-percolator example.
- `LMGREP_FEATURE_EPSILON_GC`: use [Epsilon GC](https://www.graalvm.org/22.0/reference-manual/native-image/MemoryManagement/) in the binary.

## v2022.02.17

## Fixed/enhanced

- Fixed the Windows release file name construction in Github Actions
- Remove clojure.tools.logging

## v2022.02.14

## Fixed/enhanced

- ~3x throughput improvements for matching!
- `--only-analyze` with `--no-preserve-order` prevent OOM
- `--only-analyze` option `--queue-size` to specify the Java executor service queue size
- Flag `--show-analysis-components` to print out a list of available text analysis components
- `--only-analyze` with `--preserve-order` based on Java [ExecutorService](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ExecutorService.html)

## v2022.02.10

## Fixed/enhanced

- Modular architecture
- Build uberjar with [Clojure tools.build](https://clojure.org/guides/tools_build) 
- Release with the official GraalVM Github Actions
- Update to Java 17
- Update to GraalVM 22

## v2021.12.09

### Fixed/enhanced

- Update Lucene to 9.0.0
- Workaround for the Linux static image compilation

## v2021.11.09

## New

- Flag `--query-update-buffer-size` to control how many queries are indexed at once

## Fixed

- Global query parser settings are applied for queries in a file

## v2021.11.08

## New

- [Raudikko](https://github.com/EvidentSolutions/raudikko) token filter for Finnish Language as an option
- Flag `--config-dir` to specify directory where text analysis resources are stored.
- `synonym` token filter
- Flag `--analyzers-file` for custom analyzers definitions.

## Fixed/enhanced

- Refactored custom TokenFilterFactories to act like services

## v2021.11.01

## New

- Flag to specify the input matching concurrency `--concurrency`
- Flag `--[no-]preserve-order` that attempts to increase throughput while not preserving the order, applicable to `--only-analyze`
- Flag `--reader-buffer-size` in bytes
- Flag `--writer-buffer-size` in bytes
- Flag `--graph` to modify `--only-analyze` that output a dot program for visualization of a token stream
- Flag to limit the depth of file system traversal `--max-depth`
- Flag to skip hidden files `--[no-]hidden`
- Add support for the `org.apache.lucene.analysis.miscellaneous.ConcatenateGraphFilterFactory` token filter
- Add support for the `org.apache.lucene.analysis.en.LovinsSnowballStemTokenFilterFactory` token filter
- Add support for the `org.apache.lucene.analysis.miscellaneous.DropIfFlaggedFilterFactory` token filter
- Add support for the `org.apache.lucene.analysis.pattern.PatternTypingFilter` token filter
- Support configuration of various query parsers with a flag `--query-parser-conf`

## Fixed/enhanced

- Updated to Lucene 8.10.1
- Updated to GraalVM 21.3.0
- Updated to quarkus-lucene 0.3 
- Faster file system search with GLOB

## v2021.05.23

## New

- Support for multiple Lucene query parsers with flag`--query-parser`

# Fixed/enhanced

- MacOS and Linux installation via `brew`
- Base static linux build on musl. The binary should now work in most linuxes out of the box.
- Upgrade to graalvm-ce:21.1.0
- Query parsing errors propagates exceptions

## v2021.05.02

# Fixed/enhanced

- `--explain` flag for the `--only-analyze` to return details of tokens
- `synonymgraph` token filter now can load synonyms from files
- `hyphenationcompoundword` token filter now can load data from files
- Windows installer based on scoop
- Handle autocompleted files in Windows

## Fixed/enhanced

- Hyperlinking on STDIN throws a NPE [#92](https://github.com/dainiusjocas/lucene-grep/issues/92)

## v2021.04.26

## Fixed/enhanced

- Snowball token filter classes are included into native binary
- Exceptions in clojure.core.async pipeline are handled properly
- Tests for native-image

## v2021.04.23

## Fixed/enhanced

- Expose a declarative text analysis pipeline specification in JSON (see #81)
- Provide a list of predefined analyzers
- Support customizable char Filters
- Support customizable tokenizers
- Support customizable token filters  
- Use Java PrintWriter to write to STDOUT (see #79)

## v2021.04.06

## New

- Flag to only analyze text and spit to stdout `--only-analyze`
- Lucene Monitor is executed in another thread
- Optimized loading of large query dictionaries

## v2021.03.24

## New

- Scored Highlights with a flag `--with-scored-highlights`
- Flag to force printing an empty line on no match `--with-empty-lines`
- Flag to output matching details, `--with-details`

## v2021.03.18

## Fixed/enhanced

- Handle the zero queries input

## v2021.03.17

### New

- Allows specifying multiple queries with the `--query` flags.
- `--queries-file` flag to provide a JSON file with Lucene queries.

## Fixed/enhanced

- Use the `io.quarkiverse.lucene/quarkus-lucene` extension instead of building from source
- Exit status should be 0 when -h is specified
- Wait for the input from STDIN

## v2021.02.28

### New

- Display file path as hyperlinks with the `--hyperlink` flag

### Fixed/enhanced

- Errors when matching directory

## v2021.02.06

### Changed

- Accepts multiple files (#30)
- A proper static binary builder Docker container
- WordDelimiterGraphFilter added as an optional filter

## v2021.02.02

### Changed

- Stable Clojure release 1.10.2
- Remove redundant options from native-image building script 
- Flag to search in whole files

## v2021.01.24

### New

- Compile Linux executable with `--static` flag
- Dockerfile to build static Linux executables
- Option to skip binary files on Linux and MacOS
- Updated the native-image docker to 21.0.0

### v2021.01.15

### New

- Support for output formats JSON and EDN
- Support for output template when format is string
- Support for custom highlighting tags
- Validation of the CLI options
- Building an uberjar
- Scoring (disables highlighting and tags)
- Exclude files with GLOB

## v2021.01.13

### New

- Support for Lucene query syntax

* chore: version number of previous release
### Fixed/enhanced

- Fixed GLOB patterns so that the full file path should match

### Changed

- Removed `--slop` and `--in-order` CLI params as no longer needed.

## v2021.01.11

### New

- Initial [release](https://github.com/dainiusjocas/lucene-grep/pull/10)
- Quarkus Lucene extension
- Matching every line of provided text
- text analysis
- Coloring output
- GLOB support
- STDIN support
- CLI params for text analysis

## Breaking changes

## Unreleased

- queries file entries no longer support these keys: 
`[:case-sensitive? :ascii-fold? :stem? :tokenizer :stemmer :word-delimiter-graph-filter]`

## v2021.05.02

- removed `synonym` token filter, because deprecated and required a patch.

## v2021.04.26

- Removed token filters "lovinssnowballstem" "concatenategraph" because of compilation [issues](https://github.com/dainiusjocas/lucene-grep/issues/86)

## v2021.01.13

- Removed `--slop` and `--in-order` CLI params as no longer needed.
