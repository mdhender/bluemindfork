package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface CertDelegate extends HollowObjectDelegate {

    public byte[] getValue(int ordinal);

    public CertTypeAPI getTypeAPI();

}