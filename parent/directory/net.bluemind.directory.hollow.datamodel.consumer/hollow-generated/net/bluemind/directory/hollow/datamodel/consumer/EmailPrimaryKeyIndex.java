package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class EmailPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<OfflineDirectoryAPI, Email> implements HollowUniqueKeyIndex<Email> {

    public EmailPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public EmailPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("Email")).getPrimaryKey().getFieldPaths());
    }

    public EmailPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public EmailPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "Email", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public Email findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getEmail(ordinal);
    }

}