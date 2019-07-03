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

import net.bluemind.directory.service.DirValueStoreService.MailboxAdapter;
import net.bluemind.directory.service.NullMailboxAdapter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.User;

public class UserMailboxAdapter implements MailboxAdapter<User> {

	private UserMailboxAdapter() {
	}

	@Override
	public Mailbox asMailbox(String domainUid, String uid, User user) {
		if (user.system) {
			return null;
		}
		Mailbox mbox = new Mailbox();
		mbox.archived = user.archived;

		if (user.dataLocation != null) {
			mbox.dataLocation = user.dataLocation;
		}
		mbox.emails = user.emails;
		mbox.hidden = user.hidden;
		mbox.name = user.login;
		mbox.routing = user.routing;
		mbox.type = Mailbox.Type.user;
		mbox.system = user.system;
		mbox.quota = user.quota;

		return mbox;
	}

	public static MailboxAdapter<User> create(boolean globalVirt) {
		if (globalVirt) {
			return new NullMailboxAdapter<>();
		} else {
			return new UserMailboxAdapter();
		}
	}
}
