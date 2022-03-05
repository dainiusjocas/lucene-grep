package org.apache.lucene.analysis.fi;

import fi.evident.raudikko.Analyzer;
import fi.evident.raudikko.Morphology;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.fi.RaudikkoTokenFilter;

import java.util.Map;

public class RaudikkoTokenFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "raudikko";

    final boolean analyzeAll;
    final int minimumWordSize;
    final int maximumWordSize;
    public static final String ANALYZE_ALL_KEY = "analyzeAll";
    public static final String MINIMUM_WORD_SIZE_KEY = "minimumWordSize";
    public static final String MAXIMUM_WORD_SIZE_KEY = "maximumWordSize";
    private final Morphology morphology;
    private final Analyzer analyzer;


    /** Creates a new RaudikkoTokenFilterFactory */
    public RaudikkoTokenFilterFactory(Map<String,String> args) {
        super(args);
        analyzeAll = getBoolean(args, ANALYZE_ALL_KEY, true);
        minimumWordSize = getInt(args, MINIMUM_WORD_SIZE_KEY, 3);
        maximumWordSize = getInt(args, MAXIMUM_WORD_SIZE_KEY, 100);
        morphology = Morphology.loadBundled();
        analyzer = morphology.newAnalyzer();
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    public RaudikkoTokenFilterFactory() {
        throw defaultCtorException();
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new RaudikkoTokenFilter(input, analyzer, analyzeAll, minimumWordSize, maximumWordSize);
    }
}
