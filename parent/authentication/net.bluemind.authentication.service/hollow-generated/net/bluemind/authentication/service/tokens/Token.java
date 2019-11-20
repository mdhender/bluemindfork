package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

import com.netflix.hollow.tools.stringifier.HollowRecordStringifier;

@SuppressWarnings("all")
public class Token extends HollowObject {

    public Token(TokenDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public HString getKey() {
        int refOrdinal = delegate().getKeyOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
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

}