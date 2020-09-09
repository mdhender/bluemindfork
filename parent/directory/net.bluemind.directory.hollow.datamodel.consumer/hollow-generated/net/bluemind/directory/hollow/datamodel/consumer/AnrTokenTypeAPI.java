package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class AnrTokenTypeAPI extends HollowObjectTypeAPI {

    private final AnrTokenDelegateLookupImpl delegateLookupImpl;

    public AnrTokenTypeAPI(OfflineDirectoryAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "token"
        });
        this.delegateLookupImpl = new AnrTokenDelegateLookupImpl(this);
    }

    public String getToken(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleString("AnrToken", ordinal, "token");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[0]);
        return getTypeDataAccess().readString(ordinal, fieldIndex[0]);
    }

    public boolean isTokenEqual(int ordinal, String testValue) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleStringEquals("AnrToken", ordinal, "token", testValue);
        return getTypeDataAccess().isStringFieldEqual(ordinal, fieldIndex[0], testValue);
    }

    public AnrTokenDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI) api;
    }

}