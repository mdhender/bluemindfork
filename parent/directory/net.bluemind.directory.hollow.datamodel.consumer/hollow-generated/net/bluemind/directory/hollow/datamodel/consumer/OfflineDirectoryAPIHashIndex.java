package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.core.index.HollowHashIndexResult;
import java.util.Collections;
import java.lang.Iterable;
import com.netflix.hollow.api.consumer.index.AbstractHollowHashIndex;
import com.netflix.hollow.api.consumer.data.AbstractHollowOrdinalIterable;


/**
 * @deprecated see {@link com.netflix.hollow.api.consumer.index.HashIndex} which can be built as follows:
 * <pre>{@code
 *     HashIndex<Date, K> uki = HashIndex.from(consumer, Date.class)
 *         .usingBean(k);
 *     Stream<Date> results = uki.findMatches(k);
 * }</pre>
 * where {@code K} is a class declaring key field paths members, annotated with
 * {@link com.netflix.hollow.api.consumer.index.FieldPath}, and {@code k} is an instance of
 * {@code K} that is the query to find the matching {@code Date} objects.
 */
@Deprecated
@SuppressWarnings("all")
public class OfflineDirectoryAPIHashIndex extends AbstractHollowHashIndex<OfflineDirectoryAPI> {

    public OfflineDirectoryAPIHashIndex(HollowConsumer consumer, String queryType, String selectFieldPath, String... matchFieldPaths) {
        super(consumer, true, queryType, selectFieldPath, matchFieldPaths);
    }

    public OfflineDirectoryAPIHashIndex(HollowConsumer consumer, boolean isListenToDataRefresh, String queryType, String selectFieldPath, String... matchFieldPaths) {
        super(consumer, isListenToDataRefresh, queryType, selectFieldPath, matchFieldPaths);
    }

    public Iterable<Date> findDateMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<Date>(matches.iterator()) {
            public Date getData(int ordinal) {
                return api.getDate(ordinal);
            }
        };
    }

    public Iterable<HString> findStringMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<HString>(matches.iterator()) {
            public HString getData(int ordinal) {
                return api.getHString(ordinal);
            }
        };
    }

    public Iterable<DataLocation> findDataLocationMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<DataLocation>(matches.iterator()) {
            public DataLocation getData(int ordinal) {
                return api.getDataLocation(ordinal);
            }
        };
    }

    public Iterable<Email> findEmailMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<Email>(matches.iterator()) {
            public Email getData(int ordinal) {
                return api.getEmail(ordinal);
            }
        };
    }

    public Iterable<ListOfEmail> findListOfEmailMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<ListOfEmail>(matches.iterator()) {
            public ListOfEmail getData(int ordinal) {
                return api.getListOfEmail(ordinal);
            }
        };
    }

    public Iterable<SetOfString> findSetOfStringMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<SetOfString>(matches.iterator()) {
            public SetOfString getData(int ordinal) {
                return api.getSetOfString(ordinal);
            }
        };
    }

    public Iterable<OfflineAddressBook> findOfflineAddressBookMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<OfflineAddressBook>(matches.iterator()) {
            public OfflineAddressBook getData(int ordinal) {
                return api.getOfflineAddressBook(ordinal);
            }
        };
    }

    public Iterable<AddressBookRecord> findAddressBookRecordMatches(Object... keys) {
        HollowHashIndexResult matches = idx.findMatches(keys);
        if(matches == null) return Collections.emptySet();

        return new AbstractHollowOrdinalIterable<AddressBookRecord>(matches.iterator()) {
            public AddressBookRecord getData(int ordinal) {
                return api.getAddressBookRecord(ordinal);
            }
        };
    }

}