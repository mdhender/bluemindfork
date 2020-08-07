package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class AddressBookRecord extends HollowObject {

    public AddressBookRecord(AddressBookRecordDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public OfflineAddressBook getAddressBook() {
        int refOrdinal = delegate().getAddressBookOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getOfflineAddressBook(refOrdinal);
    }

    public HString getUid() {
        int refOrdinal = delegate().getUidOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getDistinguishedName() {
        int refOrdinal = delegate().getDistinguishedNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getDomain() {
        int refOrdinal = delegate().getDomainOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getKind() {
        int refOrdinal = delegate().getKindOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public ListOfEmail getEmails() {
        int refOrdinal = delegate().getEmailsOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getListOfEmail(refOrdinal);
    }

    public Date getCreated() {
        int refOrdinal = delegate().getCreatedOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getDate(refOrdinal);
    }

    public Date getUpdated() {
        int refOrdinal = delegate().getUpdatedOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getDate(refOrdinal);
    }

    public HString getEmail() {
        int refOrdinal = delegate().getEmailOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public long getMinimalid() {
        return delegate().getMinimalid(ordinal);
    }

    public Long getMinimalidBoxed() {
        return delegate().getMinimalidBoxed(ordinal);
    }

    public HString getName() {
        int refOrdinal = delegate().getNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getSurname() {
        int refOrdinal = delegate().getSurnameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getGivenName() {
        int refOrdinal = delegate().getGivenNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getTitle() {
        int refOrdinal = delegate().getTitleOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getOfficeLocation() {
        int refOrdinal = delegate().getOfficeLocationOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getDepartmentName() {
        int refOrdinal = delegate().getDepartmentNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getCompanyName() {
        int refOrdinal = delegate().getCompanyNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAssistant() {
        int refOrdinal = delegate().getAssistantOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAddressBookManagerDistinguishedName() {
        int refOrdinal = delegate().getAddressBookManagerDistinguishedNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAddressBookPhoneticGivenName() {
        int refOrdinal = delegate().getAddressBookPhoneticGivenNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAddressBookPhoneticSurname() {
        int refOrdinal = delegate().getAddressBookPhoneticSurnameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAddressBookPhoneticCompanyName() {
        int refOrdinal = delegate().getAddressBookPhoneticCompanyNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAddressBookPhoneticDepartmentName() {
        int refOrdinal = delegate().getAddressBookPhoneticDepartmentNameOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getStreetAddress() {
        int refOrdinal = delegate().getStreetAddressOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getPostOfficeBox() {
        int refOrdinal = delegate().getPostOfficeBoxOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getLocality() {
        int refOrdinal = delegate().getLocalityOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getStateOrProvince() {
        int refOrdinal = delegate().getStateOrProvinceOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getPostalCode() {
        int refOrdinal = delegate().getPostalCodeOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getCountry() {
        int refOrdinal = delegate().getCountryOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public DataLocation getDataLocation() {
        int refOrdinal = delegate().getDataLocationOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getDataLocation(refOrdinal);
    }

    public HString getBusinessTelephoneNumber() {
        int refOrdinal = delegate().getBusinessTelephoneNumberOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getHomeTelephoneNumber() {
        int refOrdinal = delegate().getHomeTelephoneNumberOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getBusiness2TelephoneNumbers() {
        int refOrdinal = delegate().getBusiness2TelephoneNumbersOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getHome2TelephoneNumber() {
        int refOrdinal = delegate().getHome2TelephoneNumberOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getMobileTelephoneNumber() {
        int refOrdinal = delegate().getMobileTelephoneNumberOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getPagerTelephoneNumber() {
        int refOrdinal = delegate().getPagerTelephoneNumberOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getPrimaryFaxNumber() {
        int refOrdinal = delegate().getPrimaryFaxNumberOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAssistantTelephoneNumber() {
        int refOrdinal = delegate().getAssistantTelephoneNumberOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getUserCertificate() {
        int refOrdinal = delegate().getUserCertificateOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public byte[] getAddressBookX509Certificate() {
        return delegate().getAddressBookX509Certificate(ordinal);
    }

    public byte[] getUserX509Certificate() {
        return delegate().getUserX509Certificate(ordinal);
    }

    public byte[] getThumbnail() {
        return delegate().getThumbnail(ordinal);
    }

    public boolean getHidden() {
        return delegate().getHidden(ordinal);
    }

    public Boolean getHiddenBoxed() {
        return delegate().getHiddenBoxed(ordinal);
    }

    public OfflineDirectoryAPI api() {
        return typeApi().getAPI();
    }

    public AddressBookRecordTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected AddressBookRecordDelegate delegate() {
        return (AddressBookRecordDelegate)delegate;
    }

    /**
     * Creates a unique key index for {@code AddressBookRecord} that has a primary key.
     * The primary key is represented by the class {@link String}.
     * <p>
     * By default the unique key index will not track updates to the {@code consumer} and thus
     * any changes will not be reflected in matched results.  To track updates the index must be
     * {@link HollowConsumer#addRefreshListener(HollowConsumer.RefreshListener) registered}
     * with the {@code consumer}
     *
     * @param consumer the consumer
     * @return the unique key index
     */
    public static UniqueKeyIndex<AddressBookRecord, String> uniqueIndex(HollowConsumer consumer) {
        return UniqueKeyIndex.from(consumer, AddressBookRecord.class)
            .bindToPrimaryKey()
            .usingPath("uid", String.class);
    }

}