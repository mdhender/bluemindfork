package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class CertTypeAPI extends HollowObjectTypeAPI {

    private final CertDelegateLookupImpl delegateLookupImpl;

    public CertTypeAPI(OfflineDirectoryAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "value"
        });
        this.delegateLookupImpl = new CertDelegateLookupImpl(this);
    }

    public byte[] getValue(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleBytes("Cert", ordinal, "value");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[0]);
        return getTypeDataAccess().readBytes(ordinal, fieldIndex[0]);
    }



    public CertDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI) api;
    }

}