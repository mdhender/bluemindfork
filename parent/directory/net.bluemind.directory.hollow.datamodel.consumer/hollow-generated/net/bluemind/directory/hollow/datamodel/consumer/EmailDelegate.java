package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface EmailDelegate extends HollowObjectDelegate {

    public int getAddressOrdinal(int ordinal);

    public boolean getAllAliases(int ordinal);

    public Boolean getAllAliasesBoxed(int ordinal);

    public boolean getIsDefault(int ordinal);

    public Boolean getIsDefaultBoxed(int ordinal);

    public EmailTypeAPI getTypeAPI();

}