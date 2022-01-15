package org.apache.lucene.analysis.en;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.tartarus.snowball.ext.LovinsStemmer;

import java.util.Map;

public class LovinsSnowballStemTokenFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "lovinsSnowballStem";

    /** Creates a new LovinsSnowballStemTokenFilterFactory */
    public LovinsSnowballStemTokenFilterFactory(Map<String,String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    public LovinsSnowballStemTokenFilterFactory() {
        throw defaultCtorException();
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new SnowballFilter(input, new LovinsStemmer());
    }
}
