package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowListTypeAPI;

import com.netflix.hollow.core.read.dataaccess.HollowListTypeDataAccess;
import com.netflix.hollow.api.objects.delegate.HollowListLookupDelegate;

@SuppressWarnings("all")
public class ListOfStringTypeAPI extends HollowListTypeAPI {

    private final HollowListLookupDelegate delegateLookupImpl;

    public ListOfStringTypeAPI(OfflineDirectoryAPI api, HollowListTypeDataAccess dataAccess) {
        super(api, dataAccess);
        this.delegateLookupImpl = new HollowListLookupDelegate(this);
    }

    public StringTypeAPI getElementAPI() {
        return getAPI().getStringTypeAPI();
    }

    public HollowListLookupDelegate getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI)api;
    }

}