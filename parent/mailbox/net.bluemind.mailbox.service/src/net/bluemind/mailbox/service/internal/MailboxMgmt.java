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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.index.mail.BoxIndexing;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.SimpleShardStats;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.SplittedShardsMapping;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class MailboxMgmt implements IMailboxMgmt {
	private static final Logger logger = LoggerFactory.getLogger(MailboxMgmt.class);
	private final BmContext context;
	private final String domainUid;
	private RBACManager rbacManager;
	private static final IMailboxesStorage mailboxStorage = MailboxesStorageFactory.getMailStorage();

	public MailboxMgmt(BmContext context, String domainUid) {
		this.context = context;
		this.domainUid = domainUid;
		rbacManager = new RBACManager(context).forDomain(domainUid);

	}

	@Override
	public TaskRef consolidateMailbox(final String mailboxUid) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX);

		return context.provider().instance(ITasksManager.class).run(new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				BoxIndexing mailboxIndexer = new BoxIndexing(domainUid);
				mailboxIndexer.resync(getMailboxItem(mailboxUid), monitor);
				logger.info("Consolidate {} mail index, mailbox {}", mailboxIndexer.getCounter().get(), mailboxUid);

			}
		});
	}

	private ItemValue<Mailbox> getMailboxItem(String mailboxUid) throws ServerFault {
		return context.provider().instance(IMailboxes.class, domainUid).getComplete(mailboxUid);
	}

	@Override
	public TaskRef consolidateDomain() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		return context.provider().instance(ITasksManager.class).run(new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				BoxIndexing mailboxIndexer = new BoxIndexing(domainUid);
				IMailboxes service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IMailboxes.class, domainUid);
				List<ItemValue<Mailbox>> mailboxes = service.list();
				monitor.begin(mailboxes.size(),
						String.format("going to consolidate index for %d mailboxes", mailboxes.size()));

				for (ItemValue<Mailbox> mbox : mailboxes) {
					if (mbox.value.routing == Routing.internal) {
						mailboxIndexer.resync(mbox, monitor.subWork(1));
						logger.info("Resync {} mail index, mailbox {}", mailboxIndexer.getCounter().get(),
								mbox.value.name);
					}
				}
			}
		});
	}

	@Override
	public TaskRef moveIndex(String mailboxUid, String indexName) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX);

		if (Strings.isNullOrEmpty(indexName) || !indexName.startsWith("mailspool")) {
			throw new ServerFault("index name must start with mailspool", ErrorCode.INVALID_PARAMETER);
		}
		ItemValue<Mailbox> mailbox = context.provider().instance(IMailboxes.class, domainUid).getComplete(mailboxUid);

		if (mailbox == null) {
			throw new ServerFault("mailbox " + mailboxUid + " not found", ErrorCode.NOT_FOUND);
		}

		return context.provider().instance(ITasksManager.class).run("move-index-" + mailboxUid, new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				RecordIndexActivator.getIndexer().get().moveMailbox(mailboxUid, indexName);
			}
		});
	}

	@Override
	public List<ShardStats> getShardsStats() {
		rbacManager.check(BasicRoles.ROLE_SYSTEM_MANAGER);

		if (!domainUid.equals("global.virt")) {
			throw new ServerFault("only available on global.virt domain");
		}

		return RecordIndexActivator.getIndexer().map(IMailIndexService::getStats)
				.orElseThrow(() -> new ServerFault("RecordIndexActivator is missing, consider restarting core"));
	}

	@Override
	public List<SimpleShardStats> getLiteStats() {
		rbacManager.check(BasicRoles.ROLE_SYSTEM_MANAGER);

		if (!domainUid.equals("global.virt")) {
			throw new ServerFault("only available on global.virt domain");
		}

		return RecordIndexActivator.getIndexer().map(IMailIndexService::getLiteStats)
				.orElseThrow(() -> new ServerFault("RecordIndexActivator is missing, consider restarting core"));
	}

	@Override
	public void move(ItemValue<Mailbox> mailbox, ItemValue<Server> server) throws ServerFault {
		ItemValue<Server> sourceServer = context.su().provider().instance(IServer.class, "default")
				.getComplete(mailbox.value.dataLocation);

		ItemValue<Server> altBackend = SplittedShardsMapping.remap(server);
		mailboxStorage.move(domainUid, mailbox, sourceServer, altBackend);

	}

	public class ServerInfo {

		public String uid;
		public String address;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((address == null) ? 0 : address.hashCode());
			result = prime * result + ((uid == null) ? 0 : uid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServerInfo other = (ServerInfo) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			if (uid == null) {
				if (other.uid != null)
					return false;
			} else if (!uid.equals(other.uid))
				return false;
			return true;
		}

		private MailboxMgmt getOuterType() {
			return MailboxMgmt.this;
		}

	}
}
