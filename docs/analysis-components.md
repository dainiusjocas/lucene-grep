# Text Analysis Components

`lmgrep` provides a rich collection of the Lucene analysis components out of the box: char filters, tokenizers, token filters.

Some components accept additional arguments, they are key-value pairs under `args`, e.g. see `patternReplace`:
```shell
echo "FOO foo" | \
  ./lmgrep \
  --only-analyze \
  --analysis='
  {
    "char-filters": [
      {
        "name": "patternReplace",
         "args": {
           "pattern": "foo",
           "replacement": "bar"
        }
      }
    ]
  }
  '
=>
["FOO","bar"]
```

To learn more about each analysis component Google for `Lucene <Class Name>`.
Exceptions are all Snowball stemmers because they are not part of the Standard Lucene distribution.
They are just `Factory` wrappers for the standard Lucene snowball stemmers. 
They are provided for backwards compatibility.

Name of the analysis components is case-insensitive.

## Char Filters

|          Name  |                                                              Class |
|---------------:|--------------------------------------------------------------------|
|      htmlstrip |   org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory |
|        mapping |     org.apache.lucene.analysis.charfilter.MappingCharFilterFactory |
|       cjkwidth |           org.apache.lucene.analysis.cjk.CJKWidthCharFilterFactory |
|        persian |             org.apache.lucene.analysis.fa.PersianCharFilterFactory |
| patternreplace | org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory |

## Tokenizers

|              Name |                                                                 Class |
|-------------------:|:----------------------------------------------------------------------|
|      simplepattern |      org.apache.lucene.analysis.pattern.SimplePatternTokenizerFactory |
|           standard |          org.apache.lucene.analysis.standard.StandardTokenizerFactory |
|          wikipedia |        org.apache.lucene.analysis.wikipedia.WikipediaTokenizerFactory |
|          edgengram |            org.apache.lucene.analysis.ngram.EdgeNGramTokenizerFactory |
|            classic |           org.apache.lucene.analysis.standard.ClassicTokenizerFactory |
|               thai |                    org.apache.lucene.analysis.th.ThaiTokenizerFactory |
|            keyword |               org.apache.lucene.analysis.core.KeywordTokenizerFactory |
|            pattern |            org.apache.lucene.analysis.pattern.PatternTokenizerFactory |
|      uax29urlemail |     org.apache.lucene.analysis.standard.UAX29URLEmailTokenizerFactory |
| simplepatternsplit | org.apache.lucene.analysis.pattern.SimplePatternSplitTokenizerFactory |
|         whitespace |            org.apache.lucene.analysis.core.WhitespaceTokenizerFactory |
|              ngram |                org.apache.lucene.analysis.ngram.NGramTokenizerFactory |
|             letter |                org.apache.lucene.analysis.core.LetterTokenizerFactory |
|      pathhierarchy |         org.apache.lucene.analysis.path.PathHierarchyTokenizerFactory |

## Token Filters

|                     Name |                                                                            Class |
|---------------------------:|----------------------------------------------------------------------------------|
|               commongrams |                   org.apache.lucene.analysis.commongrams.CommonGramsFilterFactory |
|                 hindistem |                              org.apache.lucene.analysis.hi.HindiStemFilterFactory |
|                      stop |                                 org.apache.lucene.analysis.core.StopFilterFactory |
|        spanishminimalstem |                     org.apache.lucene.analysis.es.SpanishMinimalStemFilterFactory |
|        norwegianlightstem |                     org.apache.lucene.analysis.no.NorwegianLightStemFilterFactory |
|       germannormalization |                    org.apache.lucene.analysis.de.GermanNormalizationFilterFactory |
|          spanishlightstem |                       org.apache.lucene.analysis.es.SpanishLightStemFilterFactory |
|           limittokencount |             org.apache.lucene.analysis.miscellaneous.LimitTokenCountFilterFactory |
|            indonesianstem |                         org.apache.lucene.analysis.id.IndonesianStemFilterFactory |
|        hindinormalization |                     org.apache.lucene.analysis.hi.HindiNormalizationFilterFactory |
|                      trim |                        org.apache.lucene.analysis.miscellaneous.TrimFilterFactory |
|           frenchlightstem |                        org.apache.lucene.analysis.fr.FrenchLightStemFilterFactory |
|             typeaspayload |               org.apache.lucene.analysis.payloads.TypeAsPayloadTokenFilterFactory |
|              fixedshingle |                      org.apache.lucene.analysis.shingle.FixedShingleFilterFactory |
|      norwegianminimalstem |                   org.apache.lucene.analysis.no.NorwegianMinimalStemFilterFactory |
|                   minhash |                           org.apache.lucene.analysis.minhash.MinHashFilterFactory |
|          delimitedpayload |            org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilterFactory |
|            daterecognizer |              org.apache.lucene.analysis.miscellaneous.DateRecognizerFilterFactory |
|                 greekstem |                              org.apache.lucene.analysis.el.GreekStemFilterFactory |
|             keywordrepeat |               org.apache.lucene.analysis.miscellaneous.KeywordRepeatFilterFactory |
|             keywordmarker |               org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory |
|                 lowercase |                            org.apache.lucene.analysis.core.LowerCaseFilterFactory |
|            delimitedboost |                 org.apache.lucene.analysis.boost.DelimitedBoostTokenFilterFactory |
|         irishsnowballstem |                 org.apache.lucene.analysis.ga.IrishSnowballStemTokenFilterFactory |
|               latvianstem |                            org.apache.lucene.analysis.lv.LatvianStemFilterFactory |
|                   shingle |                           org.apache.lucene.analysis.shingle.ShingleFilterFactory |
|         frenchminimalstem |                      org.apache.lucene.analysis.fr.FrenchMinimalStemFilterFactory |
|          commongramsquery |              org.apache.lucene.analysis.commongrams.CommonGramsQueryFilterFactory |
|    dictionarycompoundword |      org.apache.lucene.analysis.compound.DictionaryCompoundWordTokenFilterFactory |
|      persiannormalization |                   org.apache.lucene.analysis.fa.PersianNormalizationFilterFactory |
|               fingerprint |                 org.apache.lucene.analysis.miscellaneous.FingerprintFilterFactory |
|                soranistem |                            org.apache.lucene.analysis.ckb.SoraniStemFilterFactory |
|        indicnormalization |                     org.apache.lucene.analysis.in.IndicNormalizationFilterFactory |
|            numericpayload |              org.apache.lucene.analysis.payloads.NumericPayloadTokenFilterFactory |
|              hunspellstem |                     org.apache.lucene.analysis.hunspell.HunspellStemFilterFactory |
|        worddelimitergraph |          org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory |
|                 edgengram |                           org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory |
|         dutchsnowballstem |                 org.apache.lucene.analysis.nl.DutchSnowballStemTokenFilterFactory |
|      armeniansnowballstem |              org.apache.lucene.analysis.hy.ArmenianSnowballStemTokenFilterFactory |
|          turkishlowercase |                       org.apache.lucene.analysis.tr.TurkishLowerCaseFilterFactory |
|      romaniansnowballstem |              org.apache.lucene.analysis.ro.RomanianSnowballStemTokenFilterFactory |
|        hungarianlightstem |                     org.apache.lucene.analysis.hu.HungarianLightStemFilterFactory |
|       patterncapturegroup |               org.apache.lucene.analysis.pattern.PatternCaptureGroupFilterFactory |
|             brazilianstem |                          org.apache.lucene.analysis.br.BrazilianStemFilterFactory |
|          removeduplicates |       org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilterFactory |
|       arabicnormalization |                    org.apache.lucene.analysis.ar.ArabicNormalizationFilterFactory |
|         germanminimalstem |                      org.apache.lucene.analysis.de.GermanMinimalStemFilterFactory |
|             typeassynonym |               org.apache.lucene.analysis.miscellaneous.TypeAsSynonymFilterFactory |
|       soraninormalization |                   org.apache.lucene.analysis.ckb.SoraniNormalizationFilterFactory |
|           germanlightstem |                        org.apache.lucene.analysis.de.GermanLightStemFilterFactory |
|                   classic |                          org.apache.lucene.analysis.standard.ClassicFilterFactory |
|            capitalization |              org.apache.lucene.analysis.miscellaneous.CapitalizationFilterFactory |
|          fixbrokenoffsets |            org.apache.lucene.analysis.miscellaneous.FixBrokenOffsetsFilterFactory |
|                arabicstem |                             org.apache.lucene.analysis.ar.ArabicStemFilterFactory |
|             bulgarianstem |                          org.apache.lucene.analysis.bg.BulgarianStemFilterFactory |
|   hyphenationcompoundword |     org.apache.lucene.analysis.compound.HyphenationCompoundWordTokenFilterFactory |
|                 cjkbigram |                             org.apache.lucene.analysis.cjk.CJKBigramFilterFactory |
|       catalansnowballstem |               org.apache.lucene.analysis.ca.CatalanSnowballStemTokenFilterFactory |
|           stemmeroverride |             org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilterFactory |
|                   elision |                              org.apache.lucene.analysis.util.ElisionFilterFactory |
|               bengalistem |                            org.apache.lucene.analysis.bn.BengaliStemFilterFactory |
|       turkishsnowballstem |               org.apache.lucene.analysis.tr.TurkishSnowballStemTokenFilterFactory |
|        englishminimalstem |                     org.apache.lucene.analysis.en.EnglishMinimalStemFilterFactory |
|              decimaldigit |                         org.apache.lucene.analysis.core.DecimalDigitFilterFactory |
|                  cjkwidth |                              org.apache.lucene.analysis.cjk.CJKWidthFilterFactory |
|       galicianminimalstem |                    org.apache.lucene.analysis.gl.GalicianMinimalStemFilterFactory |
|          russianlightstem |                       org.apache.lucene.analysis.ru.RussianLightStemFilterFactory |
|            kpsnowballstem |                    org.apache.lucene.analysis.nl.KPSnowballStemTokenFilterFactory |
|                    length |                      org.apache.lucene.analysis.miscellaneous.LengthFilterFactory |
|                  truncate |               org.apache.lucene.analysis.miscellaneous.TruncateTokenFilterFactory |
|         englishpossessive |                      org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory |
|          italianlightstem |                       org.apache.lucene.analysis.it.ItalianLightStemFilterFactory |
|        basquesnowballstem |              org.apache.lucene.analysis.et.EstonianSnowballStemTokenFilterFactory |
|       scandinavianfolding |         org.apache.lucene.analysis.miscellaneous.ScandinavianFoldingFilterFactory |
|                porterstem |                             org.apache.lucene.analysis.en.PorterStemFilterFactory |
|            portuguesestem |                         org.apache.lucene.analysis.pt.PortugueseStemFilterFactory |
|                      type |                            org.apache.lucene.analysis.core.TypeTokenFilterFactory |
|      bengalinormalization |                   org.apache.lucene.analysis.bn.BengaliNormalizationFilterFactory |
|                 czechstem |                              org.apache.lucene.analysis.cz.CzechStemFilterFactory |
|        tokenoffsetpayload |          org.apache.lucene.analysis.payloads.TokenOffsetPayloadTokenFilterFactory |
|              synonymgraph |                      org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory |
|         stempelpolishstem |                 org.apache.lucene.analysis.stempel.StempelPolishStemFilterFactory |
| scandinaviannormalization |   org.apache.lucene.analysis.miscellaneous.ScandinavianNormalizationFilterFactory |
|          limittokenoffset |            org.apache.lucene.analysis.miscellaneous.LimitTokenOffsetFilterFactory |
|            irishlowercase |                         org.apache.lucene.analysis.ga.IrishLowerCaseFilterFactory |
|          swedishlightstem |                       org.apache.lucene.analysis.sv.SwedishLightStemFilterFactory |
|     portugueseminimalstem |                  org.apache.lucene.analysis.pt.PortugueseMinimalStemFilterFactory |
|             protectedterm |               org.apache.lucene.analysis.miscellaneous.ProtectedTermFilterFactory |
|              flattengraph |                         org.apache.lucene.analysis.core.FlattenGraphFilterFactory |
|            greeklowercase |                         org.apache.lucene.analysis.el.GreekLowerCaseFilterFactory |
|             reversestring |                     org.apache.lucene.analysis.reverse.ReverseStringFilterFactory |
|                     kstem |                                  org.apache.lucene.analysis.en.KStemFilterFactory |
|       portugueselightstem |                    org.apache.lucene.analysis.pt.PortugueseLightStemFilterFactory |
|                 uppercase |                            org.apache.lucene.analysis.core.UpperCaseFilterFactory |
|        limittokenposition |          org.apache.lucene.analysis.miscellaneous.LimitTokenPositionFilterFactory |
|    delimitedtermfrequency | org.apache.lucene.analysis.miscellaneous.DelimitedTermFrequencyTokenFilterFactory |
|                apostrophe |                             org.apache.lucene.analysis.tr.ApostropheFilterFactory |
|      serbiannormalization |                   org.apache.lucene.analysis.sr.SerbianNormalizationFilterFactory |
|            patternreplace |                    org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory |
|            snowballporter |                   org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory |
|                     ngram |                               org.apache.lucene.analysis.ngram.NGramFilterFactory |
|          finnishlightstem |                       org.apache.lucene.analysis.fi.FinnishLightStemFilterFactory |
|             worddelimiter |               org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory |
|            codepointcount |              org.apache.lucene.analysis.miscellaneous.CodepointCountFilterFactory |
|              galicianstem |                           org.apache.lucene.analysis.gl.GalicianStemFilterFactory |
|                germanstem |                             org.apache.lucene.analysis.de.GermanStemFilterFactory |
|              asciifolding |                org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory |
|                  keepword |                    org.apache.lucene.analysis.miscellaneous.KeepWordFilterFactory |
|           hyphenatedwords |             org.apache.lucene.analysis.miscellaneous.HyphenatedWordsFilterFactory |
|    lithuaniansnowballstem |            org.apache.lucene.analysis.lt.LithuanianSnowballStemTokenFilterFactory |
|        danishsnowballstem |                org.apache.lucene.analysis.da.DanishSnowballStemTokenFilterFactory |
|          concatenategraph |            org.apache.lucene.analysis.miscellaneous.ConcatenateGraphFilterFactory |
