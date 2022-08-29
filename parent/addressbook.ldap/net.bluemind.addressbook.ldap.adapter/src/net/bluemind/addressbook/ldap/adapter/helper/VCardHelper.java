/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.addressbook.ldap.adapter.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Value;

import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;

import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Parameter;

public class VCardHelper {

	private static List<Parameter> getParameters(List<String> types) {
		List<Parameter> parameters = new ArrayList<>(types.size());
		for (String type : types) {
			parameters.add(Parameter.create("TYPE", type));
		}

		return parameters;
	}

	public static List<Tel> managePhones(Attribute phoneAttribute, List<String> types) {
		List<Tel> phones = new ArrayList<>();
		List<Parameter> parameters = getParameters(types);

		if (phoneAttribute != null) {
			Iterator<Value> phoneIterator = phoneAttribute.iterator();
			while (phoneIterator.hasNext()) {
				String phone = phoneIterator.next().getString().trim();
				if (phone.isEmpty()) {
					continue;
				}

				phones.add(Tel.create(phone, parameters));
			}
		}

		return phones;
	}

	public static List<Email> manageEmails(Attribute emailAttribute, String type) {
		List<Email> emails = new ArrayList<>();
		ArrayList<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(Parameter.create("TYPE", type));

		if (emailAttribute != null) {
			Iterator<Value> it = emailAttribute.iterator();
			while (it.hasNext()) {
				String emailAddress = it.next().getString().trim();
				if (emailAddress.isEmpty()) {
					continue;
				}

				emails.add(Email.create(emailAddress, parameters));
			}
		}

		return emails;
	}

	public static List<DeliveryAddressing> manageAddress(Attribute addressAttribute, String type) {
		// address format: streetAddress $extentedAddress $postOfficeBox
		// $postalCode locality $country
		List<DeliveryAddressing> addresses = new ArrayList<>();

		if (addressAttribute != null) {
			Iterator<Value> it = addressAttribute.iterator();
			while (it.hasNext()) {
				String address = it.next().getString().trim();
				if (address.isEmpty()) {
					continue;
				}
				addresses.add(getAddress(address, type));
			}
		}
		return addresses;

	}

	private static DeliveryAddressing getAddress(String address, String type) {
		List<String> fields = Splitter.on("$").trimResults().splitToList(address);

		int postalCodeAndLocalityPosition = 0; // should be 1, 2 or 3
		int count = 0;
		for (String s : fields) {
			if (isPostalCodeAndLocality(s)) {
				postalCodeAndLocalityPosition = count;
			}
			count++;
		}

		DeliveryAddressing addr = new DeliveryAddressing();
		addr.address.parameters = new ArrayList<Parameter>();
		addr.address.parameters.add(Parameter.create("TYPE", type));

		if (postalCodeAndLocalityPosition == 0 || postalCodeAndLocalityPosition > 3) {
			// Don't know what to do.
			addr.address.streetAddress = address;
		} else {
			addr.address.streetAddress = fields.get(0);
			if (postalCodeAndLocalityPosition == 3) {
				addr.address.extentedAddress = fields.get(1);
				addr.address.postOfficeBox = fields.get(2);
			} else if (postalCodeAndLocalityPosition == 2) {
				String s = fields.get(1);
				if (isPostalOfficeBox(s)) {
					addr.address.postOfficeBox = s;
				} else {
					addr.address.extentedAddress = s;
				}
			}

			String[] codeAndLocality = fields.get(postalCodeAndLocalityPosition).split(" ", 2);
			addr.address.postalCode = codeAndLocality[0];
			addr.address.locality = codeAndLocality[1];

			if (fields.size() == postalCodeAndLocalityPosition + 2) {
				addr.address.countryName = fields.get(postalCodeAndLocalityPosition + 1);
			}

		}
		return addr;
	}

	private static boolean isPostalOfficeBox(String s) {
		return s.startsWith("BP");
	}

	private static boolean isPostalCodeAndLocality(String s) {
		String[] codeAndLocality = s.split(" ", 2);
		if (codeAndLocality.length != 2) {
			return false;
		}
		Integer postalCode = Ints.tryParse(codeAndLocality[0]);
		return postalCode != null;
	}

}
