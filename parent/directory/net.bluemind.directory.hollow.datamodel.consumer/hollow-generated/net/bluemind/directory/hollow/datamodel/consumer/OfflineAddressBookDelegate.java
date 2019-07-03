package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface OfflineAddressBookDelegate extends HollowObjectDelegate {

    public int getDomainNameOrdinal(int ordinal);

    public int getDomainAliasesOrdinal(int ordinal);

    public int getNameOrdinal(int ordinal);

    public int getDistinguishedNameOrdinal(int ordinal);

    public int getSequence(int ordinal);

    public Integer getSequenceBoxed(int ordinal);

    public int getContainerGuidOrdinal(int ordinal);

    public int getHierarchicalRootDepartmentOrdinal(int ordinal);

    public OfflineAddressBookTypeAPI getTypeAPI();

}