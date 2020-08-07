package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class Email extends HollowObject {

    public Email(EmailDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public HString getAddress() {
        int refOrdinal = delegate().getAddressOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public boolean getAllAliases() {
        return delegate().getAllAliases(ordinal);
    }

    public Boolean getAllAliasesBoxed() {
        return delegate().getAllAliasesBoxed(ordinal);
    }

    public boolean getIsDefault() {
        return delegate().getIsDefault(ordinal);
    }

    public Boolean getIsDefaultBoxed() {
        return delegate().getIsDefaultBoxed(ordinal);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public EmailTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected EmailDelegate delegate() {
        return (EmailDelegate)delegate;
    }

}