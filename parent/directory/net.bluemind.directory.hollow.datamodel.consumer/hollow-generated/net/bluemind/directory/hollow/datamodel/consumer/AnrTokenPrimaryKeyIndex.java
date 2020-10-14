package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

/**
 * @deprecated see {@link com.netflix.hollow.api.consumer.index.UniqueKeyIndex} which can be built as follows:
 * <pre>{@code
 *     UniqueKeyIndex<AnrToken, K> uki = UniqueKeyIndex.from(consumer, AnrToken.class)
 *         .usingBean(k);
 *     AnrToken m = uki.findMatch(k);
 * }</pre>
 * where {@code K} is a class declaring key field paths members, annotated with
 * {@link com.netflix.hollow.api.consumer.index.FieldPath}, and {@code k} is an instance of
 * {@code K} that is the key to find the unique {@code AnrToken} object.
 */
@Deprecated
@SuppressWarnings("all")
public class AnrTokenPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<OfflineDirectoryAPI, AnrToken> implements HollowUniqueKeyIndex<AnrToken> {

    public AnrTokenPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public AnrTokenPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("AnrToken")).getPrimaryKey().getFieldPaths());
    }

    public AnrTokenPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public AnrTokenPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "AnrToken", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public AnrToken findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getAnrToken(ordinal);
    }

}