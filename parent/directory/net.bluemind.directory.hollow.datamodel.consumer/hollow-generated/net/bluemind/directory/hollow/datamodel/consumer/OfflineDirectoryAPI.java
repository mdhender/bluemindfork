package net.bluemind.directory.hollow.datamodel.consumer;

import java.util.Objects;
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

    private final AnrTokenTypeAPI anrTokenTypeAPI;
    private final DateTypeAPI dateTypeAPI;
    private final EmailTypeAPI emailTypeAPI;
    private final ListOfAnrTokenTypeAPI listOfAnrTokenTypeAPI;
    private final ListOfEmailTypeAPI listOfEmailTypeAPI;
    private final StringTypeAPI stringTypeAPI;
    private final DataLocationTypeAPI dataLocationTypeAPI;
    private final AddressBookRecordTypeAPI addressBookRecordTypeAPI;
    private final SetOfStringTypeAPI setOfStringTypeAPI;
    private final OfflineAddressBookTypeAPI offlineAddressBookTypeAPI;

    private final HollowObjectProvider anrTokenProvider;
    private final HollowObjectProvider dateProvider;
    private final HollowObjectProvider emailProvider;
    private final HollowObjectProvider listOfAnrTokenProvider;
    private final HollowObjectProvider listOfEmailProvider;
    private final HollowObjectProvider stringProvider;
    private final HollowObjectProvider dataLocationProvider;
    private final HollowObjectProvider addressBookRecordProvider;
    private final HollowObjectProvider setOfStringProvider;
    private final HollowObjectProvider offlineAddressBookProvider;

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

        objectCreationSampler = new HollowObjectCreationSampler("AnrToken","Date","Email","ListOfAnrToken","ListOfEmail","String","DataLocation","AddressBookRecord","SetOfString","OfflineAddressBook");

        typeDataAccess = dataAccess.getTypeDataAccess("AnrToken");
        if(typeDataAccess != null) {
            anrTokenTypeAPI = new AnrTokenTypeAPI(this, (HollowObjectTypeDataAccess)typeDataAccess);
        } else {
            anrTokenTypeAPI = new AnrTokenTypeAPI(this, new HollowObjectMissingDataAccess(dataAccess, "AnrToken"));
        }
        addTypeAPI(anrTokenTypeAPI);
        factory = factoryOverrides.get("AnrToken");
        if(factory == null)
            factory = new AnrTokenHollowFactory();
        if(cachedTypes.contains("AnrToken")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.anrTokenProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.anrTokenProvider;
            anrTokenProvider = new HollowObjectCacheProvider(typeDataAccess, anrTokenTypeAPI, factory, previousCacheProvider);
        } else {
            anrTokenProvider = new HollowObjectFactoryProvider(typeDataAccess, anrTokenTypeAPI, factory);
        }

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

        typeDataAccess = dataAccess.getTypeDataAccess("ListOfAnrToken");
        if(typeDataAccess != null) {
            listOfAnrTokenTypeAPI = new ListOfAnrTokenTypeAPI(this, (HollowListTypeDataAccess)typeDataAccess);
        } else {
            listOfAnrTokenTypeAPI = new ListOfAnrTokenTypeAPI(this, new HollowListMissingDataAccess(dataAccess, "ListOfAnrToken"));
        }
        addTypeAPI(listOfAnrTokenTypeAPI);
        factory = factoryOverrides.get("ListOfAnrToken");
        if(factory == null)
            factory = new ListOfAnrTokenHollowFactory();
        if(cachedTypes.contains("ListOfAnrToken")) {
            HollowObjectCacheProvider previousCacheProvider = null;
            if(previousCycleAPI != null && (previousCycleAPI.listOfAnrTokenProvider instanceof HollowObjectCacheProvider))
                previousCacheProvider = (HollowObjectCacheProvider) previousCycleAPI.listOfAnrTokenProvider;
            listOfAnrTokenProvider = new HollowObjectCacheProvider(typeDataAccess, listOfAnrTokenTypeAPI, factory, previousCacheProvider);
        } else {
            listOfAnrTokenProvider = new HollowObjectFactoryProvider(typeDataAccess, listOfAnrTokenTypeAPI, factory);
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

    }

    public void detachCaches() {
        if(anrTokenProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)anrTokenProvider).detach();
        if(dateProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)dateProvider).detach();
        if(emailProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)emailProvider).detach();
        if(listOfAnrTokenProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)listOfAnrTokenProvider).detach();
        if(listOfEmailProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)listOfEmailProvider).detach();
        if(stringProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)stringProvider).detach();
        if(dataLocationProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)dataLocationProvider).detach();
        if(addressBookRecordProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)addressBookRecordProvider).detach();
        if(setOfStringProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)setOfStringProvider).detach();
        if(offlineAddressBookProvider instanceof HollowObjectCacheProvider)
            ((HollowObjectCacheProvider)offlineAddressBookProvider).detach();
    }

    public AnrTokenTypeAPI getAnrTokenTypeAPI() {
        return anrTokenTypeAPI;
    }
    public DateTypeAPI getDateTypeAPI() {
        return dateTypeAPI;
    }
    public EmailTypeAPI getEmailTypeAPI() {
        return emailTypeAPI;
    }
    public ListOfAnrTokenTypeAPI getListOfAnrTokenTypeAPI() {
        return listOfAnrTokenTypeAPI;
    }
    public ListOfEmailTypeAPI getListOfEmailTypeAPI() {
        return listOfEmailTypeAPI;
    }
    public StringTypeAPI getStringTypeAPI() {
        return stringTypeAPI;
    }
    public DataLocationTypeAPI getDataLocationTypeAPI() {
        return dataLocationTypeAPI;
    }
    public AddressBookRecordTypeAPI getAddressBookRecordTypeAPI() {
        return addressBookRecordTypeAPI;
    }
    public SetOfStringTypeAPI getSetOfStringTypeAPI() {
        return setOfStringTypeAPI;
    }
    public OfflineAddressBookTypeAPI getOfflineAddressBookTypeAPI() {
        return offlineAddressBookTypeAPI;
    }
    public Collection<AnrToken> getAllAnrToken() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("AnrToken"), "type not loaded or does not exist in dataset; type=AnrToken");
        return new AllHollowRecordCollection<AnrToken>(tda.getTypeState()) {
            protected AnrToken getForOrdinal(int ordinal) {
                return getAnrToken(ordinal);
            }
        };
    }
    public AnrToken getAnrToken(int ordinal) {
        objectCreationSampler.recordCreation(0);
        return (AnrToken)anrTokenProvider.getHollowObject(ordinal);
    }
    public Collection<Date> getAllDate() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("Date"), "type not loaded or does not exist in dataset; type=Date");
        return new AllHollowRecordCollection<Date>(tda.getTypeState()) {
            protected Date getForOrdinal(int ordinal) {
                return getDate(ordinal);
            }
        };
    }
    public Date getDate(int ordinal) {
        objectCreationSampler.recordCreation(1);
        return (Date)dateProvider.getHollowObject(ordinal);
    }
    public Collection<Email> getAllEmail() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("Email"), "type not loaded or does not exist in dataset; type=Email");
        return new AllHollowRecordCollection<Email>(tda.getTypeState()) {
            protected Email getForOrdinal(int ordinal) {
                return getEmail(ordinal);
            }
        };
    }
    public Email getEmail(int ordinal) {
        objectCreationSampler.recordCreation(2);
        return (Email)emailProvider.getHollowObject(ordinal);
    }
    public Collection<ListOfAnrToken> getAllListOfAnrToken() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("ListOfAnrToken"), "type not loaded or does not exist in dataset; type=ListOfAnrToken");
        return new AllHollowRecordCollection<ListOfAnrToken>(tda.getTypeState()) {
            protected ListOfAnrToken getForOrdinal(int ordinal) {
                return getListOfAnrToken(ordinal);
            }
        };
    }
    public ListOfAnrToken getListOfAnrToken(int ordinal) {
        objectCreationSampler.recordCreation(3);
        return (ListOfAnrToken)listOfAnrTokenProvider.getHollowObject(ordinal);
    }
    public Collection<ListOfEmail> getAllListOfEmail() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("ListOfEmail"), "type not loaded or does not exist in dataset; type=ListOfEmail");
        return new AllHollowRecordCollection<ListOfEmail>(tda.getTypeState()) {
            protected ListOfEmail getForOrdinal(int ordinal) {
                return getListOfEmail(ordinal);
            }
        };
    }
    public ListOfEmail getListOfEmail(int ordinal) {
        objectCreationSampler.recordCreation(4);
        return (ListOfEmail)listOfEmailProvider.getHollowObject(ordinal);
    }
    public Collection<HString> getAllHString() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("String"), "type not loaded or does not exist in dataset; type=String");
        return new AllHollowRecordCollection<HString>(tda.getTypeState()) {
            protected HString getForOrdinal(int ordinal) {
                return getHString(ordinal);
            }
        };
    }
    public HString getHString(int ordinal) {
        objectCreationSampler.recordCreation(5);
        return (HString)stringProvider.getHollowObject(ordinal);
    }
    public Collection<DataLocation> getAllDataLocation() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("DataLocation"), "type not loaded or does not exist in dataset; type=DataLocation");
        return new AllHollowRecordCollection<DataLocation>(tda.getTypeState()) {
            protected DataLocation getForOrdinal(int ordinal) {
                return getDataLocation(ordinal);
            }
        };
    }
    public DataLocation getDataLocation(int ordinal) {
        objectCreationSampler.recordCreation(6);
        return (DataLocation)dataLocationProvider.getHollowObject(ordinal);
    }
    public Collection<AddressBookRecord> getAllAddressBookRecord() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("AddressBookRecord"), "type not loaded or does not exist in dataset; type=AddressBookRecord");
        return new AllHollowRecordCollection<AddressBookRecord>(tda.getTypeState()) {
            protected AddressBookRecord getForOrdinal(int ordinal) {
                return getAddressBookRecord(ordinal);
            }
        };
    }
    public AddressBookRecord getAddressBookRecord(int ordinal) {
        objectCreationSampler.recordCreation(7);
        return (AddressBookRecord)addressBookRecordProvider.getHollowObject(ordinal);
    }
    public Collection<SetOfString> getAllSetOfString() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("SetOfString"), "type not loaded or does not exist in dataset; type=SetOfString");
        return new AllHollowRecordCollection<SetOfString>(tda.getTypeState()) {
            protected SetOfString getForOrdinal(int ordinal) {
                return getSetOfString(ordinal);
            }
        };
    }
    public SetOfString getSetOfString(int ordinal) {
        objectCreationSampler.recordCreation(8);
        return (SetOfString)setOfStringProvider.getHollowObject(ordinal);
    }
    public Collection<OfflineAddressBook> getAllOfflineAddressBook() {
        HollowTypeDataAccess tda = Objects.requireNonNull(getDataAccess().getTypeDataAccess("OfflineAddressBook"), "type not loaded or does not exist in dataset; type=OfflineAddressBook");
        return new AllHollowRecordCollection<OfflineAddressBook>(tda.getTypeState()) {
            protected OfflineAddressBook getForOrdinal(int ordinal) {
                return getOfflineAddressBook(ordinal);
            }
        };
    }
    public OfflineAddressBook getOfflineAddressBook(int ordinal) {
        objectCreationSampler.recordCreation(9);
        return (OfflineAddressBook)offlineAddressBookProvider.getHollowObject(ordinal);
    }
    public void setSamplingDirector(HollowSamplingDirector director) {
        super.setSamplingDirector(director);
        objectCreationSampler.setSamplingDirector(director);
    }

    public Collection<SampleResult> getObjectCreationSamplingResults() {
        return objectCreationSampler.getSampleResults();
    }

}
