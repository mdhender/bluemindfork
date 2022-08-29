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
package net.bluemind.system.importation.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;

import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.Parameter;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class VCardHelper {
	/**
	 * @param entry
	 * @param adPhoneFax
	 * @param string
	 * @param string2
	 * @return
	 */
	public static List<Tel> managePhones(Entry entry, String[] phoneAttrs, String... types) {
		List<Tel> phones = new ArrayList<>();
		List<Parameter> parameters = getParamters(types);

		for (String phoneAttr : phoneAttrs) {
			Attribute adPhones = entry.get(phoneAttr);
			if (adPhones != null) {
				Iterator<Value> phoneIterator = adPhones.iterator();
				while (phoneIterator.hasNext()) {
					String phone = phoneIterator.next().getString().trim();
					if (phone.isEmpty()) {
						continue;
					}

					phones.add(Tel.create(phone, parameters));
				}
			}
		}

		return phones;
	}

	/**
	 * @param types
	 * @return
	 */
	private static List<Parameter> getParamters(String... types) {
		List<Parameter> parameters = new ArrayList<>(types.length);
		for (String type : types) {
			parameters.add(Parameter.create("TYPE", type));

		}

		return parameters;
	}

	/**
	 * @param emails
	 * @return
	 */
	public static List<Email> manageEmails(Collection<net.bluemind.core.api.Email> userEmails) {
		List<Parameter> parameters = typeWorkParameter();
		return userEmails.stream().map(email -> Email.create(email.address, parameters)).collect(Collectors.toList());
	}

	public static Email manageEmail(net.bluemind.core.api.Email userEmail) {
		return Email.create(userEmail.address, typeWorkParameter());
	}

	private static List<Parameter> typeWorkParameter() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(Parameter.create("TYPE", "work"));
		return parameters;
	}
}
