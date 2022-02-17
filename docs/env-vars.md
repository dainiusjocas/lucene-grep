# Environment Variables

## Build-time

### Functionality

`LMGREP_FEATURE_SNOWBALL`: add a bunch of snowball based token filters, default `true`.
`LMGREP_FEATURE_BUNDLED_ANALYZERS`: adds a bunch of predefined Lucene analyzers, default `true`. 
`LMGREP_FEATURE_CHARSETS`: add all charsets to the native image, default `true`.
`LMGREP_FEATURE_STEMPEL`: add [Stempel](https://lucene.apache.org/core/6_6_0/analyzers-stempel/org/apache/lucene/analysis/stempel/package-summary.html) token filter for the Polish language, default `true`.
`LMGREP_FEATURE_RAUDIKKO`: add [Raudikko](https://github.com/EvidentSolutions/raudikko) token Filter for the Finnish language, default `false`.

### Environment variables that specify what binary should be built

`LMGREP_STATIC`: `true` instructs GraalVM native-image tool to build a [statically linked](https://www.graalvm.org/22.0/reference-manual/native-image/StaticImages/) binary, only for Linux. 
`LMGREP_MUSL`: instructs GraalVM native-image to statically link against [musl-libc](https://musl.libc.org/), only for Linux.
`LMGREP_FEATURE_EPSILON_GC`: use [Epsilon GC](https://www.graalvm.org/22.0/reference-manual/native-image/MemoryManagement/) in the binary.

## Runtime

`DEBUG_MODE`: when `true` then on error prints full stack trace, default `false`.
