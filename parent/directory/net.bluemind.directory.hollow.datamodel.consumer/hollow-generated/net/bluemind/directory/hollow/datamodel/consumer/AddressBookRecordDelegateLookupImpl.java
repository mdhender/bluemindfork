package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class AddressBookRecordDelegateLookupImpl extends HollowObjectAbstractDelegate implements AddressBookRecordDelegate {

    private final AddressBookRecordTypeAPI typeAPI;

    public AddressBookRecordDelegateLookupImpl(AddressBookRecordTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public String getUid(int ordinal) {
        return typeAPI.getUid(ordinal);
    }

    public boolean isUidEqual(int ordinal, String testValue) {
        return typeAPI.isUidEqual(ordinal, testValue);
    }

    public String getDistinguishedName(int ordinal) {
        return typeAPI.getDistinguishedName(ordinal);
    }

    public boolean isDistinguishedNameEqual(int ordinal, String testValue) {
        return typeAPI.isDistinguishedNameEqual(ordinal, testValue);
    }

    public int getDomainOrdinal(int ordinal) {
        return typeAPI.getDomainOrdinal(ordinal);
    }

    public int getKindOrdinal(int ordinal) {
        return typeAPI.getKindOrdinal(ordinal);
    }

    public int getEmailsOrdinal(int ordinal) {
        return typeAPI.getEmailsOrdinal(ordinal);
    }

    public int getCreatedOrdinal(int ordinal) {
        return typeAPI.getCreatedOrdinal(ordinal);
    }

    public int getUpdatedOrdinal(int ordinal) {
        return typeAPI.getUpdatedOrdinal(ordinal);
    }

    public String getEmail(int ordinal) {
        return typeAPI.getEmail(ordinal);
    }

    public boolean isEmailEqual(int ordinal, String testValue) {
        return typeAPI.isEmailEqual(ordinal, testValue);
    }

    public long getMinimalid(int ordinal) {
        return typeAPI.getMinimalid(ordinal);
    }

    public Long getMinimalidBoxed(int ordinal) {
        return typeAPI.getMinimalidBoxed(ordinal);
    }

    public String getName(int ordinal) {
        return typeAPI.getName(ordinal);
    }

    public boolean isNameEqual(int ordinal, String testValue) {
        return typeAPI.isNameEqual(ordinal, testValue);
    }

    public String getSurname(int ordinal) {
        return typeAPI.getSurname(ordinal);
    }

    public boolean isSurnameEqual(int ordinal, String testValue) {
        return typeAPI.isSurnameEqual(ordinal, testValue);
    }

    public String getGivenName(int ordinal) {
        return typeAPI.getGivenName(ordinal);
    }

    public boolean isGivenNameEqual(int ordinal, String testValue) {
        return typeAPI.isGivenNameEqual(ordinal, testValue);
    }

    public int getTitleOrdinal(int ordinal) {
        return typeAPI.getTitleOrdinal(ordinal);
    }

    public int getOfficeLocationOrdinal(int ordinal) {
        return typeAPI.getOfficeLocationOrdinal(ordinal);
    }

    public int getDepartmentNameOrdinal(int ordinal) {
        return typeAPI.getDepartmentNameOrdinal(ordinal);
    }

    public int getCompanyNameOrdinal(int ordinal) {
        return typeAPI.getCompanyNameOrdinal(ordinal);
    }

    public int getAssistantOrdinal(int ordinal) {
        return typeAPI.getAssistantOrdinal(ordinal);
    }

    public int getAddressBookManagerDistinguishedNameOrdinal(int ordinal) {
        return typeAPI.getAddressBookManagerDistinguishedNameOrdinal(ordinal);
    }

    public int getAddressBookPhoneticGivenNameOrdinal(int ordinal) {
        return typeAPI.getAddressBookPhoneticGivenNameOrdinal(ordinal);
    }

    public int getAddressBookPhoneticSurnameOrdinal(int ordinal) {
        return typeAPI.getAddressBookPhoneticSurnameOrdinal(ordinal);
    }

    public int getAddressBookPhoneticCompanyNameOrdinal(int ordinal) {
        return typeAPI.getAddressBookPhoneticCompanyNameOrdinal(ordinal);
    }

    public int getAddressBookPhoneticDepartmentNameOrdinal(int ordinal) {
        return typeAPI.getAddressBookPhoneticDepartmentNameOrdinal(ordinal);
    }

    public int getStreetAddressOrdinal(int ordinal) {
        return typeAPI.getStreetAddressOrdinal(ordinal);
    }

    public int getPostOfficeBoxOrdinal(int ordinal) {
        return typeAPI.getPostOfficeBoxOrdinal(ordinal);
    }

    public int getLocalityOrdinal(int ordinal) {
        return typeAPI.getLocalityOrdinal(ordinal);
    }

    public int getStateOrProvinceOrdinal(int ordinal) {
        return typeAPI.getStateOrProvinceOrdinal(ordinal);
    }

    public int getPostalCodeOrdinal(int ordinal) {
        return typeAPI.getPostalCodeOrdinal(ordinal);
    }

    public int getCountryOrdinal(int ordinal) {
        return typeAPI.getCountryOrdinal(ordinal);
    }

    public int getDataLocationOrdinal(int ordinal) {
        return typeAPI.getDataLocationOrdinal(ordinal);
    }

    public int getBusinessTelephoneNumberOrdinal(int ordinal) {
        return typeAPI.getBusinessTelephoneNumberOrdinal(ordinal);
    }

    public int getHomeTelephoneNumberOrdinal(int ordinal) {
        return typeAPI.getHomeTelephoneNumberOrdinal(ordinal);
    }

    public int getBusiness2TelephoneNumbersOrdinal(int ordinal) {
        return typeAPI.getBusiness2TelephoneNumbersOrdinal(ordinal);
    }

    public int getHome2TelephoneNumberOrdinal(int ordinal) {
        return typeAPI.getHome2TelephoneNumberOrdinal(ordinal);
    }

    public int getMobileTelephoneNumberOrdinal(int ordinal) {
        return typeAPI.getMobileTelephoneNumberOrdinal(ordinal);
    }

    public int getPagerTelephoneNumberOrdinal(int ordinal) {
        return typeAPI.getPagerTelephoneNumberOrdinal(ordinal);
    }

    public int getPrimaryFaxNumberOrdinal(int ordinal) {
        return typeAPI.getPrimaryFaxNumberOrdinal(ordinal);
    }

    public int getAssistantTelephoneNumberOrdinal(int ordinal) {
        return typeAPI.getAssistantTelephoneNumberOrdinal(ordinal);
    }

    public int getUserCertificateOrdinal(int ordinal) {
        return typeAPI.getUserCertificateOrdinal(ordinal);
    }

    public int getAddressBookX509CertificateOrdinal(int ordinal) {
        return typeAPI.getAddressBookX509CertificateOrdinal(ordinal);
    }

    public int getUserX509CertificateOrdinal(int ordinal) {
        return typeAPI.getUserX509CertificateOrdinal(ordinal);
    }

    public byte[] getThumbnail(int ordinal) {
        return typeAPI.getThumbnail(ordinal);
    }

    public boolean getHidden(int ordinal) {
        return typeAPI.getHidden(ordinal);
    }

    public Boolean getHiddenBoxed(int ordinal) {
        return typeAPI.getHiddenBoxed(ordinal);
    }

    public int getAnrOrdinal(int ordinal) {
        return typeAPI.getAnrOrdinal(ordinal);
    }

    public AddressBookRecordTypeAPI getTypeAPI() {
        return typeAPI;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

}