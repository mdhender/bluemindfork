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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.MailboxConfig;
import net.bluemind.mailbox.api.MailboxQuota;

public class FakeMailboxes implements IMailboxes {

	private Map<String, ItemValue<Mailbox>> mboxes;

	public FakeMailboxes(Map<String, ItemValue<Mailbox>> mboxes) {
		this.mboxes = mboxes;
	}

	@Override
	public void create(String uid, Mailbox mailshare) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public void update(String uid, Mailbox mailshare) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public ItemValue<Mailbox> getComplete(String uid) throws ServerFault {
		return mboxes.get(uid);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public ItemValue<Mailbox> byEmail(String email) throws ServerFault {
		for (ItemValue<Mailbox> mbox : this.mboxes.values()) {
			for (Email eml : mbox.value.emails) {
				if (eml.address.equals(email)) {
					return mbox;
				}
			}
		}
		return null;
	}

	@Override
	public ItemValue<Mailbox> byName(String name) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public MailFilter getDomainFilter() throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public void setDomainFilter(MailFilter filter) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public MailFilter getMailboxFilter(String mailboxUid) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public void setMailboxFilter(String mailboxUid, MailFilter filter) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public List<AccessControlEntry> getMailboxAccessControlList(String mailboxUid) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public void setMailboxAccessControlList(String mailboxUid, List<AccessControlEntry> accessControlEntries)
			throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public List<String> byType(Mailbox.Type type) throws ServerFault {
		return new ArrayList<>(mboxes.keySet());
	}

	@Override
	public Integer getUnreadMessagesCount() throws ServerFault {
		return 0;
	}

	@Override
	public List<ItemValue<Mailbox>> list() throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public List<String> byRouting(Routing routing) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public TaskRef checkAndRepairAll() throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public TaskRef checkAll() throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public TaskRef checkAndRepair(String uid) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public TaskRef check(String uid) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public MailboxQuota getMailboxQuota(String uid) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public List<ItemValue<Mailbox>> multipleGet(List<String> uids) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public MailboxConfig getMailboxConfig(String uid) throws ServerFault {
		throw new ServerFault("not implemented");
	}

	@Override
	public List<String> listUids() {
		throw new ServerFault("not implemented");
	}

}
