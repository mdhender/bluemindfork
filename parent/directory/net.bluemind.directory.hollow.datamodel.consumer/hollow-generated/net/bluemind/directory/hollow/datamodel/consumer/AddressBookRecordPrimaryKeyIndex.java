package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class AddressBookRecordPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<OfflineDirectoryAPI, AddressBookRecord> implements HollowUniqueKeyIndex<AddressBookRecord> {

    public AddressBookRecordPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public AddressBookRecordPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("AddressBookRecord")).getPrimaryKey().getFieldPaths());
    }

    public AddressBookRecordPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public AddressBookRecordPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "AddressBookRecord", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public AddressBookRecord findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getAddressBookRecord(ordinal);
    }

}