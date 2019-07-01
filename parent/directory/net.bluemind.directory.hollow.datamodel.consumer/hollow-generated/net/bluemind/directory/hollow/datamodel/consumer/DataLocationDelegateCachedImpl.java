package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.custom.HollowTypeAPI;
import com.netflix.hollow.api.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class DataLocationDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, DataLocationDelegate {

    private final int fqdnOrdinal;
    private final int serverOrdinal;
    private DataLocationTypeAPI typeAPI;

    public DataLocationDelegateCachedImpl(DataLocationTypeAPI typeAPI, int ordinal) {
        this.fqdnOrdinal = typeAPI.getFqdnOrdinal(ordinal);
        this.serverOrdinal = typeAPI.getServerOrdinal(ordinal);
        this.typeAPI = typeAPI;
    }

    public int getFqdnOrdinal(int ordinal) {
        return fqdnOrdinal;
    }

    public int getServerOrdinal(int ordinal) {
        return serverOrdinal;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public DataLocationTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (DataLocationTypeAPI) typeAPI;
    }

}