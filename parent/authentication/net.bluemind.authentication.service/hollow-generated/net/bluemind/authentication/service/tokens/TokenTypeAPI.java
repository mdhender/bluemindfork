package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class TokenTypeAPI extends HollowObjectTypeAPI {

    private final TokenDelegateLookupImpl delegateLookupImpl;

    public TokenTypeAPI(TokensAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "key",
            "subjectUid",
            "subjectDomain",
            "origin",
            "expiresTimestamp"
        });
        this.delegateLookupImpl = new TokenDelegateLookupImpl(this);
    }

    public String getKey(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleString("Token", ordinal, "key");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[0]);
        return getTypeDataAccess().readString(ordinal, fieldIndex[0]);
    }

    public boolean isKeyEqual(int ordinal, String testValue) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleStringEquals("Token", ordinal, "key", testValue);
        return getTypeDataAccess().isStringFieldEqual(ordinal, fieldIndex[0], testValue);
    }

    public int getSubjectUidOrdinal(int ordinal) {
        if(fieldIndex[1] == -1)
            return missingDataHandler().handleReferencedOrdinal("Token", ordinal, "subjectUid");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[1]);
    }

    public StringTypeAPI getSubjectUidTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getSubjectDomainOrdinal(int ordinal) {
        if(fieldIndex[2] == -1)
            return missingDataHandler().handleReferencedOrdinal("Token", ordinal, "subjectDomain");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[2]);
    }

    public StringTypeAPI getSubjectDomainTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public String getOrigin(int ordinal) {
        if(fieldIndex[3] == -1)
            return missingDataHandler().handleString("Token", ordinal, "origin");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[3]);
        return getTypeDataAccess().readString(ordinal, fieldIndex[3]);
    }

    public boolean isOriginEqual(int ordinal, String testValue) {
        if(fieldIndex[3] == -1)
            return missingDataHandler().handleStringEquals("Token", ordinal, "origin", testValue);
        return getTypeDataAccess().isStringFieldEqual(ordinal, fieldIndex[3], testValue);
    }

    public long getExpiresTimestamp(int ordinal) {
        if(fieldIndex[4] == -1)
            return missingDataHandler().handleLong("Token", ordinal, "expiresTimestamp");
        return getTypeDataAccess().readLong(ordinal, fieldIndex[4]);
    }

    public Long getExpiresTimestampBoxed(int ordinal) {
        long l;
        if(fieldIndex[4] == -1) {
            l = missingDataHandler().handleLong("Token", ordinal, "expiresTimestamp");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[4]);
            l = getTypeDataAccess().readLong(ordinal, fieldIndex[4]);
        }
        if(l == Long.MIN_VALUE)
            return null;
        return Long.valueOf(l);
    }



    public TokenDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public TokensAPI getAPI() {
        return (TokensAPI) api;
    }

}