package org.apache.lucene.analysis.tr;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.tartarus.snowball.ext.TurkishStemmer;

import java.util.Map;

public class TurkishSnowballStemTokenFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "turkishSnowballStem";

    /** Creates a new TurkishSnowballStemTokenFilterFactory */
    public TurkishSnowballStemTokenFilterFactory(Map<String,String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    public TurkishSnowballStemTokenFilterFactory() {
        throw defaultCtorException();
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new SnowballFilter(input, new TurkishStemmer());
    }
}
