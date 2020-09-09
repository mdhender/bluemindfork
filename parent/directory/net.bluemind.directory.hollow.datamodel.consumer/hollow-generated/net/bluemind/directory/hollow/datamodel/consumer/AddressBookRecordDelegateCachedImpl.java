package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.custom.HollowTypeAPI;
import com.netflix.hollow.api.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class AddressBookRecordDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, AddressBookRecordDelegate {

    private final String uid;
    private final String distinguishedName;
    private final int domainOrdinal;
    private final int kindOrdinal;
    private final int emailsOrdinal;
    private final int createdOrdinal;
    private final int updatedOrdinal;
    private final String email;
    private final Long minimalid;
    private final String name;
    private final String surname;
    private final String givenName;
    private final int titleOrdinal;
    private final int officeLocationOrdinal;
    private final int departmentNameOrdinal;
    private final int companyNameOrdinal;
    private final int assistantOrdinal;
    private final int addressBookManagerDistinguishedNameOrdinal;
    private final int addressBookPhoneticGivenNameOrdinal;
    private final int addressBookPhoneticSurnameOrdinal;
    private final int addressBookPhoneticCompanyNameOrdinal;
    private final int addressBookPhoneticDepartmentNameOrdinal;
    private final int streetAddressOrdinal;
    private final int postOfficeBoxOrdinal;
    private final int localityOrdinal;
    private final int stateOrProvinceOrdinal;
    private final int postalCodeOrdinal;
    private final int countryOrdinal;
    private final int dataLocationOrdinal;
    private final int businessTelephoneNumberOrdinal;
    private final int homeTelephoneNumberOrdinal;
    private final int business2TelephoneNumbersOrdinal;
    private final int home2TelephoneNumberOrdinal;
    private final int mobileTelephoneNumberOrdinal;
    private final int pagerTelephoneNumberOrdinal;
    private final int primaryFaxNumberOrdinal;
    private final int assistantTelephoneNumberOrdinal;
    private final int userCertificateOrdinal;
    private final byte[] addressBookX509Certificate;
    private final byte[] userX509Certificate;
    private final byte[] thumbnail;
    private final Boolean hidden;
    private final int anrOrdinal;
    private AddressBookRecordTypeAPI typeAPI;

    public AddressBookRecordDelegateCachedImpl(AddressBookRecordTypeAPI typeAPI, int ordinal) {
        this.uid = typeAPI.getUid(ordinal);
        this.distinguishedName = typeAPI.getDistinguishedName(ordinal);
        this.domainOrdinal = typeAPI.getDomainOrdinal(ordinal);
        this.kindOrdinal = typeAPI.getKindOrdinal(ordinal);
        this.emailsOrdinal = typeAPI.getEmailsOrdinal(ordinal);
        this.createdOrdinal = typeAPI.getCreatedOrdinal(ordinal);
        this.updatedOrdinal = typeAPI.getUpdatedOrdinal(ordinal);
        this.email = typeAPI.getEmail(ordinal);
        this.minimalid = typeAPI.getMinimalidBoxed(ordinal);
        this.name = typeAPI.getName(ordinal);
        this.surname = typeAPI.getSurname(ordinal);
        this.givenName = typeAPI.getGivenName(ordinal);
        this.titleOrdinal = typeAPI.getTitleOrdinal(ordinal);
        this.officeLocationOrdinal = typeAPI.getOfficeLocationOrdinal(ordinal);
        this.departmentNameOrdinal = typeAPI.getDepartmentNameOrdinal(ordinal);
        this.companyNameOrdinal = typeAPI.getCompanyNameOrdinal(ordinal);
        this.assistantOrdinal = typeAPI.getAssistantOrdinal(ordinal);
        this.addressBookManagerDistinguishedNameOrdinal = typeAPI.getAddressBookManagerDistinguishedNameOrdinal(ordinal);
        this.addressBookPhoneticGivenNameOrdinal = typeAPI.getAddressBookPhoneticGivenNameOrdinal(ordinal);
        this.addressBookPhoneticSurnameOrdinal = typeAPI.getAddressBookPhoneticSurnameOrdinal(ordinal);
        this.addressBookPhoneticCompanyNameOrdinal = typeAPI.getAddressBookPhoneticCompanyNameOrdinal(ordinal);
        this.addressBookPhoneticDepartmentNameOrdinal = typeAPI.getAddressBookPhoneticDepartmentNameOrdinal(ordinal);
        this.streetAddressOrdinal = typeAPI.getStreetAddressOrdinal(ordinal);
        this.postOfficeBoxOrdinal = typeAPI.getPostOfficeBoxOrdinal(ordinal);
        this.localityOrdinal = typeAPI.getLocalityOrdinal(ordinal);
        this.stateOrProvinceOrdinal = typeAPI.getStateOrProvinceOrdinal(ordinal);
        this.postalCodeOrdinal = typeAPI.getPostalCodeOrdinal(ordinal);
        this.countryOrdinal = typeAPI.getCountryOrdinal(ordinal);
        this.dataLocationOrdinal = typeAPI.getDataLocationOrdinal(ordinal);
        this.businessTelephoneNumberOrdinal = typeAPI.getBusinessTelephoneNumberOrdinal(ordinal);
        this.homeTelephoneNumberOrdinal = typeAPI.getHomeTelephoneNumberOrdinal(ordinal);
        this.business2TelephoneNumbersOrdinal = typeAPI.getBusiness2TelephoneNumbersOrdinal(ordinal);
        this.home2TelephoneNumberOrdinal = typeAPI.getHome2TelephoneNumberOrdinal(ordinal);
        this.mobileTelephoneNumberOrdinal = typeAPI.getMobileTelephoneNumberOrdinal(ordinal);
        this.pagerTelephoneNumberOrdinal = typeAPI.getPagerTelephoneNumberOrdinal(ordinal);
        this.primaryFaxNumberOrdinal = typeAPI.getPrimaryFaxNumberOrdinal(ordinal);
        this.assistantTelephoneNumberOrdinal = typeAPI.getAssistantTelephoneNumberOrdinal(ordinal);
        this.userCertificateOrdinal = typeAPI.getUserCertificateOrdinal(ordinal);
        this.addressBookX509Certificate = typeAPI.getAddressBookX509Certificate(ordinal);
        this.userX509Certificate = typeAPI.getUserX509Certificate(ordinal);
        this.thumbnail = typeAPI.getThumbnail(ordinal);
        this.hidden = typeAPI.getHiddenBoxed(ordinal);
        this.anrOrdinal = typeAPI.getAnrOrdinal(ordinal);
        this.typeAPI = typeAPI;
    }

    public String getUid(int ordinal) {
        return uid;
    }

    public boolean isUidEqual(int ordinal, String testValue) {
        if(testValue == null)
            return uid == null;
        return testValue.equals(uid);
    }

    public String getDistinguishedName(int ordinal) {
        return distinguishedName;
    }

    public boolean isDistinguishedNameEqual(int ordinal, String testValue) {
        if(testValue == null)
            return distinguishedName == null;
        return testValue.equals(distinguishedName);
    }

    public int getDomainOrdinal(int ordinal) {
        return domainOrdinal;
    }

    public int getKindOrdinal(int ordinal) {
        return kindOrdinal;
    }

    public int getEmailsOrdinal(int ordinal) {
        return emailsOrdinal;
    }

    public int getCreatedOrdinal(int ordinal) {
        return createdOrdinal;
    }

    public int getUpdatedOrdinal(int ordinal) {
        return updatedOrdinal;
    }

    public String getEmail(int ordinal) {
        return email;
    }

    public boolean isEmailEqual(int ordinal, String testValue) {
        if(testValue == null)
            return email == null;
        return testValue.equals(email);
    }

    public long getMinimalid(int ordinal) {
        if(minimalid == null)
            return Long.MIN_VALUE;
        return minimalid.longValue();
    }

    public Long getMinimalidBoxed(int ordinal) {
        return minimalid;
    }

    public String getName(int ordinal) {
        return name;
    }

    public boolean isNameEqual(int ordinal, String testValue) {
        if(testValue == null)
            return name == null;
        return testValue.equals(name);
    }

    public String getSurname(int ordinal) {
        return surname;
    }

    public boolean isSurnameEqual(int ordinal, String testValue) {
        if(testValue == null)
            return surname == null;
        return testValue.equals(surname);
    }

    public String getGivenName(int ordinal) {
        return givenName;
    }

    public boolean isGivenNameEqual(int ordinal, String testValue) {
        if(testValue == null)
            return givenName == null;
        return testValue.equals(givenName);
    }

    public int getTitleOrdinal(int ordinal) {
        return titleOrdinal;
    }

    public int getOfficeLocationOrdinal(int ordinal) {
        return officeLocationOrdinal;
    }

    public int getDepartmentNameOrdinal(int ordinal) {
        return departmentNameOrdinal;
    }

    public int getCompanyNameOrdinal(int ordinal) {
        return companyNameOrdinal;
    }

    public int getAssistantOrdinal(int ordinal) {
        return assistantOrdinal;
    }

    public int getAddressBookManagerDistinguishedNameOrdinal(int ordinal) {
        return addressBookManagerDistinguishedNameOrdinal;
    }

    public int getAddressBookPhoneticGivenNameOrdinal(int ordinal) {
        return addressBookPhoneticGivenNameOrdinal;
    }

    public int getAddressBookPhoneticSurnameOrdinal(int ordinal) {
        return addressBookPhoneticSurnameOrdinal;
    }

    public int getAddressBookPhoneticCompanyNameOrdinal(int ordinal) {
        return addressBookPhoneticCompanyNameOrdinal;
    }

    public int getAddressBookPhoneticDepartmentNameOrdinal(int ordinal) {
        return addressBookPhoneticDepartmentNameOrdinal;
    }

    public int getStreetAddressOrdinal(int ordinal) {
        return streetAddressOrdinal;
    }

    public int getPostOfficeBoxOrdinal(int ordinal) {
        return postOfficeBoxOrdinal;
    }

    public int getLocalityOrdinal(int ordinal) {
        return localityOrdinal;
    }

    public int getStateOrProvinceOrdinal(int ordinal) {
        return stateOrProvinceOrdinal;
    }

    public int getPostalCodeOrdinal(int ordinal) {
        return postalCodeOrdinal;
    }

    public int getCountryOrdinal(int ordinal) {
        return countryOrdinal;
    }

    public int getDataLocationOrdinal(int ordinal) {
        return dataLocationOrdinal;
    }

    public int getBusinessTelephoneNumberOrdinal(int ordinal) {
        return businessTelephoneNumberOrdinal;
    }

    public int getHomeTelephoneNumberOrdinal(int ordinal) {
        return homeTelephoneNumberOrdinal;
    }

    public int getBusiness2TelephoneNumbersOrdinal(int ordinal) {
        return business2TelephoneNumbersOrdinal;
    }

    public int getHome2TelephoneNumberOrdinal(int ordinal) {
        return home2TelephoneNumberOrdinal;
    }

    public int getMobileTelephoneNumberOrdinal(int ordinal) {
        return mobileTelephoneNumberOrdinal;
    }

    public int getPagerTelephoneNumberOrdinal(int ordinal) {
        return pagerTelephoneNumberOrdinal;
    }

    public int getPrimaryFaxNumberOrdinal(int ordinal) {
        return primaryFaxNumberOrdinal;
    }

    public int getAssistantTelephoneNumberOrdinal(int ordinal) {
        return assistantTelephoneNumberOrdinal;
    }

    public int getUserCertificateOrdinal(int ordinal) {
        return userCertificateOrdinal;
    }

    public byte[] getAddressBookX509Certificate(int ordinal) {
        return (byte[]) addressBookX509Certificate;
    }

    public byte[] getUserX509Certificate(int ordinal) {
        return (byte[]) userX509Certificate;
    }

    public byte[] getThumbnail(int ordinal) {
        return (byte[]) thumbnail;
    }

    public boolean getHidden(int ordinal) {
        if(hidden == null)
            return false;
        return hidden.booleanValue();
    }

    public Boolean getHiddenBoxed(int ordinal) {
        return hidden;
    }

    public int getAnrOrdinal(int ordinal) {
        return anrOrdinal;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public AddressBookRecordTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (AddressBookRecordTypeAPI) typeAPI;
    }

}