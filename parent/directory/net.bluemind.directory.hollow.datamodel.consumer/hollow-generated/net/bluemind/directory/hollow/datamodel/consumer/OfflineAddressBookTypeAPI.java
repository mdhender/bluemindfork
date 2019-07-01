package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class OfflineAddressBookTypeAPI extends HollowObjectTypeAPI {

    private final OfflineAddressBookDelegateLookupImpl delegateLookupImpl;

    public OfflineAddressBookTypeAPI(OfflineDirectoryAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "domainName",
            "domainAliases",
            "name",
            "distinguishedName",
            "sequence",
            "containerGuid",
            "hierarchicalRootDepartment"
        });
        this.delegateLookupImpl = new OfflineAddressBookDelegateLookupImpl(this);
    }

    public int getDomainNameOrdinal(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleReferencedOrdinal("OfflineAddressBook", ordinal, "domainName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[0]);
    }

    public StringTypeAPI getDomainNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getDomainAliasesOrdinal(int ordinal) {
        if(fieldIndex[1] == -1)
            return missingDataHandler().handleReferencedOrdinal("OfflineAddressBook", ordinal, "domainAliases");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[1]);
    }

    public SetOfStringTypeAPI getDomainAliasesTypeAPI() {
        return getAPI().getSetOfStringTypeAPI();
    }

    public int getNameOrdinal(int ordinal) {
        if(fieldIndex[2] == -1)
            return missingDataHandler().handleReferencedOrdinal("OfflineAddressBook", ordinal, "name");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[2]);
    }

    public StringTypeAPI getNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getDistinguishedNameOrdinal(int ordinal) {
        if(fieldIndex[3] == -1)
            return missingDataHandler().handleReferencedOrdinal("OfflineAddressBook", ordinal, "distinguishedName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[3]);
    }

    public StringTypeAPI getDistinguishedNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getSequence(int ordinal) {
        if(fieldIndex[4] == -1)
            return missingDataHandler().handleInt("OfflineAddressBook", ordinal, "sequence");
        return getTypeDataAccess().readInt(ordinal, fieldIndex[4]);
    }

    public Integer getSequenceBoxed(int ordinal) {
        int i;
        if(fieldIndex[4] == -1) {
            i = missingDataHandler().handleInt("OfflineAddressBook", ordinal, "sequence");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[4]);
            i = getTypeDataAccess().readInt(ordinal, fieldIndex[4]);
        }
        if(i == Integer.MIN_VALUE)
            return null;
        return Integer.valueOf(i);
    }



    public int getContainerGuidOrdinal(int ordinal) {
        if(fieldIndex[5] == -1)
            return missingDataHandler().handleReferencedOrdinal("OfflineAddressBook", ordinal, "containerGuid");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[5]);
    }

    public StringTypeAPI getContainerGuidTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getHierarchicalRootDepartmentOrdinal(int ordinal) {
        if(fieldIndex[6] == -1)
            return missingDataHandler().handleReferencedOrdinal("OfflineAddressBook", ordinal, "hierarchicalRootDepartment");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[6]);
    }

    public StringTypeAPI getHierarchicalRootDepartmentTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public OfflineAddressBookDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI) api;
    }

}