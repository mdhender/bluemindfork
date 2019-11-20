package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.client.HollowAPIFactory;
import com.netflix.hollow.api.custom.HollowAPI;
import com.netflix.hollow.api.objects.provider.HollowFactory;
import com.netflix.hollow.core.read.dataaccess.HollowDataAccess;
import java.util.Collections;
import java.util.Set;

@SuppressWarnings("all")
public class TokensAPIFactory implements HollowAPIFactory {

    private final Set<String> cachedTypes;

    public TokensAPIFactory() {
        this(Collections.<String>emptySet());
    }

    public TokensAPIFactory(Set<String> cachedTypes) {
        this.cachedTypes = cachedTypes;
    }

    @Override
    public HollowAPI createAPI(HollowDataAccess dataAccess) {
        return new TokensAPI(dataAccess, cachedTypes);
    }

    @Override
    public HollowAPI createAPI(HollowDataAccess dataAccess, HollowAPI previousCycleAPI) {
        if (!(previousCycleAPI instanceof TokensAPI)) {
            throw new ClassCastException(previousCycleAPI.getClass() + " not instance of TokensAPI");        }
        return new TokensAPI(dataAccess, cachedTypes, Collections.<String, HollowFactory<?>>emptyMap(), (TokensAPI) previousCycleAPI);
    }

}