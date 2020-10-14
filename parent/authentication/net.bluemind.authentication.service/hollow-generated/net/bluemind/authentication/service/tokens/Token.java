package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class Token extends HollowObject {

    public Token(TokenDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public String getKey() {
        return delegate().getKey(ordinal);
    }

    public boolean isKeyEqual(String testValue) {
        return delegate().isKeyEqual(ordinal, testValue);
    }

    public HString getSubjectUid() {
        int refOrdinal = delegate().getSubjectUidOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getSubjectDomain() {
        int refOrdinal = delegate().getSubjectDomainOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public String getOrigin() {
        return delegate().getOrigin(ordinal);
    }

    public boolean isOriginEqual(String testValue) {
        return delegate().isOriginEqual(ordinal, testValue);
    }

    public long getExpiresTimestamp() {
        return delegate().getExpiresTimestamp(ordinal);
    }

    public Long getExpiresTimestampBoxed() {
        return delegate().getExpiresTimestampBoxed(ordinal);
    }

    public TokensAPI api() {
        return typeApi().getAPI();
    }

    public TokenTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected TokenDelegate delegate() {
        return (TokenDelegate)delegate;
    }

    /**
     * Creates a unique key index for {@code Token} that has a primary key.
     * The primary key is represented by the class {@link String}.
     * <p>
     * By default the unique key index will not track updates to the {@code consumer} and thus
     * any changes will not be reflected in matched results.  To track updates the index must be
     * {@link HollowConsumer#addRefreshListener(HollowConsumer.RefreshListener) registered}
     * with the {@code consumer}
     *
     * @param consumer the consumer
     * @return the unique key index
     */
    public static UniqueKeyIndex<Token, String> uniqueIndex(HollowConsumer consumer) {
        return UniqueKeyIndex.from(consumer, Token.class)
            .bindToPrimaryKey()
            .usingPath("key", String.class);
    }

}