package net.bluemind.directory.hollow.datamodel.consumer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import com.netflix.hollow.api.consumer.HollowConsumerAPI;
import com.netflix.hollow.api.custom.HollowAPI;
import com.netflix.hollow.core.read.dataaccess.HollowDataAccess;
import com.netflix.hollow.core.read.dataaccess.HollowTypeDataAccess;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.read.dataaccess.HollowListTypeDataAccess;
import com.netflix.hollow.core.read.dataaccess.HollowSetTypeDataAccess;
import com.netflix.hollow.core.read.dataaccess.HollowMapTypeDataAccess;
import com.netflix.hollow.core.read.dataaccess.missing.HollowObjectMissingDataAccess;
import com.netflix.hollow.core.read.dataaccess.missing.HollowListMissingDataAccess;
import com.netflix.hollow.core.read.dataaccess.missing.HollowSetMissingDataAccess;
import com.netflix.hollow.core.read.dataaccess.missing.HollowMapMissingDataAccess;
import com.netflix.hollow.api.objects.provider.HollowFactory;
import com.netflix.hollow.api.objects.provider.HollowObjectProvider;
import com.netflix.hollow.api.objects.provider.HollowObjectCacheProvider;
import com.netflix.hollow.api.objects.provider.HollowObjectFactoryProvider;
import com.netflix.hollow.api.sampling.HollowObjectCreationSampler;
import com.netflix.hollow.api.sampling.HollowSamplingDirector;
import com.netflix.hollow.api.sampling.SampleResult;
import com.netflix.hollow.core.util.AllHollowRecordCollection;

@SuppressWarnings("all")
public class OfflineDirectoryAPI extends HollowAPI  {

    private final HollowObjectCreationSampler objectCreationSampler;

    private final DateTypeAPI dateTypeAPI;
    private final StringTypeAPI stringTypeAPI;
    private final DataLocationTypeAPI dataLocationTypeAPI;
    private final EmailTypeAPI emailTypeAPI;
    private final ListOfEmailTypeAPI listOfEmailTypeAPI;
    private final SetOfStringTypeAPI setOfStringTypeAPI;
    private final OfflineAddressBookTypeAPI offlineAddressBookTypeAPI;
    private final AddressBookRecordTypeAPI addressBookRecordTypeAPI;

    private final HollowObjectProvider dateProvider;
    private final HollowObjectProvider stringProvider;
    private final HollowObjectProvider dataLocationProvider;
    private final HollowObjectProvider emailProvider;
    private final HollowObjectProvider listOfEmailProvider;
    private final HollowObjectProvider setOfStringProvider;
    private final HollowObjectProvider offlineAddressBookProvider;
    private final HollowObjectProvider addressBookRecordProvider;

    public OfflineDirectoryAPI(HollowDataAccess dataAccess) {
        this(dataAccess, Collections.<String>emptySet());
    }

    public OfflineDirectoryAPI(HollowDataAccess dataAccess, Set<String> cachedTypes) {
        this(dataAccess, cachedTypes, Collections.<String, HollowFactory<?>>emptyMap());
    }

    public OfflineDirectoryAPI(HollowDataAccess dataAccess, Set<String> cachedTypes, Map<String, HollowFactory<?>> factoryOverrides) {
        this(dataAccess, cachedTypes, factoryOverrides, null);
    }

    public OfflineDirectoryAPI(HollowDataAccess dataAccess, Set<String> cachedTypes, Map<String, HollowFactory<?>> factoryOverrides, OfflineDirectoryAPI previousCycleAPI) {
        super(dataAccess);
        HollowTypeDataAccess typeDataAccess;
        HollowFactory factory;

        objectCreationSampler = new HollowObjectCreationSampler("Date","String","DataLocation","Email","ListOfEmail","SetOfString","OfflineAddressBook","AddressBookRecord");

        typeDataAccess = dataAccess.getTypeDataAccess("Date");
        if(typeDataAccess != null) {
            dateTypeAPI = new DateTypeAPI(this, (HollowObjectTypeDataAccess)typeDataAccess);
        } else {
            dateTypeAPI = new DateTypeAPI(this, new HollowObjectMissingDataAccess(dataAccess, "Date"));
        }
        addTypeAPI(dateTypeAPI);
        factory = factoryOverrides.get("Date");
        if(factory == null)
            factory = new DateHollowFactory();
        if(cachedTypes.contains("Date")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.dateProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.dateProvider;
            dateProvider = new HollowObjectCacheProvider(typeDataAccess, dateTypeAPI, factory, previousCacheProvider);
        } else {
            dateProvider = new HollowObjectFactoryProvider(typeDataAccess, dateTypeAPI, factory);
        }

        typeDataAccess = dataAccess.getTypeDataAccess("String");
        if(typeDataAccess != null) {
            stringTypeAPI = new StringTypeAPI(this, (HollowObjectTypeDataAccess)typeDataAccess);
        } else {
            stringTypeAPI = new StringTypeAPI(this, new HollowObjectMissingDataAccess(dataAccess, "String"));
        }
        addTypeAPI(stringTypeAPI);
        factory = factoryOverrides.get("String");
        if(factory == null)
            factory = new StringHollowFactory();
        if(cachedTypes.contains("String")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.stringProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.stringProvider;
            stringProvider = new HollowObjectCacheProvider(typeDataAccess, stringTypeAPI, factory, previousCacheProvider);
        } else {
            stringProvider = new HollowObjectFactoryProvider(typeDataAccess, stringTypeAPI, factory);
        }

        typeDataAccess = dataAccess.getTypeDataAccess("DataLocation");
        if(typeDataAccess != null) {
            dataLocationTypeAPI = new DataLocationTypeAPI(this, (HollowObjectTypeDataAccess)typeDataAccess);
        } else {
            dataLocationTypeAPI = new DataLocationTypeAPI(this, new HollowObjectMissingDataAccess(dataAccess, "DataLocation"));
        }
        addTypeAPI(dataLocationTypeAPI);
        factory = factoryOverrides.get("DataLocation");
        if(factory == null)
            factory = new DataLocationHollowFactory();
        if(cachedTypes.contains("DataLocation")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.dataLocationProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.dataLocationProvider;
            dataLocationProvider = new HollowObjectCacheProvider(typeDataAccess, dataLocationTypeAPI, factory, previousCacheProvider);
        } else {
            dataLocationProvider = new HollowObjectFactoryProvider(typeDataAccess, dataLocationTypeAPI, factory);
        }

        typeDataAccess = dataAccess.getTypeDataAccess("Email");
        if(typeDataAccess != null) {
            emailTypeAPI = new EmailTypeAPI(this, (HollowObjectTypeDataAccess)typeDataAccess);
        } else {
            emailTypeAPI = new EmailTypeAPI(this, new HollowObjectMissingDataAccess(dataAccess, "Email"));
        }
        addTypeAPI(emailTypeAPI);
        factory = factoryOverrides.get("Email");
        if(factory == null)
            factory = new EmailHollowFactory();
        if(cachedTypes.contains("Email")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.emailProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.emailProvider;
            emailProvider = new HollowObjectCacheProvider(typeDataAccess, emailTypeAPI, factory, previousCacheProvider);
        } else {
            emailProvider = new HollowObjectFactoryProvider(typeDataAccess, emailTypeAPI, factory);
        }

        typeDataAccess = dataAccess.getTypeDataAccess("ListOfEmail");
        if(typeDataAccess != null) {
            listOfEmailTypeAPI = new ListOfEmailTypeAPI(this, (HollowListTypeDataAccess)typeDataAccess);
        } else {
            listOfEmailTypeAPI = new ListOfEmailTypeAPI(this, new HollowListMissingDataAccess(dataAccess, "ListOfEmail"));
        }
        addTypeAPI(listOfEmailTypeAPI);
        factory = factoryOverrides.get("ListOfEmail");
        if(factory == null)
            factory = new ListOfEmailHollowFactory();
        if(cachedTypes.contains("ListOfEmail")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.listOfEmailProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.listOfEmailProvider;
            listOfEmailProvider = new HollowObjectCacheProvider(typeDataAccess, listOfEmailTypeAPI, factory, previousCacheProvider);
        } else {
            listOfEmailProvider = new HollowObjectFactoryProvider(typeDataAccess, listOfEmailTypeAPI, factory);
        }

        typeDataAccess = dataAccess.getTypeDataAccess("SetOfString");
        if(typeDataAccess != null) {
            setOfStringTypeAPI = new SetOfStringTypeAPI(this, (HollowSetTypeDataAccess)typeDataAccess);
        } else {
            setOfStringTypeAPI = new SetOfStringTypeAPI(this, new HollowSetMissingDataAccess(dataAccess, "SetOfString"));
        }
        addTypeAPI(setOfStringTypeAPI);
        factory = factoryOverrides.get("SetOfString");
        if(factory == null)
            factory = new SetOfStringHollowFactory();
        if(cachedTypes.contains("SetOfString")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.setOfStringProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.setOfStringProvider;
            setOfStringProvider = new HollowObjectCacheProvider(typeDataAccess, setOfStringTypeAPI, factory, previousCacheProvider);
        } else {
            setOfStringProvider = new HollowObjectFactoryProvider(typeDataAccess, setOfStringTypeAPI, factory);
        }

        typeDataAccess = dataAccess.getTypeDataAccess("OfflineAddressBook");
        if(typeDataAccess != null) {
            offlineAddressBookTypeAPI = new OfflineAddressBookTypeAPI(this, (HollowObjectTypeDataAccess)typeDataAccess);
        } else {
            offlineAddressBookTypeAPI = new OfflineAddressBookTypeAPI(this, new HollowObjectMissingDataAccess(dataAccess, "OfflineAddressBook"));
        }
        addTypeAPI(offlineAddressBookTypeAPI);
        factory = factoryOverrides.get("OfflineAddressBook");
        if(factory == null)
            factory = new OfflineAddressBookHollowFactory();
        if(cachedTypes.contains("OfflineAddressBook")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.offlineAddressBookProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.offlineAddressBookProvider;
            offlineAddressBookProvider = new HollowObjectCacheProvider(typeDataAccess, offlineAddressBookTypeAPI, factory, previousCacheProvider);
        } else {
            offlineAddressBookProvider = new HollowObjectFactoryProvider(typeDataAccess, offlineAddressBookTypeAPI, factory);
        }

        typeDataAccess = dataAccess.getTypeDataAccess("AddressBookRecord");
        if(typeDataAccess != null) {
            addressBookRecordTypeAPI = new AddressBookRecordTypeAPI(this, (HollowObjectTypeDataAccess)typeDataAccess);
        } else {
            addressBookRecordTypeAPI = new AddressBookRecordTypeAPI(this, new HollowObjectMissingDataAccess(dataAccess, "AddressBookRecord"));
        }
        addTypeAPI(addressBookRecordTypeAPI);
        factory = factoryOverrides.get("AddressBookRecord");
        if(factory == null)
            factory = new AddressBookRecordHollowFactory();
        if(cachedTypes.contains("AddressBookRecord")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.addressBookRecordProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.addressBookRecordProvider;
            addressBookRecordProvider = new HollowObjectCacheProvider(typeDataAccess, addressBookRecordTypeAPI, factory, previousCacheProvider);
        } else {
            addressBookRecordProvider = new HollowObjectFactoryProvider(typeDataAccess, addressBookRecordTypeAPI, factory);
        }

    }

    public void detachCaches() {
        if(dateProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)dateProvider).detach();
        if(stringProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)stringProvider).detach();
        if(dataLocationProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)dataLocationProvider).detach();
        if(emailProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)emailProvider).detach();
        if(listOfEmailProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)listOfEmailProvider).detach();
        if(setOfStringProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)setOfStringProvider).detach();
        if(offlineAddressBookProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)offlineAddressBookProvider).detach();
        if(addressBookRecordProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)addressBookRecordProvider).detach();
    }

    public DateTypeAPI getDateTypeAPI() {
        return dateTypeAPI;
    }
    public StringTypeAPI getStringTypeAPI() {
        return stringTypeAPI;
    }
    public DataLocationTypeAPI getDataLocationTypeAPI() {
        return dataLocationTypeAPI;
    }
    public EmailTypeAPI getEmailTypeAPI() {
        return emailTypeAPI;
    }
    public ListOfEmailTypeAPI getListOfEmailTypeAPI() {
        return listOfEmailTypeAPI;
    }
    public SetOfStringTypeAPI getSetOfStringTypeAPI() {
        return setOfStringTypeAPI;
    }
    public OfflineAddressBookTypeAPI getOfflineAddressBookTypeAPI() {
        return offlineAddressBookTypeAPI;
    }
    public AddressBookRecordTypeAPI getAddressBookRecordTypeAPI() {
        return addressBookRecordTypeAPI;
    }
    public Collection<Date> getAllDate() {
        return new AllHollowRecordCollection<Date>(getDataAccess().getTypeDataAccess("Date").getTypeState()) {
            protected Date getForOrdinal(int ordinal) {
                return getDate(ordinal);
            }
        };
    }
    public Date getDate(int ordinal) {
        objectCreationSampler.recordCreation(0);
        return (Date)dateProvider.getHollowObject(ordinal);
    }
    public Collection<HString> getAllHString() {
        return new AllHollowRecordCollection<HString>(getDataAccess().getTypeDataAccess("String").getTypeState()) {
            protected HString getForOrdinal(int ordinal) {
                return getHString(ordinal);
            }
        };
    }
    public HString getHString(int ordinal) {
        objectCreationSampler.recordCreation(1);
        return (HString)stringProvider.getHollowObject(ordinal);
    }
    public Collection<DataLocation> getAllDataLocation() {
        return new AllHollowRecordCollection<DataLocation>(getDataAccess().getTypeDataAccess("DataLocation").getTypeState()) {
            protected DataLocation getForOrdinal(int ordinal) {
                return getDataLocation(ordinal);
            }
        };
    }
    public DataLocation getDataLocation(int ordinal) {
        objectCreationSampler.recordCreation(2);
        return (DataLocation)dataLocationProvider.getHollowObject(ordinal);
    }
    public Collection<Email> getAllEmail() {
        return new AllHollowRecordCollection<Email>(getDataAccess().getTypeDataAccess("Email").getTypeState()) {
            protected Email getForOrdinal(int ordinal) {
                return getEmail(ordinal);
            }
        };
    }
    public Email getEmail(int ordinal) {
        objectCreationSampler.recordCreation(3);
        return (Email)emailProvider.getHollowObject(ordinal);
    }
    public Collection<ListOfEmail> getAllListOfEmail() {
        return new AllHollowRecordCollection<ListOfEmail>(getDataAccess().getTypeDataAccess("ListOfEmail").getTypeState()) {
            protected ListOfEmail getForOrdinal(int ordinal) {
                return getListOfEmail(ordinal);
            }
        };
    }
    public ListOfEmail getListOfEmail(int ordinal) {
        objectCreationSampler.recordCreation(4);
        return (ListOfEmail)listOfEmailProvider.getHollowObject(ordinal);
    }
    public Collection<SetOfString> getAllSetOfString() {
        return new AllHollowRecordCollection<SetOfString>(getDataAccess().getTypeDataAccess("SetOfString").getTypeState()) {
            protected SetOfString getForOrdinal(int ordinal) {
                return getSetOfString(ordinal);
            }
        };
    }
    public SetOfString getSetOfString(int ordinal) {
        objectCreationSampler.recordCreation(5);
        return (SetOfString)setOfStringProvider.getHollowObject(ordinal);
    }
    public Collection<OfflineAddressBook> getAllOfflineAddressBook() {
        return new AllHollowRecordCollection<OfflineAddressBook>(getDataAccess().getTypeDataAccess("OfflineAddressBook").getTypeState()) {
            protected OfflineAddressBook getForOrdinal(int ordinal) {
                return getOfflineAddressBook(ordinal);
            }
        };
    }
    public OfflineAddressBook getOfflineAddressBook(int ordinal) {
        objectCreationSampler.recordCreation(6);
        return (OfflineAddressBook)offlineAddressBookProvider.getHollowObject(ordinal);
    }
    public Collection<AddressBookRecord> getAllAddressBookRecord() {
        return new AllHollowRecordCollection<AddressBookRecord>(getDataAccess().getTypeDataAccess("AddressBookRecord").getTypeState()) {
            protected AddressBookRecord getForOrdinal(int ordinal) {
                return getAddressBookRecord(ordinal);
            }
        };
    }
    public AddressBookRecord getAddressBookRecord(int ordinal) {
        objectCreationSampler.recordCreation(7);
        return (AddressBookRecord)addressBookRecordProvider.getHollowObject(ordinal);
    }
    public void setSamplingDirector(HollowSamplingDirector director) {
        super.setSamplingDirector(director);
        objectCreationSampler.setSamplingDirector(director);
    }

    public Collection<SampleResult> getObjectCreationSamplingResults() {
        return objectCreationSampler.getSampleResults();
    }

}
