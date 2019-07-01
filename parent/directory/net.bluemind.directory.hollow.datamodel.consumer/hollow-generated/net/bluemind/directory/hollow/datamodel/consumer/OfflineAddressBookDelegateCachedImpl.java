package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.custom.HollowTypeAPI;
import com.netflix.hollow.api.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class OfflineAddressBookDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, OfflineAddressBookDelegate {

    private final int domainNameOrdinal;
    private final int domainAliasesOrdinal;
    private final int nameOrdinal;
    private final int distinguishedNameOrdinal;
    private final Integer sequence;
    private final int containerGuidOrdinal;
    private final int hierarchicalRootDepartmentOrdinal;
    private OfflineAddressBookTypeAPI typeAPI;

    public OfflineAddressBookDelegateCachedImpl(OfflineAddressBookTypeAPI typeAPI, int ordinal) {
        this.domainNameOrdinal = typeAPI.getDomainNameOrdinal(ordinal);
        this.domainAliasesOrdinal = typeAPI.getDomainAliasesOrdinal(ordinal);
        this.nameOrdinal = typeAPI.getNameOrdinal(ordinal);
        this.distinguishedNameOrdinal = typeAPI.getDistinguishedNameOrdinal(ordinal);
        this.sequence = typeAPI.getSequenceBoxed(ordinal);
        this.containerGuidOrdinal = typeAPI.getContainerGuidOrdinal(ordinal);
        this.hierarchicalRootDepartmentOrdinal = typeAPI.getHierarchicalRootDepartmentOrdinal(ordinal);
        this.typeAPI = typeAPI;
    }

    public int getDomainNameOrdinal(int ordinal) {
        return domainNameOrdinal;
    }

    public int getDomainAliasesOrdinal(int ordinal) {
        return domainAliasesOrdinal;
    }

    public int getNameOrdinal(int ordinal) {
        return nameOrdinal;
    }

    public int getDistinguishedNameOrdinal(int ordinal) {
        return distinguishedNameOrdinal;
    }

    public int getSequence(int ordinal) {
        if(sequence == null)
            return Integer.MIN_VALUE;
        return sequence.intValue();
    }

    public Integer getSequenceBoxed(int ordinal) {
        return sequence;
    }

    public int getContainerGuidOrdinal(int ordinal) {
        return containerGuidOrdinal;
    }

    public int getHierarchicalRootDepartmentOrdinal(int ordinal) {
        return hierarchicalRootDepartmentOrdinal;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public OfflineAddressBookTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (OfflineAddressBookTypeAPI) typeAPI;
    }

}