/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IInternalMailboxItems;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ItemsTransferService implements IItemsTransfer {

	public static class Factory implements ServerSideServiceProvider.IServerSideServiceFactory<IItemsTransfer> {

		@Override
		public Class<IItemsTransfer> factoryClass() {
			return IItemsTransfer.class;
		}

		@Override
		public IItemsTransfer instance(BmContext context, String... params) {
			if (params.length != 2) {
				throw new ServerFault("fromMailboxUid & toMailboxUid are required.");
			}
			return new ItemsTransferService(context, params[0], params[1]);
		}

	}

	@FunctionalInterface
	public interface PostCopyOp {
		void operation(List<Long> srcItems);
	}

	public interface ICopyStrategy {
		List<ItemIdentifier> copy(List<Long> itemIds, PostCopyOp op);
	}

	private static final Logger logger = LoggerFactory.getLogger(ItemsTransferService.class);

	private final IDbMailboxRecords fromRecords;
	private final IInternalMailboxItems fromImap;
	private final IInternalMailboxItems toRecords;
	private ICopyStrategy copyStrat;

	@VisibleForTesting
	public static boolean FORCE_CROSS = false; // NOSONAR

	public ItemsTransferService(BmContext context, String fromUid, String toUid) {

		this.fromRecords = context.provider().instance(IDbMailboxRecords.class, fromUid);
		this.fromImap = context.provider().instance(IInternalMailboxItems.class, fromUid);
		this.toRecords = context.provider().instance(IInternalMailboxItems.class, toUid);
		this.copyStrat = loadStrat(context, fromUid, toUid);
	}

	private ICopyStrategy loadStrat(BmContext context, String fromUid, String toUid) {
		String loc1 = DataSourceRouter.location(context, IMailReplicaUids.mboxRecords(fromUid));
		String loc2 = DataSourceRouter.location(context, IMailReplicaUids.mboxRecords(toUid));
		if (loc1.equals(loc2) && !FORCE_CROSS) {
			return new ImapCopyStrategy(context, toUid);
		} else {
			return new CrossBackendCopyStrategy();
		}
	}

	@Override
	public List<ItemIdentifier> copy(List<Long> itemIds) {
		return transferImpl(itemIds, x -> {
		});
	}

	public class CrossBackendCopyStrategy implements ICopyStrategy {
		public List<ItemIdentifier> copy(List<Long> itemIds, PostCopyOp op) {
			List<ImapBinding> srcItems = fromRecords.imapBindings(itemIds);
			List<MailboxItem> toCreate = new ArrayList<>(itemIds.size());
			List<String> parts = new ArrayList<>(itemIds.size());
			for (ImapBinding src : srcItems) {
				MailboxItem mi = new MailboxItem();
				Stream emlStream = fromRecords.fetchComplete(src.imapUid);
				String asPartId = toRecords.uploadPart(emlStream);
				parts.add(asPartId);
				mi.body = new MessageBody();
				mi.body.structure = Part.create(null, "message/rfc822", asPartId);
				toCreate.add(mi);
			}
			return toRecords.multiCreate(toCreate);
		}
	}

	public class ImapCopyStrategy implements ICopyStrategy {
		private String toUid;
		private BmContext context;

		public ImapCopyStrategy(BmContext context, String toUid) {
			this.toUid = toUid;
			this.context = context;
		}

		public List<ItemIdentifier> copy(List<Long> itemIds, PostCopyOp op) {
			List<ItemIdentifier> ret = new ArrayList<>(itemIds.size());
			for (List<Long> someItemIds : Lists.partition(itemIds, 100)) {
				ret.addAll(copySome(someItemIds, op));
			}
			return ret;
		}

		private List<ItemIdentifier> copySome(List<Long> itemIds, PostCopyOp op) {
			List<ImapBinding> srcItems = fromRecords.imapBindings(itemIds);
			if (srcItems.isEmpty()) {
				return Collections.emptyList();
			}
			String destImap = toRecords.imapFolder();
			String srcImap = fromImap.imapFolder();
			IOfflineMgmt idAllocator = context.provider().instance(IOfflineMgmt.class,
					context.getSecurityContext().getContainerUid(), context.getSecurityContext().getSubject());
			IdRange idRange = idAllocator.allocateOfflineIds(srcItems.size());
			long startId = idRange.globalCounter;
			for (ImapBinding ib : srcItems) {
				long expec = idRange.globalCounter++;
				GuidExpectedIdCache.store(toUid + ":" + ib.bodyGuid, expec);
			}
			CompletableFuture<?> replicated = ReplicationEvents.onRecordIdChanged(toUid, startId);
			CompletableFuture<Map<Integer, Integer>> freshImapUids = new CompletableFuture<>();
			toRecords.imapExecutor().withClient(sc -> {
				if (sc.select(srcImap)) {
					Map<Integer, Integer> mapping = sc.uidCopy(
							srcItems.stream().map(ib -> (int) ib.imapUid).collect(Collectors.toList()), destImap);
					if (!mapping.isEmpty()) {
						logger.info("IMAP copy returned {} item(s)", mapping.size());
						op.operation(itemIds);
					} else {
						logger.warn("IMAP copy returned no items");
						replicated.complete(null);
					}
					freshImapUids.complete(mapping);
				} else {
					ServerFault ex = new ServerFault("Failed to select " + srcImap);
					freshImapUids.completeExceptionally(ex);
					replicated.completeExceptionally(ex);
				}
			});
			try {
				return replicated.thenCompose(v -> freshImapUids).thenApply(mapping -> {
					List<ItemIdentifier> ret = new ArrayList<>(srcItems.size());
					long v = toRecords.getVersion();
					long start = startId;
					for (int imapUid : mapping.values()) {
						ret.add(ItemIdentifier.of(imapUid + ".", start++, v));
					}
					return ret;
				}).get(ImapMailboxRecordsService.DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			} catch (TimeoutException to) {
				throw new ServerFault("timeout: " + to.getMessage(), ErrorCode.TIMEOUT);
			} catch (Exception e) {
				throw new ServerFault(e);
			}

		}
	}

	public List<ItemIdentifier> transferImpl(List<Long> itemIds, PostCopyOp op) {
		List<ItemIdentifier> ret = copyStrat.copy(itemIds, op);
		return ret;
	}

	@Override
	public List<ItemIdentifier> move(List<Long> itemIds) {
		return transferImpl(itemIds, toDelete -> fromImap.multipleDeleteById(itemIds));
	}

}
