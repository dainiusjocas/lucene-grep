package org.apache.lucene.analysis.lt;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.tartarus.snowball.ext.LithuanianStemmer;

import java.util.Map;

/**
 * Factory for {@link SnowballFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_ltsnowstem" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.StandardTokenizerFactory"/&gt;
 *     &lt;filter class="solr.LowerCaseFilterFactory"/&gt;
 *     &lt;filter class="solr.LithuanianSnowballStemFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * @lucene.spi {@value #NAME}
 */
public class LithuanianSnowballStemTokenFilterFactory extends TokenFilterFactory  {

    /** SPI name */
    public static final String NAME = "lithuanianSnowballStem";

    /** Creates a new LithuanianSnowballStemFilterFactory */
    public LithuanianSnowballStemTokenFilterFactory(Map<String,String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    public LithuanianSnowballStemTokenFilterFactory() {
        throw defaultCtorException();
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new SnowballFilter(input, new LithuanianStemmer());
    }
}
