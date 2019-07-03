package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.client.HollowAPIFactory;
import com.netflix.hollow.api.custom.HollowAPI;
import com.netflix.hollow.api.objects.provider.HollowFactory;
import com.netflix.hollow.core.read.dataaccess.HollowDataAccess;
import java.util.Collections;
import java.util.Set;

@SuppressWarnings("all")
public class OfflineDirectoryAPIFactory implements HollowAPIFactory {

    private final Set<String> cachedTypes;

    public OfflineDirectoryAPIFactory() {
        this(Collections.<String>emptySet());
    }

    public OfflineDirectoryAPIFactory(Set<String> cachedTypes) {
        this.cachedTypes = cachedTypes;
    }

    @Override
    public HollowAPI createAPI(HollowDataAccess dataAccess) {
        return new OfflineDirectoryAPI(dataAccess, cachedTypes);
    }

    @Override
    public HollowAPI createAPI(HollowDataAccess dataAccess, HollowAPI previousCycleAPI) {
        if (!(previousCycleAPI instanceof OfflineDirectoryAPI)) {
            throw new ClassCastException(previousCycleAPI.getClass() + " not instance of OfflineDirectoryAPI");        }
        return new OfflineDirectoryAPI(dataAccess, cachedTypes, Collections.<String, HollowFactory<?>>emptyMap(), (OfflineDirectoryAPI) previousCycleAPI);
    }

}