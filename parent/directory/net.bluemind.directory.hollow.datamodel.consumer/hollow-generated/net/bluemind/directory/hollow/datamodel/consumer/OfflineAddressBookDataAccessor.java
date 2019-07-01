package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.data.AbstractHollowDataAccessor;
import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

@SuppressWarnings("all")
public class OfflineAddressBookDataAccessor extends AbstractHollowDataAccessor<OfflineAddressBook> {

    public static final String TYPE = "OfflineAddressBook";
    private OfflineDirectoryAPI api;

    public OfflineAddressBookDataAccessor(HollowConsumer consumer) {
        super(consumer, TYPE);
        this.api = (OfflineDirectoryAPI)consumer.getAPI();
    }

    public OfflineAddressBookDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api) {
        super(rStateEngine, TYPE);
        this.api = api;
    }

    public OfflineAddressBookDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, String ... fieldPaths) {
        super(rStateEngine, TYPE, fieldPaths);
        this.api = api;
    }

    public OfflineAddressBookDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, PrimaryKey primaryKey) {
        super(rStateEngine, TYPE, primaryKey);
        this.api = api;
    }

    @Override public OfflineAddressBook getRecord(int ordinal){
        return api.getOfflineAddressBook(ordinal);
    }

}