Go [here](https://github.com/dainiusjocas/lucene-grep/releases/tag/v2022.02.19) and fetch the binary for your platform to this directory.

Extract the binary, e.g. `unzip lmgrep*` and make sure that the binary is in your `$PATH`.

Then run the example:

```
python percolator.py
```

The output should be similar to:

```shell
MATCH:>>>*STDIN*:1:The quick brown fox jumps over the lazy dog<<<
Line: >>>not matching<<< didn't match.

All lines were processed.
```

Cheers!
