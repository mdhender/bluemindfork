package net.bluemind.directory.hollow.datamodel.consumer;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface AddressBookRecordDelegate extends HollowObjectDelegate {

    public int getAddressBookOrdinal(int ordinal);

    public int getUidOrdinal(int ordinal);

    public int getDistinguishedNameOrdinal(int ordinal);

    public int getDomainOrdinal(int ordinal);

    public int getKindOrdinal(int ordinal);

    public int getEmailsOrdinal(int ordinal);

    public int getCreatedOrdinal(int ordinal);

    public int getUpdatedOrdinal(int ordinal);

    public int getEmailOrdinal(int ordinal);

    public long getMinimalid(int ordinal);

    public Long getMinimalidBoxed(int ordinal);

    public int getNameOrdinal(int ordinal);

    public int getSurnameOrdinal(int ordinal);

    public int getGivenNameOrdinal(int ordinal);

    public int getTitleOrdinal(int ordinal);

    public int getOfficeLocationOrdinal(int ordinal);

    public int getDepartmentNameOrdinal(int ordinal);

    public int getCompanyNameOrdinal(int ordinal);

    public int getAssistantOrdinal(int ordinal);

    public int getAddressBookManagerDistinguishedNameOrdinal(int ordinal);

    public int getAddressBookPhoneticGivenNameOrdinal(int ordinal);

    public int getAddressBookPhoneticSurnameOrdinal(int ordinal);

    public int getAddressBookPhoneticCompanyNameOrdinal(int ordinal);

    public int getAddressBookPhoneticDepartmentNameOrdinal(int ordinal);

    public int getStreetAddressOrdinal(int ordinal);

    public int getPostOfficeBoxOrdinal(int ordinal);

    public int getLocalityOrdinal(int ordinal);

    public int getStateOrProvinceOrdinal(int ordinal);

    public int getPostalCodeOrdinal(int ordinal);

    public int getCountryOrdinal(int ordinal);

    public int getDataLocationOrdinal(int ordinal);

    public int getBusinessTelephoneNumberOrdinal(int ordinal);

    public int getHomeTelephoneNumberOrdinal(int ordinal);

    public int getBusiness2TelephoneNumbersOrdinal(int ordinal);

    public int getHome2TelephoneNumberOrdinal(int ordinal);

    public int getMobileTelephoneNumberOrdinal(int ordinal);

    public int getPagerTelephoneNumberOrdinal(int ordinal);

    public int getPrimaryFaxNumberOrdinal(int ordinal);

    public int getAssistantTelephoneNumberOrdinal(int ordinal);

    public int getUserCertificateOrdinal(int ordinal);

    public byte[] getAddressBookX509Certificate(int ordinal);

    public byte[] getUserX509Certificate(int ordinal);

    public byte[] getThumbnail(int ordinal);

    public boolean getHidden(int ordinal);

    public Boolean getHiddenBoxed(int ordinal);

    public AddressBookRecordTypeAPI getTypeAPI();

}