package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface AnrTokenDelegate extends HollowObjectDelegate {

    public String getToken(int ordinal);

    public boolean isTokenEqual(int ordinal, String testValue);

    public AnrTokenTypeAPI getTypeAPI();

}