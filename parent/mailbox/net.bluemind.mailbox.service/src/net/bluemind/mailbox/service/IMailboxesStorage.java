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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.server.api.Server;

public interface IMailboxesStorage {

	public static class MailFolder {
		public String name;
		public Type type;
		public String rootUri;

		public enum Type {
			normal, mailshare, user
		}
	}

	void delete(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault;

	void update(BmContext context, String domainUid, ItemValue<Mailbox> previousValue, ItemValue<Mailbox> value)
			throws ServerFault;

	void create(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault;

	MailboxQuota getQuota(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault;

	void initialize(BmContext context, ItemValue<Server> server) throws ServerFault;

	boolean mailboxExist(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) throws ServerFault;

	// One method per repair feels horribly wrong here as most repair are only
	// relevant for cyrus
	// The check & repair monitor to track progress is not even given which prevent
	// progress reporting

	public static class CheckAndRepairStatus {
		public CheckAndRepairStatus(int checked, int broken, int fixed) {
			this.checked = checked;
			this.broken = broken;
			this.fixed = fixed;
		}

		public int checked;
		public int broken;
		public int fixed;
	}

	void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer);

}
