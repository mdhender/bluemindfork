package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.HollowList;
import com.netflix.hollow.core.schema.HollowListSchema;
import com.netflix.hollow.api.objects.delegate.HollowListDelegate;
import com.netflix.hollow.api.objects.generic.GenericHollowRecordHelper;

@SuppressWarnings("all")
public class ListOfAnrToken extends HollowList<AnrToken> {

    public ListOfAnrToken(HollowListDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    @Override
    public AnrToken instantiateElement(int ordinal) {
        return (AnrToken) api().getAnrToken(ordinal);
    }

    @Override
    public boolean equalsElement(int elementOrdinal, Object testObject) {
        return GenericHollowRecordHelper.equalObject(getSchema().getElementType(), elementOrdinal, testObject);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public ListOfAnrTokenTypeAPI typeApi() {
        return (ListOfAnrTokenTypeAPI) delegate.getTypeAPI();
    }

}