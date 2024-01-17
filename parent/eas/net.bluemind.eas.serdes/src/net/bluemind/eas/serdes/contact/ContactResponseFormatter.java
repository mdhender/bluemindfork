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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.contact.ContactResponse;
import net.bluemind.eas.serdes.IEasFragmentFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class ContactResponseFormatter implements IEasFragmentFormatter<ContactResponse> {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd'T'HH':'mm':'ss.SSS'Z'");

	@Override
	public void append(IResponseBuilder b, double protocolVersion, ContactResponse contact,
			Callback<IResponseBuilder> cb) {

		if (contact.anniversary != null) {
			b.text(NamespaceMapping.CONTACTS, "Anniversary", formatDate(contact.anniversary));
		}

		if (notEmpty(contact.assistantName)) {
			b.text(NamespaceMapping.CONTACTS, "AssistantName", contact.assistantName);
		}

		// AssistantPhoneNumber

		if (contact.birthday != null) {
			b.text(NamespaceMapping.CONTACTS, "Birthday", formatDate(contact.birthday));
		}

		if (notEmpty(contact.business2PhoneNumber)) {
			b.text(NamespaceMapping.CONTACTS, "Business2PhoneNumber", contact.business2PhoneNumber);
		}

		if (notEmpty(contact.businessAddressCity)) {
			b.text(NamespaceMapping.CONTACTS, "BusinessAddressCity", contact.businessAddressCity);
		}

		if (notEmpty(contact.businessPhoneNumber)) {
			b.text(NamespaceMapping.CONTACTS, "BusinessPhoneNumber", contact.businessPhoneNumber);
		}

		if (notEmpty(contact.webPage)) {
			b.text(NamespaceMapping.CONTACTS, "WebPage", contact.webPage);
		}

		if (notEmpty(contact.businessAddressCountry)) {
			b.text(NamespaceMapping.CONTACTS, "BusinessAddressCountry", contact.businessAddressCountry);
		}

		if (notEmpty(contact.department)) {
			b.text(NamespaceMapping.CONTACTS, "Department", contact.department);
		}

		if (notEmpty(contact.email1Address)) {
			b.text(NamespaceMapping.CONTACTS, "Email1Address", contact.email1Address);
		}

		if (notEmpty(contact.email2Address)) {
			b.text(NamespaceMapping.CONTACTS, "Email2Address", contact.email2Address);
		}

		if (notEmpty(contact.email3Address)) {
			b.text(NamespaceMapping.CONTACTS, "Email3Address", contact.email3Address);
		}

		if (notEmpty(contact.businessFaxNumber)) {
			b.text(NamespaceMapping.CONTACTS, "BusinessFaxNumber", contact.businessFaxNumber);
		}

		// FileAs

		// Alias

		// WeightedRank

		if (notEmpty(contact.firstName)) {
			b.text(NamespaceMapping.CONTACTS, "FirstName", contact.firstName);
		}

		if (notEmpty(contact.middleName)) {
			b.text(NamespaceMapping.CONTACTS, "MiddleName", contact.middleName);
		}

		if (notEmpty(contact.homeAddressCity)) {
			b.text(NamespaceMapping.CONTACTS, "HomeAddressCity", contact.homeAddressCity);
		}

		if (notEmpty(contact.homeAddressCountry)) {
			b.text(NamespaceMapping.CONTACTS, "HomeAddressCountry", contact.homeAddressCountry);
		}

		if (notEmpty(contact.homeFaxNumber)) {
			b.text(NamespaceMapping.CONTACTS, "HomeFaxNumber", contact.homeFaxNumber);
		}

		if (notEmpty(contact.homePhoneNumber)) {
			b.text(NamespaceMapping.CONTACTS, "HomePhoneNumber", contact.homePhoneNumber);
		}

		if (notEmpty(contact.home2PhoneNumber)) {
			b.text(NamespaceMapping.CONTACTS, "Home2PhoneNumber", contact.home2PhoneNumber);
		}

		if (notEmpty(contact.homeAddressPostalCode)) {
			b.text(NamespaceMapping.CONTACTS, "HomeAddressPostalCode", contact.homeAddressPostalCode);
		}

		if (notEmpty(contact.homeAddressState)) {
			b.text(NamespaceMapping.CONTACTS, "HomeAddressState", contact.homeAddressState);
		}

		if (notEmpty(contact.homeAddressStreet)) {
			b.text(NamespaceMapping.CONTACTS, "HomeAddressStreet", contact.homeAddressStreet);
		}

		if (notEmpty(contact.mobilePhoneNumber)) {
			b.text(NamespaceMapping.CONTACTS, "MobilePhoneNumber", contact.mobilePhoneNumber);
		}

		if (notEmpty(contact.suffix)) {
			b.text(NamespaceMapping.CONTACTS, "Suffix", contact.suffix);
		}

		if (notEmpty(contact.companyName)) {
			b.text(NamespaceMapping.CONTACTS, "CompanyName", contact.companyName);
		}

		if (notEmpty(contact.otherAddressCity)) {
			b.text(NamespaceMapping.CONTACTS, "OtherAddressCity", contact.otherAddressCity);
		}

		if (notEmpty(contact.otherAddressCountry)) {
			b.text(NamespaceMapping.CONTACTS, "OtherAddressCountry", contact.otherAddressCountry);
		}

		// CarPhoneNumber

		if (notEmpty(contact.otherAddressPostalCode)) {
			b.text(NamespaceMapping.CONTACTS, "OtherAddressPostalCode", contact.otherAddressPostalCode);
		}

		if (notEmpty(contact.otherAddressState)) {
			b.text(NamespaceMapping.CONTACTS, "OtherAddressState", contact.otherAddressState);
		}

		if (notEmpty(contact.otherAddressStreet)) {
			b.text(NamespaceMapping.CONTACTS, "OtherAddressStreet", contact.otherAddressStreet);
		}

		if (notEmpty(contact.pagerNumber)) {
			b.text(NamespaceMapping.CONTACTS, "PagerNumber", contact.pagerNumber);
		}

		if (notEmpty(contact.title)) {
			b.text(NamespaceMapping.CONTACTS, "Title", contact.title);
		}
		if (notEmpty(contact.businessAddressPostalCode)) {
			b.text(NamespaceMapping.CONTACTS, "BusinessAddressPostalCode", contact.businessAddressPostalCode);
		}

		if (notEmpty(contact.lastName)) {
			b.text(NamespaceMapping.CONTACTS, "LastName", contact.lastName);
		}
		if (notEmpty(contact.spouse)) {
			b.text(NamespaceMapping.CONTACTS, "Spouse", contact.spouse);
		}

		if (notEmpty(contact.businessAddressState)) {
			b.text(NamespaceMapping.CONTACTS, "BusinessAddressState", contact.businessAddressState);
		}

		if (notEmpty(contact.businessAddressStreet)) {
			b.text(NamespaceMapping.CONTACTS, "BusinessAddressStreet", contact.businessAddressStreet);
		}

		if (notEmpty(contact.jobTitle)) {
			b.text(NamespaceMapping.CONTACTS, "JobTitle", contact.jobTitle);
		}

		// YomiFirstName

		// YomiLastName

		// YomiCompanyName

		// OfficeLocation

		// RadioPhoneNumber

		if (notEmpty(contact.picture)) {
			b.text(NamespaceMapping.CONTACTS, "Picture", contact.picture);
		}

		if (contact.categories != null && !contact.categories.isEmpty()) {
			b.container(NamespaceMapping.CONTACTS, "Categories");
			for (String cat : contact.categories) {
				b.text("Category", cat);// meow
			}
			b.endContainer();
		}

		// Contacts2:CustomerId

		// Contacts2:GovernmentId

		if (notEmpty(contact.imAddress)) {
			b.text(NamespaceMapping.CONTACTS_2, "IMAddress", contact.imAddress);
		}
		if (notEmpty(contact.imAddress2)) {
			b.text(NamespaceMapping.CONTACTS_2, "IMAddress2", contact.imAddress2);
		}
		if (notEmpty(contact.imAddress3)) {
			b.text(NamespaceMapping.CONTACTS_2, "IMAddress3", contact.imAddress3);
		}

		if (notEmpty(contact.managerName)) {
			b.text(NamespaceMapping.CONTACTS_2, "ManagerName", contact.managerName);
		}

		// Contacts2:CompanyMainPhone

		// Contacts2:AccountName

		if (notEmpty(contact.nickName)) {
			b.text(NamespaceMapping.CONTACTS_2, "NickName", contact.nickName);
		}

		// Contacts2:MMS

		cb.onResult(b);
	}

	private String formatDate(Date date) {
		Instant instant = Instant.ofEpochMilli(date.getTime());
		LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
		return dtf.format(localDateTime);
	}

	private boolean notEmpty(String s) {
		return s != null && !s.trim().isEmpty();
	}

}
