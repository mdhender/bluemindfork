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
package net.bluemind.mailbox.service;

import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IEventBusAccessRule;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class MailboxEventBusAccessRule implements IEventBusAccessRule {

	private static final String BASE_ADDRESS = "bm.mailbox.hook.";

	@Override
	public boolean match(String path) {
		if (path.startsWith(BASE_ADDRESS) && path.endsWith(".changed")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean authorize(BmContext context, String path) {
		String uid = path.substring(BASE_ADDRESS.length());
		uid = uid.substring(0, uid.length() - ".changed".length());
		String containerUid = IMailboxAclUids.uidForMailbox(uid);
		return new RBACManager(context).forContainer(containerUid).can(Verb.Read.name());
	}

}
