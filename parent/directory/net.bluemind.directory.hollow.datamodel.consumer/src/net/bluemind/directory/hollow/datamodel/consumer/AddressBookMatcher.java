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
package net.bluemind.directory.hollow.datamodel.consumer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class AddressBookMatcher {

	private AddressBookMatcher() {
	}

	public static boolean matches(String key, String value, AddressBookRecord record) {
		value = value.toLowerCase();
		switch (key) {
		case "name":
			return value(record.getName()).contains(value);
		case "office":
			String locality = value(record.getLocality());
			String street = value(record.getStreetAddress());
			String poBox = value(record.getPostOfficeBox());
			String postalCode = value(record.getPostalCode());
			String country = value(record.getCountry());
			return locality.contains(value) || street.contains(value) || poBox.contains(value)
					|| postalCode.contains(value) || country.contains(value);
		case "department":
			return value(record.getDepartmentName()).contains(value);
		case "company":
			return value(record.getCompanyName()).contains(value);
		case "kind":
			return value(record.getKind()).contains(value);
		case "title":
			return value(record.getTitle()).contains(value);
		case "surname":
			return value(record.getSurname()).contains(value);
		case "givenName":
			return value(record.getGivenName()).contains(value);
		case "anr":
			String email = value(record.getEmail());
			String name = value(record.getName());
			return email.contains(value) || name.contains(value) || emailsMatch(value, record);
		default:
			return false;
		}

	}

	private static boolean emailsMatch(String value, AddressBookRecord record) {
		Set<String> domainNames = new HashSet<>();
		domainNames.add(record.getAddressBook().getDomainName().getValue());
		for (Iterator<HString> iter = record.getAddressBook().getDomainAliases().iterator(); iter.hasNext();) {
			domainNames.add(iter.next().getValue());
		}
		for (Iterator<Email> iter = record.getEmails().iterator(); iter.hasNext();) {
			Email email = iter.next();
			String emailAddress = value(email.getAddress());
			Set<String> addresses = new HashSet<>();
			if (!email.getAllAliases()) {
				addresses.add(emailAddress);
			} else {
				addresses.addAll(domainNames.stream().map(alias -> emailAddress.split("@")[0] + "@" + alias)
						.collect(Collectors.toSet()));
			}
			if (addresses.stream().anyMatch(addr -> addr.contains(value))) {
				return true;
			}
		}
		return false;
	}

	private static String value(HString string) {
		if (string == null) {
			return "";
		}
		return string.getValue().toLowerCase();
	}

}
