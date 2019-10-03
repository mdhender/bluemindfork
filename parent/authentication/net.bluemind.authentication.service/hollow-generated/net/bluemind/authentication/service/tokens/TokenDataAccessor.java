package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.data.AbstractHollowDataAccessor;
import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

@SuppressWarnings("all")
public class TokenDataAccessor extends AbstractHollowDataAccessor<Token> {

    public static final String TYPE = "Token";
    private TokensAPI api;

    public TokenDataAccessor(HollowConsumer consumer) {
        super(consumer, TYPE);
        this.api = (TokensAPI)consumer.getAPI();
    }

    public TokenDataAccessor(HollowReadStateEngine rStateEngine, TokensAPI api) {
        super(rStateEngine, TYPE);
        this.api = api;
    }

    public TokenDataAccessor(HollowReadStateEngine rStateEngine, TokensAPI api, String ... fieldPaths) {
        super(rStateEngine, TYPE, fieldPaths);
        this.api = api;
    }

    public TokenDataAccessor(HollowReadStateEngine rStateEngine, TokensAPI api, PrimaryKey primaryKey) {
        super(rStateEngine, TYPE, primaryKey);
        this.api = api;
    }

    @Override public Token getRecord(int ordinal){
        return api.getToken(ordinal);
    }

}