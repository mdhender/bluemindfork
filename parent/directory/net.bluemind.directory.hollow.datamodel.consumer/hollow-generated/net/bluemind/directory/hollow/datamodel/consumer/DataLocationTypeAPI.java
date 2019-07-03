package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class DataLocationTypeAPI extends HollowObjectTypeAPI {

    private final DataLocationDelegateLookupImpl delegateLookupImpl;

    public DataLocationTypeAPI(OfflineDirectoryAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "fqdn",
            "server"
        });
        this.delegateLookupImpl = new DataLocationDelegateLookupImpl(this);
    }

    public int getFqdnOrdinal(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleReferencedOrdinal("DataLocation", ordinal, "fqdn");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[0]);
    }

    public StringTypeAPI getFqdnTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getServerOrdinal(int ordinal) {
        if(fieldIndex[1] == -1)
            return missingDataHandler().handleReferencedOrdinal("DataLocation", ordinal, "server");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[1]);
    }

    public StringTypeAPI getServerTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public DataLocationDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI) api;
    }

}