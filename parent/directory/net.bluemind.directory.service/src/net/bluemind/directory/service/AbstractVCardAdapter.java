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
package net.bluemind.directory.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.service.DirValueStoreService.VCardAdapter;
import net.bluemind.domain.api.Domain;

public abstract class AbstractVCardAdapter<T> implements VCardAdapter<T> {

	protected List<VCard.Communications.Email> getEmails(ItemValue<Domain> domain, Collection<Email> emails) {
		List<VCard.Communications.Email> vcardEmails = new ArrayList<>();

		for (Email email : emails) {
			if (email.allAliases) {

				String[] splitted = email.address.split("@");
				String addr = splitted[0];
				List<String> domains = new ArrayList<>(domain.value.aliases);
				if (domains.remove(splitted[1])) {
					domains.add(domain.value.defaultAlias);
				}
				vcardEmails.add(VCard.Communications.Email.create(email.address,
						Arrays.asList(Parameter.create("DEFAULT", email.isDefault ? "true" : "false"),
								Parameter.create("SYSTEM", "false"), Parameter.create("TYPE", "work"))));

				for (String d : Lists.reverse(domains)) {
					vcardEmails.add(VCard.Communications.Email.create(addr + "@" + d,
							Arrays.asList(Parameter.create("DEFAULT", "false"), Parameter.create("SYSTEM", "false"),
									Parameter.create("TYPE", "work"))));
				}
			} else {
				vcardEmails.add(VCard.Communications.Email.create(email.address,
						Arrays.asList(Parameter.create("DEFAULT", email.isDefault ? "true" : "false"),
								Parameter.create("SYSTEM", "false"), Parameter.create("TYPE", "work"))));
			}
		}

		vcardEmails.removeIf(e -> e.value.endsWith(".internal"));

		return vcardEmails;
	}

}
