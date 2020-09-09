package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class AnrTokenDelegateLookupImpl extends HollowObjectAbstractDelegate implements AnrTokenDelegate {

    private final AnrTokenTypeAPI typeAPI;

    public AnrTokenDelegateLookupImpl(AnrTokenTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public String getToken(int ordinal) {
        return typeAPI.getToken(ordinal);
    }

    public boolean isTokenEqual(int ordinal, String testValue) {
        return typeAPI.isTokenEqual(ordinal, testValue);
    }

    public AnrTokenTypeAPI getTypeAPI() {
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