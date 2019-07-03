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
package net.bluemind.backend.cyrus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class ServerHook extends DefaultServerHook {

	private static Logger logger = LoggerFactory.getLogger(ServerHook.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {

		if (!"mail/imap".equals(tag)) {
			return;
		}
		logger.info("onServerTagged mail/imap as {}", tag);

		// append our configuration
		MailboxesStorageFactory.getMailStorage().initialize(context, server);
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!"mail/imap".equals(tag)) {
			return;
		}
		// don't really care, shutdown cyrus ?
	}

	@Override
	public void onServerAssigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> assignedDomain,
			String tag) throws ServerFault {

		if (!tag.equals("mail/imap")) {
			logger.debug("Not an imap backend assignment");
			return;
		}

		MailboxesStorageFactory.getMailStorage().createDomainPartition(context, assignedDomain, server);
		MailboxesStorageFactory.getMailStorage().changeDomainFilter(context, assignedDomain.uid, new MailFilter());
	}

	@Override
	public void onServerPreUnassigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain, String tag)
			throws ServerFault {
		if (!tag.equals("mail/imap")) {
			logger.debug("Not an imap backend unassignment");
			return;
		}

		IDirectory service = context.provider().instance(IDirectory.class, domain.uid);
		ListResult<ItemValue<DirEntry>> entries = service
				.search(DirEntryQuery.filterKind(Kind.USER, Kind.MAILSHARE, Kind.RESOURCE, Kind.GROUP));
		long count = entries.values.stream().filter(entry -> server.uid.equals(entry.value.dataLocation)).count();
		if (count > 0) {
			throw new ServerFault("Cannot unassign mail/imap ", ErrorCode.FAILURE);
		}
	}

	@Override
	public void onServerUnassigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain, String tag)
			throws ServerFault {
		if (!tag.equals("mail/imap")) {
			logger.debug("Not an imap backend unassignment");
			return;
		}
		MailboxesStorageFactory.getMailStorage().deleteDomainPartition(context, domain, server);

	}

}
