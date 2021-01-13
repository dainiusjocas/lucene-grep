# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## v2021.01.13

### New

- Support for Lucene query syntax

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
