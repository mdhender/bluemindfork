package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class AnrToken extends HollowObject {

    public AnrToken(AnrTokenDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public String getToken() {
        return delegate().getToken(ordinal);
    }

    public boolean isTokenEqual(String testValue) {
        return delegate().isTokenEqual(ordinal, testValue);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public AnrTokenTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected AnrTokenDelegate delegate() {
        return (AnrTokenDelegate)delegate;
    }

}