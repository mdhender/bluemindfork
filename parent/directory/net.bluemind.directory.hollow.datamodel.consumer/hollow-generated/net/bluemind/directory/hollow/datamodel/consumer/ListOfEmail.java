package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.HollowList;
import com.netflix.hollow.core.schema.HollowListSchema;
import com.netflix.hollow.api.objects.delegate.HollowListDelegate;
import com.netflix.hollow.api.objects.generic.GenericHollowRecordHelper;

@SuppressWarnings("all")
public class ListOfEmail extends HollowList<Email> {

    public ListOfEmail(HollowListDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    @Override
    public Email instantiateElement(int ordinal) {
        return (Email) api().getEmail(ordinal);
    }

    @Override
    public boolean equalsElement(int elementOrdinal, Object testObject) {
        return GenericHollowRecordHelper.equalObject(getSchema().getElementType(), elementOrdinal, testObject);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public ListOfEmailTypeAPI typeApi() {
        return (ListOfEmailTypeAPI) delegate.getTypeAPI();
    }

}