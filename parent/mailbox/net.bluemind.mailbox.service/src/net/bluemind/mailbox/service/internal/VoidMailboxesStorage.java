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
package net.bluemind.mailbox.service.internal;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.mailbox.service.common.DefaultFolder.Status;
import net.bluemind.server.api.Server;

public class VoidMailboxesStorage implements IMailboxesStorage {
	public static final IMailboxesStorage INSTANCE = new VoidMailboxesStorage();
	private Logger logger = LoggerFactory.getLogger(VoidMailboxesStorage.class);

	@Override
	public void delete(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		logger.warn("VOID MAILSTORAGE delete {}:{}", domainUid, value.uid);
	}

	@Override
	public void update(BmContext context, String domainUid, ItemValue<Mailbox> previousValue, ItemValue<Mailbox> value)
			throws ServerFault {
		logger.warn("VOID MAILSTORAGE update {}:{}", domainUid, value.uid);
	}

	@Override
	public void create(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		logger.warn("VOID MAILSTORAGE create {}:{}", domainUid, value.uid);
	}

	@Override
	public void changeFilter(BmContext context, ItemValue<Domain> domain, ItemValue<Mailbox> value, MailFilter filter)
			throws ServerFault {
		logger.warn("VOID MAILSTORAGE changeFilter {}:{}", domain.uid, value.uid);
	}

	@Override
	public void changeDomainFilter(BmContext context, String domainUid, MailFilter filter) throws ServerFault {
		logger.warn("VOID MAILSTORAGE changeDomainFilter {}", domainUid);
	}

	@Override
	public void createDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {
		logger.warn("VOID MAILSTORAGE createDomainPartition {} on {}", value.uid, server.value.address());

	}

	@Override
	public void deleteDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {
		logger.warn("VOID MAILSTORAGE deleteDomainPartition {} on {}", value.uid, server.value.address());

	}

	@Override
	public void initialize(BmContext context, ItemValue<Server> server) throws ServerFault {
		logger.warn("VOID MAILSTORAGE initialize {}", server.value.address());
	}

	@Override
	public boolean mailboxExist(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) throws ServerFault {
		return false;
	}

	@Override
	public MailboxQuota getQuota(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		return null;
	}

	@Override
	public List<MailFolder> checkAndRepairHierarchy(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) throws ServerFault {
		logger.warn("VOID MAILSTORAGE checkAndRepairHierarchy {}:{}", domainUid, mailbox.uid);
		return Collections.emptyList();
	}

	@Override
	public void checkAndRepairQuota(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		logger.warn("VOID MAILSTORAGE checkAndRepairQuota {}:{}", domainUid, mailbox.uid);
	}

	@Override
	public void checkAndRepairFilesystem(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		logger.warn("VOID MAILSTORAGE checkAndRepairFilesystem {}:{}", domainUid, mailbox.uid);
	}

	@Override
	public Status checkAndRepairDefaultFolders(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) {
		logger.warn("VOID MAILSTORAGE checkAndRepairDefaultFolders {}:{}", domainUid, mailbox.uid);
		return new DefaultFolder.Status();
	}

	@Override
	public List<MailFolder> checkAndRepairAcl(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			List<AccessControlEntry> acls, boolean repair) throws ServerFault {
		logger.warn("VOID MAILSTORAGE checkAndRepairAcl {}:{}", domainUid, mailbox.uid);
		return Collections.emptyList();
	}

	@Override
	public CheckAndRepairStatus checkAndRepairSharedSeen(BmContext context, String domainUid,
			ItemValue<Mailbox> mailbox, boolean repair) {
		logger.warn("VOID MAILSTORAGE checkAndRepairSharedSeen {}:{}", domainUid, mailbox.uid);
		return new CheckAndRepairStatus(0, 0, 0);
	}

	@Override
	public void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer) {
		logger.warn("VOID MAILSTORAGE move");
	}

	@Override
	public void rewriteCyrusConfiguration(String serverUid) {
		logger.warn("VOID MAILSTORAGE rewriteCyrusConfiguration");
	}

}
