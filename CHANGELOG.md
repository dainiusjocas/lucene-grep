# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## Unreleased

## Fixed/enhanced

- Updated to Lucene 8.9.0
- Check if the glob is actually a file.
- Flag to specify the input matching concurrency `--concurrency`
- Faster file system search with GLOB
- Flag to limit the depth of file system traversal`--max-depth`
- Flag to skip hidden files `--[no-]hidden`
- Add support for the `org.apache.lucene.analysis.miscellaneous.DropIfFlaggedFilterFactory` token filter
- Add support for the `org.apache.lucene.analysis.pattern.PatternTypingFilter` token filter

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

## v2021.05.02

- removed `synonym` token filter, because deprecated and required a patch.

## v2021.04.26

- Removed token filters "lovinssnowballstem" "concatenategraph" because of compilation [issues](https://github.com/dainiusjocas/lucene-grep/issues/86)

## v2021.01.13

- Removed `--slop` and `--in-order` CLI params as no longer needed.
