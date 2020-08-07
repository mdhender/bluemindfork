package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class DataLocation extends HollowObject {

    public DataLocation(DataLocationDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public HString getFqdn() {
        int refOrdinal = delegate().getFqdnOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getServer() {
        int refOrdinal = delegate().getServerOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public DataLocationTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected DataLocationDelegate delegate() {
        return (DataLocationDelegate)delegate;
    }

}