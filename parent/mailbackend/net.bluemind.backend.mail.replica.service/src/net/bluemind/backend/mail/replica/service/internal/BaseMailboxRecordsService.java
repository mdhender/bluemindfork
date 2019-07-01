/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.partitions.MailboxDescriptor;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class BaseMailboxRecordsService implements IChangelogSupport, ICountingSupport, ISortingSupport {

	private static final Logger logger = LoggerFactory.getLogger(BaseMailboxRecordsService.class);
	protected final BmContext context;
	protected final String mailboxUniqueId;
	protected final ContainerStoreService<MailboxRecord> storeService;
	protected final MailboxRecordStore recordStore;
	protected final Container container;
	protected final ReplicasStore replicaStore;
	protected final Optional<SubtreeLocation> optRecordsLocation;
	protected final RBACManager rbac;

	public BaseMailboxRecordsService(Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService, ReplicasStore store) {
		this.container = cont;
		this.context = context;
		this.mailboxUniqueId = mailboxUniqueId;
		this.recordStore = recordStore;
		this.storeService = storeService;
		this.replicaStore = store;
		this.optRecordsLocation = SubtreeLocations.getById(store, mailboxUniqueId);
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(container.owner));
	}

	protected ItemValue<MailboxItem> adapt(ItemValue<MailboxRecord> rec) {
		if (rec == null) {
			return null;
		}
		return ItemValue.create(rec, rec.value);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbac.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, storeService, container.domainUid);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.changesetById(since, filter);
	}

	@Override
	public long getVersion() throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.count(filter);
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		rbac.check(Verb.Read.name());
		try {
			return recordStore.sortedIds(sorted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public Stream fetchComplete(long imapUid) {
		SubtreeLocation recordsLocation = optRecordsLocation
				.orElseThrow(() -> new ServerFault("Missing subtree location"));
		String datalocation = DataSourceRouter.location(context, recordsLocation.subtreeContainer);
		CyrusPartition partition = CyrusPartition.forServerAndDomain(datalocation, container.domainUid);

		Iterator<String> mbox = Splitter.on("/").split(recordsLocation.contName).iterator();
		String mboxType = mbox.next();

		MailboxDescriptor md = new MailboxDescriptor();
		md.type = "users".equals(mboxType) ? Type.user : Type.mailshare;
		md.mailboxName = mbox.next();
		md.utf7FolderPath = UTF7Converter.encode(recordsLocation.imapPath());

		if (md.type == Type.mailshare) {
			md.utf7FolderPath = md.utf7FolderPath.substring("Dossiers partag&AOk-s/".length(),
					md.utf7FolderPath.length());
		}
		IServiceTopology topology = Topology.get();

		InputStream oioStream = null;
		ItemValue<Server> backend = topology.datalocation(datalocation);

		if (backend.uid.equals(topology.core().uid)) {
			oioStream = directRead(md, partition, imapUid);
		} else {
			INodeClient nc = NodeActivator.get(backend.value.address());
			oioStream = nodeRead(nc, md, partition, imapUid);
		}
		if (oioStream == null) {
			throw new ServerFault("body for uid " + imapUid + " not found", ErrorCode.NOT_FOUND);
		}
		final InputStream constStream = oioStream;
		InputReadStream streamAdapter = new InputReadStream(constStream);
		streamAdapter.exceptionHandler(t -> {
			try {
				constStream.close();
			} catch (IOException e) {
			}
		});
		return VertxStream.stream(streamAdapter);

	}

	private InputStream nodeRead(INodeClient nc, MailboxDescriptor md, CyrusPartition partition, long imapUid) {
		String path = CyrusFileSystemPathHelper.getFileSystemPath(container.domainUid, md, partition, imapUid);
		List<FileDescription> file = nc.listFiles(path);
		if (file.isEmpty()) {
			logger.warn("{} {} is not a {}", md, imapUid, path);
			path = CyrusFileSystemPathHelper.getHSMFileSystemPath(container.domainUid, md, partition, imapUid);
			file = nc.listFiles(path);
			if (file.isEmpty()) {
				logger.warn("{} {} is not a {}", md, imapUid, path);
				return null;
			}
		}
		return nc.openStream(path);
	}

	private InputStream directRead(MailboxDescriptor md, CyrusPartition partition, long imapUid) {
		String path = CyrusFileSystemPathHelper.getFileSystemPath(container.domainUid, md, partition, imapUid);
		File pathFile = new File(path);
		if (!pathFile.exists()) {
			logger.warn("{} {} is not a {}", md, imapUid, path);
			path = CyrusFileSystemPathHelper.getHSMFileSystemPath(container.domainUid, md, partition, imapUid);
			pathFile = new File(path);
			if (!pathFile.exists()) {
				logger.warn("{} {} is not a {}", md, imapUid, path);
				return null;
			}
		}

		try {
			return Files.newInputStream(pathFile.toPath());
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

}
