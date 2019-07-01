package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.data.AbstractHollowDataAccessor;
import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

@SuppressWarnings("all")
public class DateDataAccessor extends AbstractHollowDataAccessor<Date> {

    public static final String TYPE = "Date";
    private OfflineDirectoryAPI api;

    public DateDataAccessor(HollowConsumer consumer) {
        super(consumer, TYPE);
        this.api = (OfflineDirectoryAPI)consumer.getAPI();
    }

    public DateDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api) {
        super(rStateEngine, TYPE);
        this.api = api;
    }

    public DateDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, String ... fieldPaths) {
        super(rStateEngine, TYPE, fieldPaths);
        this.api = api;
    }

    public DateDataAccessor(HollowReadStateEngine rStateEngine, OfflineDirectoryAPI api, PrimaryKey primaryKey) {
        super(rStateEngine, TYPE, primaryKey);
        this.api = api;
    }

    @Override public Date getRecord(int ordinal){
        return api.getDate(ordinal);
    }

}