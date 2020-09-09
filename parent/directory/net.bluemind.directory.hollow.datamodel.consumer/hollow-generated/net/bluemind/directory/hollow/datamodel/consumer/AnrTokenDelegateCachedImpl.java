package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.custom.HollowTypeAPI;
import com.netflix.hollow.api.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class AnrTokenDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, AnrTokenDelegate {

    private final String token;
    private AnrTokenTypeAPI typeAPI;

    public AnrTokenDelegateCachedImpl(AnrTokenTypeAPI typeAPI, int ordinal) {
        this.token = typeAPI.getToken(ordinal);
        this.typeAPI = typeAPI;
    }

    public String getToken(int ordinal) {
        return token;
    }

    public boolean isTokenEqual(int ordinal, String testValue) {
        if(testValue == null)
            return token == null;
        return testValue.equals(token);
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public AnrTokenTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (AnrTokenTypeAPI) typeAPI;
    }

}