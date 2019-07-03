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
package net.bluemind.group.service.internal;

import net.bluemind.directory.service.DirValueStoreService.MailboxAdapter;
import net.bluemind.group.api.Group;
import net.bluemind.group.service.GroupHelper;
import net.bluemind.mailbox.api.Mailbox;

public class GroupMailboxAdapter implements MailboxAdapter<Group> {

	@Override
	public Mailbox asMailbox(String domainUid, String uid, Group group) {
		return GroupHelper.groupToMailbox(group);
	}

}
