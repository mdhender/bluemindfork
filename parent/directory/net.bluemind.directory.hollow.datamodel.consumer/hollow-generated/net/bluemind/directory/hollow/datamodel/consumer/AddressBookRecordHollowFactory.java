package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.provider.HollowFactory;
import com.netflix.hollow.core.read.dataaccess.HollowTypeDataAccess;
import com.netflix.hollow.api.custom.HollowTypeAPI;

@SuppressWarnings("all")
public class AddressBookRecordHollowFactory<T extends AddressBookRecord> extends HollowFactory<T> {

    @Override
    public T newHollowObject(HollowTypeDataAccess dataAccess, HollowTypeAPI typeAPI, int ordinal) {
        return (T)new AddressBookRecord(((AddressBookRecordTypeAPI)typeAPI).getDelegateLookupImpl(), ordinal);
    }

    @Override
    public T newCachedHollowObject(HollowTypeDataAccess dataAccess, HollowTypeAPI typeAPI, int ordinal) {
        return (T)new AddressBookRecord(new AddressBookRecordDelegateCachedImpl((AddressBookRecordTypeAPI)typeAPI, ordinal), ordinal);
    }

}