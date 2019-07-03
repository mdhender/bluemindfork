package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;

@SuppressWarnings("all")
public class AddressBookRecordTypeAPI extends HollowObjectTypeAPI {

    private final AddressBookRecordDelegateLookupImpl delegateLookupImpl;

    public AddressBookRecordTypeAPI(OfflineDirectoryAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "addressBook",
            "uid",
            "distinguishedName",
            "domain",
            "kind",
            "emails",
            "created",
            "updated",
            "email",
            "minimalid",
            "name",
            "surname",
            "givenName",
            "title",
            "officeLocation",
            "departmentName",
            "companyName",
            "assistant",
            "addressBookManagerDistinguishedName",
            "addressBookPhoneticGivenName",
            "addressBookPhoneticSurname",
            "addressBookPhoneticCompanyName",
            "addressBookPhoneticDepartmentName",
            "streetAddress",
            "postOfficeBox",
            "locality",
            "stateOrProvince",
            "postalCode",
            "country",
            "dataLocation",
            "businessTelephoneNumber",
            "homeTelephoneNumber",
            "business2TelephoneNumbers",
            "home2TelephoneNumber",
            "mobileTelephoneNumber",
            "pagerTelephoneNumber",
            "primaryFaxNumber",
            "assistantTelephoneNumber",
            "userCertificate",
            "addressBookX509Certificate",
            "userX509Certificate",
            "thumbnail",
            "hidden"
        });
        this.delegateLookupImpl = new AddressBookRecordDelegateLookupImpl(this);
    }

    public int getAddressBookOrdinal(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "addressBook");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[0]);
    }

    public OfflineAddressBookTypeAPI getAddressBookTypeAPI() {
        return getAPI().getOfflineAddressBookTypeAPI();
    }

    public int getUidOrdinal(int ordinal) {
        if(fieldIndex[1] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "uid");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[1]);
    }

    public StringTypeAPI getUidTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getDistinguishedNameOrdinal(int ordinal) {
        if(fieldIndex[2] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "distinguishedName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[2]);
    }

    public StringTypeAPI getDistinguishedNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getDomainOrdinal(int ordinal) {
        if(fieldIndex[3] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "domain");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[3]);
    }

    public StringTypeAPI getDomainTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getKindOrdinal(int ordinal) {
        if(fieldIndex[4] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "kind");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[4]);
    }

    public StringTypeAPI getKindTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getEmailsOrdinal(int ordinal) {
        if(fieldIndex[5] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "emails");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[5]);
    }

    public ListOfEmailTypeAPI getEmailsTypeAPI() {
        return getAPI().getListOfEmailTypeAPI();
    }

    public int getCreatedOrdinal(int ordinal) {
        if(fieldIndex[6] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "created");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[6]);
    }

    public DateTypeAPI getCreatedTypeAPI() {
        return getAPI().getDateTypeAPI();
    }

    public int getUpdatedOrdinal(int ordinal) {
        if(fieldIndex[7] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "updated");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[7]);
    }

    public DateTypeAPI getUpdatedTypeAPI() {
        return getAPI().getDateTypeAPI();
    }

    public int getEmailOrdinal(int ordinal) {
        if(fieldIndex[8] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "email");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[8]);
    }

    public StringTypeAPI getEmailTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public long getMinimalid(int ordinal) {
        if(fieldIndex[9] == -1)
            return missingDataHandler().handleLong("AddressBookRecord", ordinal, "minimalid");
        return getTypeDataAccess().readLong(ordinal, fieldIndex[9]);
    }

    public Long getMinimalidBoxed(int ordinal) {
        long l;
        if(fieldIndex[9] == -1) {
            l = missingDataHandler().handleLong("AddressBookRecord", ordinal, "minimalid");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[9]);
            l = getTypeDataAccess().readLong(ordinal, fieldIndex[9]);
        }
        if(l == Long.MIN_VALUE)
            return null;
        return Long.valueOf(l);
    }



    public int getNameOrdinal(int ordinal) {
        if(fieldIndex[10] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "name");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[10]);
    }

    public StringTypeAPI getNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getSurnameOrdinal(int ordinal) {
        if(fieldIndex[11] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "surname");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[11]);
    }

    public StringTypeAPI getSurnameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getGivenNameOrdinal(int ordinal) {
        if(fieldIndex[12] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "givenName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[12]);
    }

    public StringTypeAPI getGivenNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getTitleOrdinal(int ordinal) {
        if(fieldIndex[13] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "title");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[13]);
    }

    public StringTypeAPI getTitleTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getOfficeLocationOrdinal(int ordinal) {
        if(fieldIndex[14] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "officeLocation");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[14]);
    }

    public StringTypeAPI getOfficeLocationTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getDepartmentNameOrdinal(int ordinal) {
        if(fieldIndex[15] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "departmentName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[15]);
    }

    public StringTypeAPI getDepartmentNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getCompanyNameOrdinal(int ordinal) {
        if(fieldIndex[16] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "companyName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[16]);
    }

    public StringTypeAPI getCompanyNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAssistantOrdinal(int ordinal) {
        if(fieldIndex[17] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "assistant");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[17]);
    }

    public StringTypeAPI getAssistantTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAddressBookManagerDistinguishedNameOrdinal(int ordinal) {
        if(fieldIndex[18] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "addressBookManagerDistinguishedName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[18]);
    }

    public StringTypeAPI getAddressBookManagerDistinguishedNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAddressBookPhoneticGivenNameOrdinal(int ordinal) {
        if(fieldIndex[19] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "addressBookPhoneticGivenName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[19]);
    }

    public StringTypeAPI getAddressBookPhoneticGivenNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAddressBookPhoneticSurnameOrdinal(int ordinal) {
        if(fieldIndex[20] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "addressBookPhoneticSurname");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[20]);
    }

    public StringTypeAPI getAddressBookPhoneticSurnameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAddressBookPhoneticCompanyNameOrdinal(int ordinal) {
        if(fieldIndex[21] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "addressBookPhoneticCompanyName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[21]);
    }

    public StringTypeAPI getAddressBookPhoneticCompanyNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAddressBookPhoneticDepartmentNameOrdinal(int ordinal) {
        if(fieldIndex[22] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "addressBookPhoneticDepartmentName");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[22]);
    }

    public StringTypeAPI getAddressBookPhoneticDepartmentNameTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getStreetAddressOrdinal(int ordinal) {
        if(fieldIndex[23] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "streetAddress");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[23]);
    }

    public StringTypeAPI getStreetAddressTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getPostOfficeBoxOrdinal(int ordinal) {
        if(fieldIndex[24] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "postOfficeBox");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[24]);
    }

    public StringTypeAPI getPostOfficeBoxTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getLocalityOrdinal(int ordinal) {
        if(fieldIndex[25] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "locality");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[25]);
    }

    public StringTypeAPI getLocalityTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getStateOrProvinceOrdinal(int ordinal) {
        if(fieldIndex[26] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "stateOrProvince");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[26]);
    }

    public StringTypeAPI getStateOrProvinceTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getPostalCodeOrdinal(int ordinal) {
        if(fieldIndex[27] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "postalCode");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[27]);
    }

    public StringTypeAPI getPostalCodeTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getCountryOrdinal(int ordinal) {
        if(fieldIndex[28] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "country");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[28]);
    }

    public StringTypeAPI getCountryTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getDataLocationOrdinal(int ordinal) {
        if(fieldIndex[29] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "dataLocation");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[29]);
    }

    public DataLocationTypeAPI getDataLocationTypeAPI() {
        return getAPI().getDataLocationTypeAPI();
    }

    public int getBusinessTelephoneNumberOrdinal(int ordinal) {
        if(fieldIndex[30] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "businessTelephoneNumber");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[30]);
    }

    public StringTypeAPI getBusinessTelephoneNumberTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getHomeTelephoneNumberOrdinal(int ordinal) {
        if(fieldIndex[31] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "homeTelephoneNumber");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[31]);
    }

    public StringTypeAPI getHomeTelephoneNumberTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getBusiness2TelephoneNumbersOrdinal(int ordinal) {
        if(fieldIndex[32] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "business2TelephoneNumbers");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[32]);
    }

    public StringTypeAPI getBusiness2TelephoneNumbersTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getHome2TelephoneNumberOrdinal(int ordinal) {
        if(fieldIndex[33] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "home2TelephoneNumber");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[33]);
    }

    public StringTypeAPI getHome2TelephoneNumberTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getMobileTelephoneNumberOrdinal(int ordinal) {
        if(fieldIndex[34] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "mobileTelephoneNumber");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[34]);
    }

    public StringTypeAPI getMobileTelephoneNumberTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getPagerTelephoneNumberOrdinal(int ordinal) {
        if(fieldIndex[35] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "pagerTelephoneNumber");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[35]);
    }

    public StringTypeAPI getPagerTelephoneNumberTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getPrimaryFaxNumberOrdinal(int ordinal) {
        if(fieldIndex[36] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "primaryFaxNumber");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[36]);
    }

    public StringTypeAPI getPrimaryFaxNumberTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAssistantTelephoneNumberOrdinal(int ordinal) {
        if(fieldIndex[37] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "assistantTelephoneNumber");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[37]);
    }

    public StringTypeAPI getAssistantTelephoneNumberTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getUserCertificateOrdinal(int ordinal) {
        if(fieldIndex[38] == -1)
            return missingDataHandler().handleReferencedOrdinal("AddressBookRecord", ordinal, "userCertificate");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[38]);
    }

    public StringTypeAPI getUserCertificateTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public byte[] getAddressBookX509Certificate(int ordinal) {
        if(fieldIndex[39] == -1)
            return missingDataHandler().handleBytes("AddressBookRecord", ordinal, "addressBookX509Certificate");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[39]);
        return getTypeDataAccess().readBytes(ordinal, fieldIndex[39]);
    }



    public byte[] getUserX509Certificate(int ordinal) {
        if(fieldIndex[40] == -1)
            return missingDataHandler().handleBytes("AddressBookRecord", ordinal, "userX509Certificate");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[40]);
        return getTypeDataAccess().readBytes(ordinal, fieldIndex[40]);
    }



    public byte[] getThumbnail(int ordinal) {
        if(fieldIndex[41] == -1)
            return missingDataHandler().handleBytes("AddressBookRecord", ordinal, "thumbnail");
        boxedFieldAccessSampler.recordFieldAccess(fieldIndex[41]);
        return getTypeDataAccess().readBytes(ordinal, fieldIndex[41]);
    }



    public boolean getHidden(int ordinal) {
        if(fieldIndex[42] == -1)
            return Boolean.TRUE.equals(missingDataHandler().handleBoolean("AddressBookRecord", ordinal, "hidden"));
        return Boolean.TRUE.equals(getTypeDataAccess().readBoolean(ordinal, fieldIndex[42]));
    }

    public Boolean getHiddenBoxed(int ordinal) {
        if(fieldIndex[42] == -1)
            return missingDataHandler().handleBoolean("AddressBookRecord", ordinal, "hidden");
        return getTypeDataAccess().readBoolean(ordinal, fieldIndex[42]);
    }



    public AddressBookRecordDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public OfflineDirectoryAPI getAPI() {
        return (OfflineDirectoryAPI) api;
    }

}