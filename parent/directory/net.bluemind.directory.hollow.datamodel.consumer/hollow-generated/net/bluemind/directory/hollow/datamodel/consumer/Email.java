package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class Email extends HollowObject {

    public Email(EmailDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public String getAddress() {
        return delegate().getAddress(ordinal);
    }

    public boolean isAddressEqual(String testValue) {
        return delegate().isAddressEqual(ordinal, testValue);
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