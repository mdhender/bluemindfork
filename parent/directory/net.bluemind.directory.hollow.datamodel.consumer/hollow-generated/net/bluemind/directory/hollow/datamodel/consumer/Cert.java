package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class Cert extends HollowObject {

    public Cert(CertDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public byte[] getValue() {
        return delegate().getValue(ordinal);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public CertTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected CertDelegate delegate() {
        return (CertDelegate)delegate;
    }

}