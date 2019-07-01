package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class StringPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<OfflineDirectoryAPI, HString> implements HollowUniqueKeyIndex<HString> {

    public StringPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public StringPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("String")).getPrimaryKey().getFieldPaths());
    }

    public StringPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public StringPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "String", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public HString findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getHString(ordinal);
    }

}