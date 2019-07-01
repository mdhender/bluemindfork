package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class DataLocationDelegateLookupImpl extends HollowObjectAbstractDelegate implements DataLocationDelegate {

    private final DataLocationTypeAPI typeAPI;

    public DataLocationDelegateLookupImpl(DataLocationTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public int getFqdnOrdinal(int ordinal) {
        return typeAPI.getFqdnOrdinal(ordinal);
    }

    public int getServerOrdinal(int ordinal) {
        return typeAPI.getServerOrdinal(ordinal);
    }

    public DataLocationTypeAPI getTypeAPI() {
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