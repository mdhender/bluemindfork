package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class OfflineAddressBookPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<OfflineDirectoryAPI, OfflineAddressBook> implements HollowUniqueKeyIndex<OfflineAddressBook> {

    public OfflineAddressBookPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public OfflineAddressBookPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("OfflineAddressBook")).getPrimaryKey().getFieldPaths());
    }

    public OfflineAddressBookPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public OfflineAddressBookPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "OfflineAddressBook", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public OfflineAddressBook findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getOfflineAddressBook(ordinal);
    }

}