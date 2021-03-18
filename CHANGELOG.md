# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## Unreleased

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

## v2021.01.13

- Removed `--slop` and `--in-order` CLI params as no longer needed.
