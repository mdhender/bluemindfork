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

import java.util.Arrays;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;

public class UserSanitizer implements ISanitizer<User> {

	private BmContext context;

	public UserSanitizer(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(User obj) throws ServerFault {
		// sanitize vcard
		if (obj.contactInfos == null) {
			obj.contactInfos = new VCard();
		}
		if (obj.accountType == null) {
			obj.accountType = AccountType.FULL;
		}
		sanitize(obj);
		new Sanitizer(context).create(obj.contactInfos);
	}

	@Override
	public void update(User current, User obj) throws ServerFault {
		if (obj.contactInfos == null) {
			obj.contactInfos = new VCard();
		}
		if (obj.accountType == null) {
			obj.accountType = AccountType.FULL;
		}
		sanitize(obj);
		new Sanitizer(context).create(obj.contactInfos);
	}

	private void sanitize(User user) {
		if (user.routing == null) {
			user.routing = Routing.none;
		}

		if (user.contactInfos.defaultMail() == null && user.defaultEmail() != null) {
			user.contactInfos.communications.emails = Arrays
					.asList(VCard.Communications.Email.create(user.defaultEmailAddress()));
		}
	}

}
