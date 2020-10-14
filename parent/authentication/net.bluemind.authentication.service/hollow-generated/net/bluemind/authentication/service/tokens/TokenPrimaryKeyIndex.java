package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

/**
 * @deprecated see {@link com.netflix.hollow.api.consumer.index.UniqueKeyIndex} which can be built as follows:
 * <pre>{@code
 *     UniqueKeyIndex<Token, K> uki = UniqueKeyIndex.from(consumer, Token.class)
 *         .usingBean(k);
 *     Token m = uki.findMatch(k);
 * }</pre>
 * where {@code K} is a class declaring key field paths members, annotated with
 * {@link com.netflix.hollow.api.consumer.index.FieldPath}, and {@code k} is an instance of
 * {@code K} that is the key to find the unique {@code Token} object.
 */
@Deprecated
@SuppressWarnings("all")
public class TokenPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<TokensAPI, Token> implements HollowUniqueKeyIndex<Token> {

    public TokenPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public TokenPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("Token")).getPrimaryKey().getFieldPaths());
    }

    public TokenPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public TokenPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "Token", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public Token findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getToken(ordinal);
    }

}