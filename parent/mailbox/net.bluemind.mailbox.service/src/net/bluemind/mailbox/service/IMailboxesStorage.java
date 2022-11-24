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

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.service.common.DefaultFolder.Status;
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

	boolean mailboxRequiresCreationInCyrus(BmContext context, String domainUid, Mailbox previous, Mailbox current);

	void create(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault;

	MailboxQuota getQuota(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault;

	void changeFilter(BmContext context, ItemValue<Domain> domain, ItemValue<Mailbox> value, MailFilter filter)
			throws ServerFault;

	void changeDomainFilter(BmContext context, String domainUid, MailFilter filter) throws ServerFault;

	void createDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server) throws ServerFault;

	void deleteDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server) throws ServerFault;

	void initialize(BmContext context, ItemValue<Server> server) throws ServerFault;

	boolean mailboxExist(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) throws ServerFault;

	// One method per repair feels horribly wrong here as most repair are only
	// relevant for cyrus
	// The check & repair monitor to track progress is not even given which prevent
	// progress reporting

	/**
	 * Fill gaps between folders
	 * 
	 * @param context
	 * @param domainUid
	 * @param mailbox
	 * @param repair
	 * @throws ServerFault
	 */
	List<MailFolder> checkAndRepairHierarchy(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) throws ServerFault;

	/**
	 * Fix mailbox quota
	 * 
	 * @param context
	 * @param domainUid
	 * @param mailbox
	 */
	void checkAndRepairQuota(BmContext context, String domainUid, ItemValue<Mailbox> mailbox);

	/**
	 * Fix mailbox filesystem
	 * 
	 * @param context
	 * @param domainUid
	 * @param mailbox
	 */
	void checkAndRepairFilesystem(BmContext context, String domainUid, ItemValue<Mailbox> mailbox);

	/**
	 * Fix mailbox default folders
	 * 
	 * @param context
	 * @param domainUid
	 * @param mailbox
	 * @param repair
	 * @return
	 * @throws ServerFault
	 */
	Status checkAndRepairDefaultFolders(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair);

	/**
	 * Sync acl with db acl
	 * 
	 * @param context
	 * @param domainUid
	 * @param mailbox
	 * @param acls
	 * @param repair
	 * @return
	 * @throws ServerFault
	 */
	List<MailFolder> checkAndRepairAcl(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			List<AccessControlEntry> acls, boolean repair) throws ServerFault;

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

	/**
	 * 
	 * @param context
	 * @param domainUid
	 * @param mailbox
	 * @param repair
	 */
	CheckAndRepairStatus checkAndRepairSharedSeen(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair);

	void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer);

	void rewriteCyrusConfiguration(String serverUid, boolean reload);

}
