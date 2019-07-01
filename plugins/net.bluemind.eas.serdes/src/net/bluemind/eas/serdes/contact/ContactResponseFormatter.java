/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.eas.serdes.contact;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.contact.ContactResponse;
import net.bluemind.eas.serdes.FastDateFormat;
import net.bluemind.eas.serdes.IEasFragmentFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class ContactResponseFormatter implements IEasFragmentFormatter<ContactResponse> {

	@Override
	public void append(IResponseBuilder b, double protocolVersion, ContactResponse contact,
			Callback<IResponseBuilder> cb) {

		if (contact.anniversary != null) {
			b.text(NamespaceMapping.Contacts, "Anniversary", FastDateFormat.format(contact.anniversary));
		}

		if (notEmpty(contact.assistantName)) {
			b.text(NamespaceMapping.Contacts, "AssistantName", contact.assistantName);
		}

		// AssistantPhoneNumber

		if (contact.birthday != null) {
			b.text(NamespaceMapping.Contacts, "Birthday", FastDateFormat.format(contact.birthday));
		}

		if (notEmpty(contact.business2PhoneNumber)) {
			b.text(NamespaceMapping.Contacts, "Business2PhoneNumber", contact.business2PhoneNumber);
		}

		if (notEmpty(contact.businessAddressCity)) {
			b.text(NamespaceMapping.Contacts, "BusinessAddressCity", contact.businessAddressCity);
		}

		if (notEmpty(contact.businessPhoneNumber)) {
			b.text(NamespaceMapping.Contacts, "BusinessPhoneNumber", contact.businessPhoneNumber);
		}

		if (notEmpty(contact.webPage)) {
			b.text(NamespaceMapping.Contacts, "WebPage", contact.webPage);
		}

		if (notEmpty(contact.businessAddressCountry)) {
			b.text(NamespaceMapping.Contacts, "BusinessAddressCountry", contact.businessAddressCountry);
		}

		if (notEmpty(contact.department)) {
			b.text(NamespaceMapping.Contacts, "Department", contact.department);
		}

		if (notEmpty(contact.email1Address)) {
			b.text(NamespaceMapping.Contacts, "Email1Address", contact.email1Address);
		}

		if (notEmpty(contact.email2Address)) {
			b.text(NamespaceMapping.Contacts, "Email2Address", contact.email2Address);
		}

		if (notEmpty(contact.email3Address)) {
			b.text(NamespaceMapping.Contacts, "Email3Address", contact.email3Address);
		}

		if (notEmpty(contact.businessFaxNumber)) {
			b.text(NamespaceMapping.Contacts, "BusinessFaxNumber", contact.businessFaxNumber);
		}

		// FileAs

		// Alias

		// WeightedRank

		if (notEmpty(contact.firstName)) {
			b.text(NamespaceMapping.Contacts, "FirstName", contact.firstName);
		}

		if (notEmpty(contact.middleName)) {
			b.text(NamespaceMapping.Contacts, "MiddleName", contact.middleName);
		}

		if (notEmpty(contact.homeAddressCity)) {
			b.text(NamespaceMapping.Contacts, "HomeAddressCity", contact.homeAddressCity);
		}

		if (notEmpty(contact.homeAddressCountry)) {
			b.text(NamespaceMapping.Contacts, "HomeAddressCountry", contact.homeAddressCountry);
		}

		if (notEmpty(contact.homeFaxNumber)) {
			b.text(NamespaceMapping.Contacts, "HomeFaxNumber", contact.homeFaxNumber);
		}

		if (notEmpty(contact.homePhoneNumber)) {
			b.text(NamespaceMapping.Contacts, "HomePhoneNumber", contact.homePhoneNumber);
		}

		if (notEmpty(contact.home2PhoneNumber)) {
			b.text(NamespaceMapping.Contacts, "Home2PhoneNumber", contact.home2PhoneNumber);
		}

		if (notEmpty(contact.homeAddressPostalCode)) {
			b.text(NamespaceMapping.Contacts, "HomeAddressPostalCode", contact.homeAddressPostalCode);
		}

		if (notEmpty(contact.homeAddressState)) {
			b.text(NamespaceMapping.Contacts, "HomeAddressState", contact.homeAddressState);
		}

		if (notEmpty(contact.homeAddressStreet)) {
			b.text(NamespaceMapping.Contacts, "HomeAddressStreet", contact.homeAddressStreet);
		}

		if (notEmpty(contact.mobilePhoneNumber)) {
			b.text(NamespaceMapping.Contacts, "MobilePhoneNumber", contact.mobilePhoneNumber);
		}

		if (notEmpty(contact.suffix)) {
			b.text(NamespaceMapping.Contacts, "Suffix", contact.suffix);
		}

		if (notEmpty(contact.companyName)) {
			b.text(NamespaceMapping.Contacts, "CompanyName", contact.companyName);
		}

		if (notEmpty(contact.otherAddressCity)) {
			b.text(NamespaceMapping.Contacts, "OtherAddressCity", contact.otherAddressCity);
		}

		if (notEmpty(contact.otherAddressCountry)) {
			b.text(NamespaceMapping.Contacts, "OtherAddressCountry", contact.otherAddressCountry);
		}

		// CarPhoneNumber

		if (notEmpty(contact.otherAddressPostalCode)) {
			b.text(NamespaceMapping.Contacts, "OtherAddressPostalCode", contact.otherAddressPostalCode);
		}

		if (notEmpty(contact.otherAddressState)) {
			b.text(NamespaceMapping.Contacts, "OtherAddressState", contact.otherAddressState);
		}

		if (notEmpty(contact.otherAddressStreet)) {
			b.text(NamespaceMapping.Contacts, "OtherAddressStreet", contact.otherAddressStreet);
		}

		if (notEmpty(contact.pagerNumber)) {
			b.text(NamespaceMapping.Contacts, "PagerNumber", contact.pagerNumber);
		}

		if (notEmpty(contact.title)) {
			b.text(NamespaceMapping.Contacts, "Title", contact.title);
		}
		if (notEmpty(contact.businessAddressPostalCode)) {
			b.text(NamespaceMapping.Contacts, "BusinessAddressPostalCode", contact.businessAddressPostalCode);
		}

		if (notEmpty(contact.lastName)) {
			b.text(NamespaceMapping.Contacts, "LastName", contact.lastName);
		}
		if (notEmpty(contact.spouse)) {
			b.text(NamespaceMapping.Contacts, "Spouse", contact.spouse);
		}

		if (notEmpty(contact.businessAddressState)) {
			b.text(NamespaceMapping.Contacts, "BusinessAddressState", contact.businessAddressState);
		}

		if (notEmpty(contact.businessAddressStreet)) {
			b.text(NamespaceMapping.Contacts, "BusinessAddressStreet", contact.businessAddressStreet);
		}

		if (notEmpty(contact.jobTitle)) {
			b.text(NamespaceMapping.Contacts, "JobTitle", contact.jobTitle);
		}

		// YomiFirstName

		// YomiLastName

		// YomiCompanyName

		// OfficeLocation

		// RadioPhoneNumber

		if (notEmpty(contact.picture)) {
			b.text(NamespaceMapping.Contacts, "Picture", contact.picture);
		}

		if (contact.categories != null && !contact.categories.isEmpty()) {
			b.container(NamespaceMapping.Contacts, "Categories");
			for (String cat : contact.categories) {
				b.text("Category", cat);// meow
			}
			b.endContainer();
		}

		// Contacts2:CustomerId

		// Contacts2:GovernmentId

		if (notEmpty(contact.imAddress)) {
			b.text(NamespaceMapping.Contacts2, "IMAddress", contact.imAddress);
		}
		if (notEmpty(contact.imAddress2)) {
			b.text(NamespaceMapping.Contacts2, "IMAddress2", contact.imAddress2);
		}
		if (notEmpty(contact.imAddress3)) {
			b.text(NamespaceMapping.Contacts2, "IMAddress3", contact.imAddress3);
		}

		if (notEmpty(contact.managerName)) {
			b.text(NamespaceMapping.Contacts2, "ManagerName", contact.managerName);
		}

		// Contacts2:CompanyMainPhone

		// Contacts2:AccountName

		if (notEmpty(contact.nickName)) {
			b.text(NamespaceMapping.Contacts2, "NickName", contact.nickName);
		}

		// Contacts2:MMS

		cb.onResult(b);
	}

	private boolean notEmpty(String s) {
		return s != null && !s.trim().isEmpty();
	}

}
