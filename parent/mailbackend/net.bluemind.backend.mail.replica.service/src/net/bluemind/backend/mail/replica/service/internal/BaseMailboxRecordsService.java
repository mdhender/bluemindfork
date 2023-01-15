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

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.backend.mail.replica.service.internal.sort.MailRecordSortStrategyFactory;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.IdQuery;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.mailbox.api.IMailboxAclUids;

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
	protected final Sanitizer sortDescSanitizer;
	protected final Supplier<MessageBodyObjectStore> sdsSuppply;

	public BaseMailboxRecordsService(Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService, ReplicasStore store) {
		this.container = cont;
		this.context = context;
		this.mailboxUniqueId = mailboxUniqueId;
		this.recordStore = recordStore;
		this.storeService = storeService;
		this.replicaStore = store;
		this.optRecordsLocation = SubtreeLocations.getById(store, mailboxUniqueId);
		this.sortDescSanitizer = new Sanitizer(context);
		this.sdsSuppply = Suppliers
				.memoize(() -> new MessageBodyObjectStore(context.su(), DataSourceRouter.location(context, cont.uid)));
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(container.owner));
	}

	protected ItemValue<MailboxItem> adapt(ItemValue<MailboxRecord> rec) {
		if (rec == null) {
			return null;
		}
		return ItemValue.create(rec, rec.value);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) {
		rbac.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, storeService, container.domainUid);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) {
		rbac.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) {
		rbac.check(Verb.Read.name());
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) {
		rbac.check(Verb.Read.name());
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.changesetById(since, filter);
	}

	@Override
	public long getVersion() {
		rbac.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public Count count(ItemFlagFilter filter) {
		rbac.check(Verb.Read.name());
		try {
			return recordStore.count(filter);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) {
		rbac.check(Verb.Read.name());
		try {
			SortDescriptor sortDesc = sorted;
			if (sortDesc == null) {
				sortDesc = new SortDescriptor();
			}
			sortDescSanitizer.create(sortDesc);
			return recordStore.sortedIds(MailRecordSortStrategyFactory.get(sortDesc).queryToSort());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	@Override
	public ListResult<Long> allIds(String filter, Long knownContainerVersion, Integer limit, Integer offset) {
		rbac.check(Verb.Read.name());
		return storeService.allIds(IdQuery.of(filter, knownContainerVersion, limit, offset));
	}

	public Stream fetchComplete(long imapUid) {
		final ByteBuf constStream = fetchCompleteMmap(imapUid);
		return VertxStream.stream(Buffer.buffer(constStream));

	}

	protected InputStream fetchCompleteOIO(long imapUid) {
		return new ByteBufInputStream(fetchCompleteMmap(imapUid), true);
	}

	protected ByteBuf fetchCompleteMmap(long imapUid) {
		String guid = null;
		try {
			guid = recordStore.getImapUidReferences(imapUid, container.owner);
		} catch (SQLException e1) {
			throw ServerFault.sqlFault(e1);
		}
		if (guid == null) {
			throw ServerFault.notFound("guid not found for imapUid " + imapUid + " in container " + container.id);
		}

		try {
			MessageBodyObjectStore sds = sdsSuppply.get();
			ByteBuf sdsDl = sds.openMmap(guid);
			logger.info("Read {} aka {} from SDS ({}bytes)", imapUid, guid, sdsDl.readableBytes());
			return sdsDl;
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault("error loading " + guid, e);
		}

	}

}
