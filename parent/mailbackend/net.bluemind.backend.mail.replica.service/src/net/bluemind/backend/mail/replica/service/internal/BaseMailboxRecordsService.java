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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.sql.DataSource;

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
import net.bluemind.core.container.api.ContainerSettingsKeys;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.CountFastPath;
import net.bluemind.core.container.model.IdQuery;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
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
	protected final DataSource savedDs;
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
	protected final Supplier<ContainerSettingsStore> settingsStore;
	private OnceReadCheck onceRead;

	public BaseMailboxRecordsService(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService, ReplicasStore store) {
		this.savedDs = ds;
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
		this.settingsStore = Suppliers.memoize(() -> new ContainerSettingsStore(ds, container));
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(container.owner));
		this.onceRead = new OnceReadCheck(rbac);
	}

	private static final class OnceReadCheck {
		private final AtomicBoolean once = new AtomicBoolean(false);
		private final RBACManager mgr;
		private static final Set<String> READ = Set.of(Verb.Read.name());

		public OnceReadCheck(RBACManager mgr) {
			this.mgr = mgr;
		}

		public void readCheck() {
			if (once.compareAndSet(false, true)) {
				mgr.check(READ);
			}
		}
	}

	protected ItemValue<MailboxItem> adapt(ItemValue<MailboxRecord> rec) {
		if (rec == null) {
			return null;
		}
		return ItemValue.create(rec, rec.value);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) {
		onceRead.readCheck();
		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, container);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) {
		onceRead.readCheck();
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) {
		onceRead.readCheck();
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		onceRead.readCheck();
		return storeService.changesetById(since, filter);
	}

	@Override
	public long getVersion() {
		onceRead.readCheck();
		return storeService.getVersion();
	}

	@Override
	public Count count(ItemFlagFilter filter) {
		Optional<CountFastPath> fastPath = filter.availableFastPath();
		if (fastPath.isPresent()) {
			return recordStore.fastpathCount(fastPath.get()).orElseGet(() -> {
				try {
					return recordStore.count(filter);
				} catch (SQLException e) {
					throw ServerFault.sqlFault(e);
				}
			});
		}

		try {
			return recordStore.count(filter);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) {
		onceRead.readCheck();
		try {
			SortDescriptor sortDesc = sorted;
			if (sortDesc == null) {
				sortDesc = new SortDescriptor();
			}
			sortDescSanitizer.create(sortDesc);
			boolean fastSortEnabled = Boolean.parseBoolean(settingsStore.get().getSettings()
					.getOrDefault(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name(), "true"));
			if (!fastSortEnabled) {
				logger.info("[{}] fast record sort is disabled by settings", container);
			}
			return recordStore.sortedIds(MailRecordSortStrategyFactory.get(fastSortEnabled, sortDesc).queryToSort());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public ListResult<Long> allIds(String filter, Long knownContainerVersion, Integer limit, Integer offset) {
		onceRead.readCheck();
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
			guid = recordStore.getImapUidReferences(imapUid);
		} catch (SQLException e1) {
			throw ServerFault.sqlFault(e1);
		}
		if (guid == null) {
			throw ServerFault.notFound("guid not found for imapUid " + imapUid + " in container " + container.id);
		}

		try {
			MessageBodyObjectStore sds = sdsSuppply.get();
			ByteBuf sdsDl = sds.openMmap(guid);
			logger.debug("Read {} aka {} from SDS ({} bytes)", imapUid, guid, sdsDl.readableBytes());
			return sdsDl;
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault("error loading " + guid, e);
		}

	}

}
