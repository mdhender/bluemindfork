/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.cyrus.storage.testhelper;

import java.util.List;

import net.bluemind.backend.cyrus.internal.CyrusMailboxesStorage;
import net.bluemind.backend.mailapi.storage.MailApiBoxStorage;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.common.DefaultFolder.Status;
import net.bluemind.server.api.Server;

public class CompositeBoxStorage implements IMailboxesStorage {

	private CyrusMailboxesStorage cyrus;
	private MailApiBoxStorage mailApi;

	public CompositeBoxStorage() {
		this.cyrus = new CyrusMailboxesStorage();
		this.mailApi = new MailApiBoxStorage();
		System.err.println("composite box storage created.");

	}

	public void create(BmContext context, String domainUid, ItemValue<Mailbox> mbox) throws ServerFault {
		mailApi.create(context, domainUid, mbox);
		cyrus.create(context, domainUid, mbox);
		System.err.println("composite create of " + mbox);
	}

	public void update(BmContext context, String domainUid, ItemValue<Mailbox> previousValue, ItemValue<Mailbox> value)
			throws ServerFault {
		cyrus.update(context, domainUid, previousValue, value);
		mailApi.update(context, domainUid, previousValue, value);
	}

	public boolean mailboxExist(BmContext context, String domainUid, ItemValue<Mailbox> cur) throws ServerFault {
		return cyrus.mailboxExist(context, domainUid, cur);
	}

	public void delete(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		cyrus.delete(context, domainUid, value);
		mailApi.delete(context, domainUid, value);
	}

	public void changeFilter(BmContext context, ItemValue<Domain> domain, ItemValue<Mailbox> mailboxItem,
			MailFilter filter) throws ServerFault {
		cyrus.changeFilter(context, domain, mailboxItem, filter);
		mailApi.changeFilter(context, domain, mailboxItem, filter);
	}

	public void changeDomainFilter(BmContext context, String domainUid, MailFilter filter) throws ServerFault {
		cyrus.changeDomainFilter(context, domainUid, filter);
		mailApi.changeDomainFilter(context, domainUid, filter);
	}

	public void createDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {
		cyrus.createDomainPartition(context, value, server);
		mailApi.createDomainPartition(context, value, server);
	}

	public void deleteDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {
		cyrus.deleteDomainPartition(context, value, server);
		mailApi.deleteDomainPartition(context, value, server);
	}

	public void initialize(BmContext context, ItemValue<Server> server) throws ServerFault {
		cyrus.initialize(context, server);
		mailApi.initialize(context, server);
	}

	public MailboxQuota getQuota(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		cyrus.getQuota(context, domainUid, value);
		return mailApi.getQuota(context, domainUid, value);
	}

	public void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer) {
		cyrus.move(domainUid, mailbox, sourceServer, dstServer);
		mailApi.move(domainUid, mailbox, sourceServer, dstServer);
	}

	public void rewriteCyrusConfiguration(String serverUid, boolean reload) {
		cyrus.rewriteCyrusConfiguration(serverUid, reload);
	}

	@Override
	public List<MailFolder> checkAndRepairHierarchy(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) throws ServerFault {
		throw new UnsupportedOperationException("checkAndRepairHierarchy");

	}

	@Override
	public void checkAndRepairQuota(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		throw new UnsupportedOperationException("checkAndRepairQuota");

	}

	@Override
	public void checkAndRepairFilesystem(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		throw new UnsupportedOperationException("checkAndRepairFilesystem");
	}

	@Override
	public Status checkAndRepairDefaultFolders(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) {
		throw new UnsupportedOperationException("checkAndRepairDefaultFolders");

	}

	@Override
	public List<MailFolder> checkAndRepairAcl(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			List<AccessControlEntry> acls, boolean repair) throws ServerFault {
		return mailApi.checkAndRepairAcl(context, domainUid, mailbox, acls, repair);
	}

	@Override
	public CheckAndRepairStatus checkAndRepairSharedSeen(BmContext context, String domainUid,
			ItemValue<Mailbox> mailbox, boolean repair) {
		throw new UnsupportedOperationException("checkAndRepairSharedSeen");
	}

	@Override
	public boolean mailboxRequiresCreationInCyrus(BmContext context, String domainUid, Mailbox previous,
			Mailbox current) {
		return cyrus.mailboxRequiresCreationInCyrus(context, domainUid, previous, current);
	}

}
