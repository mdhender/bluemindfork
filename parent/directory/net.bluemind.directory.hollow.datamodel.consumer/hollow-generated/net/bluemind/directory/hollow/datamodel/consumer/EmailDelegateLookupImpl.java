package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class EmailDelegateLookupImpl extends HollowObjectAbstractDelegate implements EmailDelegate {

    private final EmailTypeAPI typeAPI;

    public EmailDelegateLookupImpl(EmailTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public String getAddress(int ordinal) {
        return typeAPI.getAddress(ordinal);
    }

    public boolean isAddressEqual(int ordinal, String testValue) {
        return typeAPI.isAddressEqual(ordinal, testValue);
    }

    public int getNgramsOrdinal(int ordinal) {
        return typeAPI.getNgramsOrdinal(ordinal);
    }

    public boolean getAllAliases(int ordinal) {
        return typeAPI.getAllAliases(ordinal);
    }

    public Boolean getAllAliasesBoxed(int ordinal) {
        return typeAPI.getAllAliasesBoxed(ordinal);
    }

    public boolean getIsDefault(int ordinal) {
        return typeAPI.getIsDefault(ordinal);
    }

    public Boolean getIsDefaultBoxed(int ordinal) {
        return typeAPI.getIsDefaultBoxed(ordinal);
    }

    public EmailTypeAPI getTypeAPI() {
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