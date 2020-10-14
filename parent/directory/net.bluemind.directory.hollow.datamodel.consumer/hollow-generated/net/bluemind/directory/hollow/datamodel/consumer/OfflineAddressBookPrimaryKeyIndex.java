package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

/**
 * @deprecated see {@link com.netflix.hollow.api.consumer.index.UniqueKeyIndex} which can be built as follows:
 * <pre>{@code
 *     UniqueKeyIndex<OfflineAddressBook, K> uki = UniqueKeyIndex.from(consumer, OfflineAddressBook.class)
 *         .usingBean(k);
 *     OfflineAddressBook m = uki.findMatch(k);
 * }</pre>
 * where {@code K} is a class declaring key field paths members, annotated with
 * {@link com.netflix.hollow.api.consumer.index.FieldPath}, and {@code k} is an instance of
 * {@code K} that is the key to find the unique {@code OfflineAddressBook} object.
 */
@Deprecated
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