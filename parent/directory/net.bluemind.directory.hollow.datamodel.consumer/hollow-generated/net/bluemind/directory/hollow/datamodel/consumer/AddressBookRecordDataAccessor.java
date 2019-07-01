package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.data.AbstractHollowDataAccessor;
import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

@SuppressWarnings("all")
public class AddressBookRecordDataAccessor extends AbstractHollowDataAccessor<AddressBookRecord> {

    public static final String TYPE = "AddressBookRecord";
    private OfflineDirectoryAPI api;

    public AddressBookRecordDataAccessor(HollowConsumer consumer) {
        super(consumer, TYPE);
        this.api = (OfflineDirectoryAPI)consumer.getAPI();
    }

    public AddressBookRecordDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api) {
        super(rStateEngine, TYPE);
        this.api = api;
    }

    public AddressBookRecordDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, String ... fieldPaths) {
        super(rStateEngine, TYPE, fieldPaths);
        this.api = api;
    }

    public AddressBookRecordDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, PrimaryKey primaryKey) {
        super(rStateEngine, TYPE, primaryKey);
        this.api = api;
    }

    @Override public AddressBookRecord getRecord(int ordinal){
        return api.getAddressBookRecord(ordinal);
    }

}