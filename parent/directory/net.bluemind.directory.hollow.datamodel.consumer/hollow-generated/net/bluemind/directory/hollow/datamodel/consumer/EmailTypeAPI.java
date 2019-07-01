package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class EmailTypeAPI extends HollowObjectTypeAPI {

    private final EmailDelegateLookupImpl delegateLookupImpl;

    public EmailTypeAPI(OfflineDirectoryAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "address",
            "allAliases",
            "isDefault"
        });
        this.delegateLookupImpl = new EmailDelegateLookupImpl(this);
    }

    public int getAddressOrdinal(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleReferencedOrdinal("Email", ordinal, "address");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[0]);
    }

    public StringTypeAPI getAddressTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public boolean getAllAliases(int ordinal) {
        if(fieldIndex[1] == -1)
            return Boolean.TRUE.equals(missingDataHandler().handleBoolean("Email", ordinal, "allAliases"));
        return Boolean.TRUE.equals(getTypeDataAccess().readBoolean(ordinal, fieldIndex[1]));
    }

    public Boolean getAllAliasesBoxed(int ordinal) {
        if(fieldIndex[1] == -1)
            return missingDataHandler().handleBoolean("Email", ordinal, "allAliases");
        return getTypeDataAccess().readBoolean(ordinal, fieldIndex[1]);
    }



    public boolean getIsDefault(int ordinal) {
        if(fieldIndex[2] == -1)
            return Boolean.TRUE.equals(missingDataHandler().handleBoolean("Email", ordinal, "isDefault"));
        return Boolean.TRUE.equals(getTypeDataAccess().readBoolean(ordinal, fieldIndex[2]));
    }

    public Boolean getIsDefaultBoxed(int ordinal) {
        if(fieldIndex[2] == -1)
            return missingDataHandler().handleBoolean("Email", ordinal, "isDefault");
        return getTypeDataAccess().readBoolean(ordinal, fieldIndex[2]);
    }



    public EmailDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI) api;
    }

}