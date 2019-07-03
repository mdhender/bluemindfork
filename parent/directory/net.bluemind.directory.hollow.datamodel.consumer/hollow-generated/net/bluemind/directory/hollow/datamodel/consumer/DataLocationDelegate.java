package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface DataLocationDelegate extends HollowObjectDelegate {

    public int getFqdnOrdinal(int ordinal);

    public int getServerOrdinal(int ordinal);

    public DataLocationTypeAPI getTypeAPI();

}