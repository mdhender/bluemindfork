package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class EmailTypeAPI extends HollowObjectTypeAPI {

    private final EmailDelegateLookupImpl delegateLookupImpl;

    public EmailTypeAPI(OfflineDirectoryAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "address",
            "ngrams",
            "allAliases",
            "isDefault"
        });
        this.delegateLookupImpl = new EmailDelegateLookupImpl(this);
    }

    public String getAddress(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleString("Email", ordinal, "address");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[0]);
        return getTypeDataAccess().readString(ordinal, fieldIndex[0]);
    }

    public boolean isAddressEqual(int ordinal, String testValue) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleStringEquals("Email", ordinal, "address", testValue);
        return getTypeDataAccess().isStringFieldEqual(ordinal, fieldIndex[0], testValue);
    }

    public int getNgramsOrdinal(int ordinal) {
        if(fieldIndex[1] == -1)
            return missingDataHandler().handleReferencedOrdinal("Email", ordinal, "ngrams");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[1]);
    }

    public ListOfStringTypeAPI getNgramsTypeAPI() {
        return getAPI().getListOfStringTypeAPI();
    }

    public boolean getAllAliases(int ordinal) {
        if(fieldIndex[2] == -1)
            return Boolean.TRUE.equals(missingDataHandler().handleBoolean("Email", ordinal, "allAliases"));
        return Boolean.TRUE.equals(getTypeDataAccess().readBoolean(ordinal, fieldIndex[2]));
    }

    public Boolean getAllAliasesBoxed(int ordinal) {
        if(fieldIndex[2] == -1)
            return missingDataHandler().handleBoolean("Email", ordinal, "allAliases");
        return getTypeDataAccess().readBoolean(ordinal, fieldIndex[2]);
    }



    public boolean getIsDefault(int ordinal) {
        if(fieldIndex[3] == -1)
            return Boolean.TRUE.equals(missingDataHandler().handleBoolean("Email", ordinal, "isDefault"));
        return Boolean.TRUE.equals(getTypeDataAccess().readBoolean(ordinal, fieldIndex[3]));
    }

    public Boolean getIsDefaultBoxed(int ordinal) {
        if(fieldIndex[3] == -1)
            return missingDataHandler().handleBoolean("Email", ordinal, "isDefault");
        return getTypeDataAccess().readBoolean(ordinal, fieldIndex[3]);
    }



    public EmailDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI) api;
    }

}