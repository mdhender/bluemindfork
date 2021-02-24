/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.directory.hollow.datamodel;

import java.util.Date;
import java.util.List;

import com.netflix.hollow.core.write.objectmapper.HollowInline;
import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey;

@HollowPrimaryKey(fields = { "uid" })
public class AddressBookRecord {

	/**
	 * entryUid
	 */
	@HollowInline
	public String uid;

	/**
	 * PidTagEmailAddress (X500 DN)
	 */
	@HollowInline
	public String distinguishedName;

	/**
	 * domain
	 */
	public String domain;
	/**
	 * kind
	 */
	public String kind;
	/**
	 * emails
	 */
	public List<Email> emails;
	/**
	 * created
	 */
	public Date created;
	/**
	 * updated
	 */
	public Date updated;
	/**
	 * email
	 */
	@HollowInline
	public String email;
	/**
	 * internal-id
	 */
	public long minimalid;

	/**
	 * displayName
	 */
	@HollowInline
	public String name;

	/**
	 * PidTagSurname
	 */
	@HollowInline
	public String surname;
	/**
	 * PidTagGivenName
	 */
	@HollowInline
	public String givenName;
	/**
	 * PidTagTitle
	 */
	public String title;
	/**
	 * PidTagOfficeLocation
	 */
	public String officeLocation;
	/**
	 * PidTagDepartmentName
	 */
	public String departmentName;
	/**
	 * PidTagCompanyName
	 */
	public String companyName;
	/**
	 * PidTagAssistant
	 */
	public String assistant;
	public String addressBookManagerDistinguishedName;
	/**
	 * PidTagAddressBookPhoneticGivenName
	 */
	public String addressBookPhoneticGivenName;
	/**
	 * PidTagAddressBookPhoneticSurname
	 */
	public String addressBookPhoneticSurname;
	public String addressBookPhoneticCompanyName;
	public String addressBookPhoneticDepartmentName;
	/**
	 * PidTagStreetAddress
	 */
	public String streetAddress;
	public String postOfficeBox;
	/**
	 * PidTagLocality
	 */
	public String locality;
	/**
	 * PidTagStateOrProvince
	 */
	public String stateOrProvince;
	/**
	 * PidTagPostalcode
	 */
	public String postalCode;
	/**
	 * PidTagCountry
	 */
	public String country;
	/**
	 * Datalocation
	 */
	public DataLocation dataLocation;
	/**
	 * PidTagBusinessTelephoneNumber
	 */
	public String businessTelephoneNumber;
	/**
	 * PidTagHomeTelephoneNumber
	 */
	public String homeTelephoneNumber;
	/**
	 * PidTagBusiness2TelephoneNumbers
	 */
	public String business2TelephoneNumbers;
	/**
	 * PidTagBusiness2TelephoneNumbers
	 */
	public String home2TelephoneNumber;
	/**
	 * PidTagMobileTelephoneNumber
	 */
	public String mobileTelephoneNumber;
	/**
	 * PidTagPagerTelephoneNumber
	 */
	public String pagerTelephoneNumber;
	/**
	 * PidTagPrimaryFaxNumber
	 */
	public String primaryFaxNumber;
	/**
	 * PidTagAssistantTelephoneNumber
	 */
	public String assistantTelephoneNumber;
	/**
	 * PidTagUserCertificate
	 */
	public String userCertificate;
	/**
	 * PidTagAddressBookX509Certificate
	 */
	public byte[] addressBookX509Certificate;
	/**
	 * PidTagUserX509Certificate
	 */
	public byte[] userX509Certificate;
	/**
	 * PidTagThumbnailPhoto
	 */
	public byte[] thumbnail;

	public boolean hidden;

	public List<AnrToken> anr;
}
