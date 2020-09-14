package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.custom.HollowTypeAPI;
import com.netflix.hollow.api.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class TokenDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, TokenDelegate {

    private final String key;
    private final int subjectUidOrdinal;
    private final int subjectDomainOrdinal;
    private final Long expiresTimestamp;
    private TokenTypeAPI typeAPI;

    public TokenDelegateCachedImpl(TokenTypeAPI typeAPI, int ordinal) {
        this.key = typeAPI.getKey(ordinal);
        this.subjectUidOrdinal = typeAPI.getSubjectUidOrdinal(ordinal);
        this.subjectDomainOrdinal = typeAPI.getSubjectDomainOrdinal(ordinal);
        this.expiresTimestamp = typeAPI.getExpiresTimestampBoxed(ordinal);
        this.typeAPI = typeAPI;
    }

    public String getKey(int ordinal) {
        return key;
    }

    public boolean isKeyEqual(int ordinal, String testValue) {
        if(testValue == null)
            return key == null;
        return testValue.equals(key);
    }

    public int getSubjectUidOrdinal(int ordinal) {
        return subjectUidOrdinal;
    }

    public int getSubjectDomainOrdinal(int ordinal) {
        return subjectDomainOrdinal;
    }

    public long getExpiresTimestamp(int ordinal) {
        if(expiresTimestamp == null)
            return Long.MIN_VALUE;
        return expiresTimestamp.longValue();
    }

    public Long getExpiresTimestampBoxed(int ordinal) {
        return expiresTimestamp;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public TokenTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (TokenTypeAPI) typeAPI;
    }

}