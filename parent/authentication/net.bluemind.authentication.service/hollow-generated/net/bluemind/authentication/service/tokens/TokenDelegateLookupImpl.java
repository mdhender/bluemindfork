package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class TokenDelegateLookupImpl extends HollowObjectAbstractDelegate implements TokenDelegate {

    private final TokenTypeAPI typeAPI;

    public TokenDelegateLookupImpl(TokenTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public String getKey(int ordinal) {
        return typeAPI.getKey(ordinal);
    }

    public boolean isKeyEqual(int ordinal, String testValue) {
        return typeAPI.isKeyEqual(ordinal, testValue);
    }

    public int getSubjectUidOrdinal(int ordinal) {
        return typeAPI.getSubjectUidOrdinal(ordinal);
    }

    public int getSubjectDomainOrdinal(int ordinal) {
        return typeAPI.getSubjectDomainOrdinal(ordinal);
    }

    public long getExpiresTimestamp(int ordinal) {
        return typeAPI.getExpiresTimestamp(ordinal);
    }

    public Long getExpiresTimestampBoxed(int ordinal) {
        return typeAPI.getExpiresTimestampBoxed(ordinal);
    }

    public TokenTypeAPI getTypeAPI() {
        return typeAPI;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

}