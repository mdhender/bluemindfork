package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

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