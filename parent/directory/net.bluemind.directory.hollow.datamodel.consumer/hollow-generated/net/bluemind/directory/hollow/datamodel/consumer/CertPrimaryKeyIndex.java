package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.api.consumer.index.HollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

/**
 * @deprecated see {@link com.netflix.hollow.api.consumer.index.UniqueKeyIndex} which can be built as follows:
 * <pre>{@code
 *     UniqueKeyIndex<Cert, K> uki = UniqueKeyIndex.from(consumer, Cert.class)
 *         .usingBean(k);
 *     Cert m = uki.findMatch(k);
 * }</pre>
 * where {@code K} is a class declaring key field paths members, annotated with
 * {@link com.netflix.hollow.api.consumer.index.FieldPath}, and {@code k} is an instance of
 * {@code K} that is the key to find the unique {@code Cert} object.
 */
@Deprecated
@SuppressWarnings("all")
public class CertPrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<OfflineDirectoryAPI, Cert> implements HollowUniqueKeyIndex<Cert> {

    public CertPrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, true);
    }

    public CertPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh) {
        this(consumer, isListenToDataRefresh, ((HollowObjectSchema)consumer.getStateEngine().getNonNullSchema("Cert")).getPrimaryKey().getFieldPaths());
    }

    public CertPrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public CertPrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String... fieldPaths) {
        super(consumer, "Cert", isListenToDataRefresh, fieldPaths);
    }

    @Override
    public Cert findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getCert(ordinal);
    }

}