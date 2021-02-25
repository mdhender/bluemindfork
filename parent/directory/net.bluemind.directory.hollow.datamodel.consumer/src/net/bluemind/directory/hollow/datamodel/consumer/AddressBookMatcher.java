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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class AddressBookMatcher {

	private AddressBookMatcher() {
	}

	public static boolean matches(String key, String v, Optional<OfflineAddressBook> book, AddressBookRecord record) {
		String value = v.toLowerCase();
		switch (key) {
		case "uid":
			return record.getUid().equals(value);
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
			return email.startsWith(value) || name.startsWith(value) || emailsMatch(value, book, record);
		default:
			return false;
		}

	}

	private static final Splitter EMAIL_CHUNKS = Splitter.on(CharMatcher.anyOf(".-@"));
	private static final Splitter EMAIL_AT = Splitter.on('@');

	private static boolean emailsMatch(String value, Optional<OfflineAddressBook> optBook, AddressBookRecord record) {
		return optBook.map(book -> {
			Set<String> domainNames = new HashSet<>();
			domainNames.add(book.getDomainName().getValue());
			for (Iterator<HString> iter = book.getDomainAliases().iterator(); iter.hasNext();) {
				domainNames.add(iter.next().getValue());
			}
			for (Iterator<Email> iter = record.getEmails().iterator(); iter.hasNext();) {
				Email email = iter.next();
				String emailAddress = value(email.getAddress());
				Set<String> addresses = new HashSet<>();
				if (!email.getAllAliases()) {
					addresses.add(emailAddress);
				} else {
					addresses.addAll(domainNames.stream().map(alias -> localPart(emailAddress) + "@" + alias)
							.collect(Collectors.toSet()));
				}
				if (addresses.stream().flatMap(AddressBookMatcher::emailChunks)
						.anyMatch(chunk -> chunk.startsWith(value))) {
					return true;
				}
			}
			return false;
		}).orElse(false);

	}

	private static String localPart(String email) {
		return EMAIL_AT.split(email).iterator().next();
	}

	/**
	 * Turns john.bang@groupe-charal.com into [ john bang groupe charal com ]
	 * 
	 * @param addr
	 * @return
	 */
	private static Stream<String> emailChunks(String addr) {
		return EMAIL_CHUNKS.splitToList(addr).stream();
	}

	private static String value(String string) {
		return Strings.nullToEmpty(string).toLowerCase();
	}

	private static String value(HString string) {
		if (string == null) {
			return "";
		}
		return string.getValue().toLowerCase();
	}

}
