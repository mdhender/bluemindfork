package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.custom.HollowTypeAPI;
import com.netflix.hollow.api.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class EmailDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, EmailDelegate {

    private final String address;
    private final int ngramsOrdinal;
    private final Boolean allAliases;
    private final Boolean isDefault;
    private EmailTypeAPI typeAPI;

    public EmailDelegateCachedImpl(EmailTypeAPI typeAPI, int ordinal) {
        this.address = typeAPI.getAddress(ordinal);
        this.ngramsOrdinal = typeAPI.getNgramsOrdinal(ordinal);
        this.allAliases = typeAPI.getAllAliasesBoxed(ordinal);
        this.isDefault = typeAPI.getIsDefaultBoxed(ordinal);
        this.typeAPI = typeAPI;
    }

    public String getAddress(int ordinal) {
        return address;
    }

    public boolean isAddressEqual(int ordinal, String testValue) {
        if(testValue == null)
            return address == null;
        return testValue.equals(address);
    }

    public int getNgramsOrdinal(int ordinal) {
        return ngramsOrdinal;
    }

    public boolean getAllAliases(int ordinal) {
        if(allAliases == null)
            return false;
        return allAliases.booleanValue();
    }

    public Boolean getAllAliasesBoxed(int ordinal) {
        return allAliases;
    }

    public boolean getIsDefault(int ordinal) {
        if(isDefault == null)
            return false;
        return isDefault.booleanValue();
    }

    public Boolean getIsDefaultBoxed(int ordinal) {
        return isDefault;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public EmailTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (EmailTypeAPI) typeAPI;
    }

}