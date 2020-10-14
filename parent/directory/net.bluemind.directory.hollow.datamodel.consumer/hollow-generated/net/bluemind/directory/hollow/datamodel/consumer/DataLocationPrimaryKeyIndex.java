package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

/**
 * @deprecated see {@link com.netflix.hollow.api.consumer.index.UniqueKeyIndex} which can be built as follows:
 * <pre>{@code
 *     UniqueKeyIndex<DataLocation, K> uki = UniqueKeyIndex.from(consumer, DataLocation.class)
 *         .usingBean(k);
 *     DataLocation m = uki.findMatch(k);
 * }</pre>
 * where {@code K} is a class declaring key field paths members, annotated with
 * {@link com.netflix.hollow.api.consumer.index.FieldPath}, and {@code k} is an instance of
 * {@code K} that is the key to find the unique {@code DataLocation} object.
 */
@Deprecated
@SuppressWarnings("all")
public class DataLocationPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<OfflineDirectoryAPI, DataLocation> implements HollowUniqueKeyIndex<DataLocation> {

    public DataLocationPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public DataLocationPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("DataLocation")).getPrimaryKey().getFieldPaths());
    }

    public DataLocationPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public DataLocationPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "DataLocation", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public DataLocation findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getDataLocation(ordinal);
    }

}