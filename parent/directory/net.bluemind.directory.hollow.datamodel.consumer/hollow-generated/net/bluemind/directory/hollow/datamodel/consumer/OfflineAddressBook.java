package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class OfflineAddressBook extends HollowObject {

    public OfflineAddressBook(OfflineAddressBookDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public HString getDomainName() {
        int refOrdinal = delegate().getDomainNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public SetOfString getDomainAliases() {
        int refOrdinal = delegate().getDomainAliasesOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getSetOfString(refOrdinal);
    }

    public HString getName() {
        int refOrdinal = delegate().getNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getDistinguishedName() {
        int refOrdinal = delegate().getDistinguishedNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public int getSequence() {
        return delegate().getSequence(ordinal);
    }

    public Integer getSequenceBoxed() {
        return delegate().getSequenceBoxed(ordinal);
    }

    public HString getContainerGuid() {
        int refOrdinal = delegate().getContainerGuidOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getHierarchicalRootDepartment() {
        int refOrdinal = delegate().getHierarchicalRootDepartmentOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public OfflineAddressBookTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected OfflineAddressBookDelegate delegate() {
        return (OfflineAddressBookDelegate)delegate;
    }

    /**
     * Creates a unique key index for {@code OfflineAddressBook} that has a primary key.
     * The primary key is represented by the class {@link String}.
     * <p>
     * By default the unique key index will not track updates to the {@code consumer} and thus
     * any changes will not be reflected in matched results.  To track updates the index must be
     * {@link HollowConsumer#addRefreshListener(HollowConsumer.RefreshListener) registered}
     * with the {@code consumer}
     *
     * @param consumer the consumer
     * @return the unique key index
     */
    public static UniqueKeyIndex<OfflineAddressBook, String> uniqueIndex(HollowConsumer consumer) {
        return UniqueKeyIndex.from(consumer, OfflineAddressBook.class)
            .bindToPrimaryKey()
            .usingPath("domainName", String.class);
    }

}