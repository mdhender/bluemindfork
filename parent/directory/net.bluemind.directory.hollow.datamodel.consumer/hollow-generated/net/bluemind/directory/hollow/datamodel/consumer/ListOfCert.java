package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.HollowList;
import com.netflix.hollow.core.schema.HollowListSchema;
import com.netflix.hollow.api.objects.delegate.HollowListDelegate;
import com.netflix.hollow.api.objects.generic.GenericHollowRecordHelper;

@SuppressWarnings("all")
public class ListOfCert extends HollowList<Cert> {

    public ListOfCert(HollowListDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    @Override
    public Cert instantiateElement(int ordinal) {
        return (Cert) api().getCert(ordinal);
    }

    @Override
    public boolean equalsElement(int elementOrdinal, Object testObject) {
        return GenericHollowRecordHelper.equalObject(getSchema().getElementType(), elementOrdinal, testObject);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public ListOfCertTypeAPI typeApi() {
        return (ListOfCertTypeAPI) delegate.getTypeAPI();
    }

}