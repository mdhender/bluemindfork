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
package net.bluemind.user.service.internal;

import java.util.Optional;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.service.AbstractVCardAdapter;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.User;

public class UserVCardAdapter extends AbstractVCardAdapter<User> {

	@Override
	public VCard asVCard(ItemValue<Domain> domain, String uid, User user) {
		if (user.contactInfos == null) {
			return null;
		}
		VCard ret = user.contactInfos;
		ret.kind = Kind.individual;
		ret.source = "bm://" + domain.uid + "/users/" + uid;
		if (user.emails != null && !user.emails.isEmpty()) {
			ret.communications.emails = getEmails(domain, user.emails);
			String latd = user.login + "@" + domain.value.name;
			Optional<Email> systemEmail = ret.communications.emails.stream().filter(e -> e.value.equals(latd))
					.findFirst();
			if (systemEmail.isPresent()) {
				Optional<Parameter> system = systemEmail.get().parameters.stream().filter(p -> p.label.equals("SYSTEM"))
						.findFirst();
				if (!system.isPresent()) {
					systemEmail.get().parameters.add(Parameter.create("SYSTEM", "true"));
				} else {
					system.get().value = "true";
				}
			}
		}

		if (null == ret.identification.formatedName || null == ret.identification.formatedName.value) {
			ret.identification.formatedName = FormatedName.create(user.login);
		}

		return ret;
	}

}
