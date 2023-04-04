package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class CertDelegateLookupImpl extends HollowObjectAbstractDelegate implements CertDelegate {

    private final CertTypeAPI typeAPI;

    public CertDelegateLookupImpl(CertTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public byte[] getValue(int ordinal) {
        return typeAPI.getValue(ordinal);
    }

    public CertTypeAPI getTypeAPI() {
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