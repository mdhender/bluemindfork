package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class OfflineAddressBookDelegateLookupImpl extends HollowObjectAbstractDelegate implements OfflineAddressBookDelegate {

    private final OfflineAddressBookTypeAPI typeAPI;

    public OfflineAddressBookDelegateLookupImpl(OfflineAddressBookTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public int getDomainNameOrdinal(int ordinal) {
        return typeAPI.getDomainNameOrdinal(ordinal);
    }

    public int getDomainAliasesOrdinal(int ordinal) {
        return typeAPI.getDomainAliasesOrdinal(ordinal);
    }

    public int getNameOrdinal(int ordinal) {
        return typeAPI.getNameOrdinal(ordinal);
    }

    public int getDistinguishedNameOrdinal(int ordinal) {
        return typeAPI.getDistinguishedNameOrdinal(ordinal);
    }

    public int getSequence(int ordinal) {
        return typeAPI.getSequence(ordinal);
    }

    public Integer getSequenceBoxed(int ordinal) {
        return typeAPI.getSequenceBoxed(ordinal);
    }

    public int getContainerGuidOrdinal(int ordinal) {
        return typeAPI.getContainerGuidOrdinal(ordinal);
    }

    public int getHierarchicalRootDepartmentOrdinal(int ordinal) {
        return typeAPI.getHierarchicalRootDepartmentOrdinal(ordinal);
    }

    public OfflineAddressBookTypeAPI getTypeAPI() {
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