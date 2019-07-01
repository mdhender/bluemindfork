package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.data.AbstractHollowDataAccessor;
import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

@SuppressWarnings("all")
public class DataLocationDataAccessor extends AbstractHollowDataAccessor<DataLocation> {

    public static final String TYPE = "DataLocation";
    private OfflineDirectoryAPI api;

    public DataLocationDataAccessor(HollowConsumer consumer) {
        super(consumer, TYPE);
        this.api = (OfflineDirectoryAPI)consumer.getAPI();
    }

    public DataLocationDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api) {
        super(rStateEngine, TYPE);
        this.api = api;
    }

    public DataLocationDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, String ... fieldPaths) {
        super(rStateEngine, TYPE, fieldPaths);
        this.api = api;
    }

    public DataLocationDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, PrimaryKey primaryKey) {
        super(rStateEngine, TYPE, primaryKey);
        this.api = api;
    }

    @Override public DataLocation getRecord(int ordinal){
        return api.getDataLocation(ordinal);
    }

}