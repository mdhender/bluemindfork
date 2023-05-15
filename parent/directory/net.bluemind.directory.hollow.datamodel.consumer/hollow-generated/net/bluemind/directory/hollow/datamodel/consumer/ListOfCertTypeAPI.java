package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowListTypeAPI;

import com.netflix.hollow.core.read.dataaccess.HollowListTypeDataAccess;
import com.netflix.hollow.api.objects.delegate.HollowListLookupDelegate;

@SuppressWarnings("all")
public class ListOfCertTypeAPI extends HollowListTypeAPI {

    private final HollowListLookupDelegate delegateLookupImpl;

    public ListOfCertTypeAPI(OfflineDirectoryAPI api, HollowListTypeDataAccess dataAccess) {
        super(api, dataAccess);
        this.delegateLookupImpl = new HollowListLookupDelegate(this);
    }

    public CertTypeAPI getElementAPI() {
        return getAPI().getCertTypeAPI();
    }

    public HollowListLookupDelegate getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI)api;
    }

}