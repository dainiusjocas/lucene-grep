Go [here](https://github.com/dainiusjocas/lucene-grep/releases/tag/v2021.03.24) and fetch the binary for your platform to this directory.

Extract the binary, e.g. `unzip lmgrep*` and make sure that the binary is in your `$PATH`.

Then run the example:

```
ruby ruby-percolator.rb
```

The output should be similar to:

```
Given the Percolator dictionary: [{"query"=>"jump"}, {"query"=>"\"quick fox\"~2^3"}]

Checks if the text matches:
Percolator on text 'The quick brown fox jumps over the lazy dog' matches: true, in: 0.008346395s
Percolator on text 'not matching' matches: false, in: 0.000280168s

>>>The matches in are returned<<<
Percolator on text 'The quick brown fox jumps over the lazy dog' matched: '{"line-number"=>3, "line"=>"The quick brown fox jumps over the lazy dog", "score"=>0.6384387910366058, "highlights"=>[{"type"=>"QUERY", "dict-entry-id"=>"1", "meta"=>{}, "score"=>0.13076457, "begin-offset"=>20, "end-offset"=>25, "query"=>"jump"}, {"type"=>"QUERY", "dict-entry-id"=>"2", "meta"=>{}, "score"=>0.5076742, "begin-offset"=>4, "end-offset"=>19, "query"=>"\"quick fox\"~2^3"}]}' in: 0.000532075s
Percolator on text 'not matching' matched: '' in: 0.00021151s
>>>Percolator is closed.<<<
```

The trick here is that the percolator does stemming. See query "jump".

Also, the percolator supports additive scoring of matching query clauses.

Cheers!
