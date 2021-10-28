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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.sun.mail.util.UUDecoderStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.ImapAck;
import net.bluemind.backend.mail.api.ImapItemIdentifier;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.flags.FlagUpdate;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.utils.PartsWalker;
import net.bluemind.backend.mail.parsing.Bodies;
import net.bluemind.backend.mail.parsing.EmlBuilder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IInternalMailboxItems;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.persistence.RecordID;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.backend.mail.replica.service.ReplicationEvents.ItemChange;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.utils.ReadInputStream;
import net.bluemind.core.rest.vertx.BufferReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.ThreadContextHelper;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.TaggedResult;
import net.bluemind.imap.vertx.stream.EmptyStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.Mime4JHelper.SizedStream;

public class ImapMailboxRecordsService extends BaseMailboxRecordsService implements IInternalMailboxItems {

	private static final Logger logger = LoggerFactory.getLogger(ImapMailboxRecordsService.class);
	public static final Integer DEFAULT_TIMEOUT = 18; // sec
	private final String imapFolder;
	private final ImapContext imapContext;
	private final Namespace namespace;
	private final MessageBodyStore bodyStore;

	public ImapMailboxRecordsService(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService) {
		super(cont, context, mailboxUniqueId, recordStore, storeService, new ReplicasStore(ds));
		SubtreeLocation recordsLocation = optRecordsLocation
				.orElseThrow(() -> new ServerFault("Missing subtree location"));

		this.imapFolder = recordsLocation.imapPath(context);
		this.namespace = recordsLocation.namespace();
		this.imapContext = ImapContext.of(context);

		logger.debug("imapContext {}, namespace {}, subtree {}", imapContext, namespace, recordsLocation);

		bodyStore = new MessageBodyStore(ds);
	}

	@Override
	public String imapFolder() {
		return imapFolder;
	}

	@Override
	public ImapCommandRunner imapExecutor() {

		return (Consumer<ImapClient> sc) -> imapContext.withImapClient(inSc -> {
			ImapClient ic = new ImapClient() {

				@Override
				public Map<Integer, Integer> uidCopy(Collection<Integer> uids, String destMailbox) {
					return inSc.uidCopy(uids, destMailbox);
				}

				@Override
				public boolean select(String mbox) {
					try {
						return inSc.select(mbox);
					} catch (IMAPException e) {
						throw new ServerFault(e);
					}
				}
			};
			sc.accept(ic);
			return null;
		});
	}

	@Override
	public ItemValue<MailboxItem> getCompleteById(long id) {
		rbac.check(Verb.Read.name());
		ItemValue<MailboxRecord> record = storeService.get(id, null);
		if (record == null) {
			logger.warn("MailItem {} not found.", id);
			return null;
		}

		String bodyGuid = record.value.messageBody;
		MessageBody body;
		try {
			body = bodyStore.get(bodyGuid);
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), e);
		}

		// ensure we use the same date for the body as the one we use to compute
		// the item weight
		if (body == null) {
			logger.warn("{} body {} is missing for item {}", imapFolder, bodyGuid, id);
			return null;
		}
		Date over = record.value.internalDate;
		if (over != null) {
			body.date = over;
		}
		ItemValue<MailboxItem> adapted = adapt(record);
		adapted.value.body = body;
		return adapted;
	}

	@Override
	public void deleteById(long id) {
		rbac.check(Verb.Write.name());
		logger.debug("Delete {}", id);
		ItemValue<MailboxItem> toDelete = getCompleteById(id);
		if (toDelete != null) {
			Collection<MailboxItemFlag> curFlags = toDelete.value.flags;
			if (!curFlags.contains(MailboxItemFlag.System.Deleted.value())) {
				addFlagsImapCommand(Arrays.asList(Long.toString(toDelete.value.imapUid)),
						MailboxItemFlag.System.Deleted.value().flag, MailboxItemFlag.System.Seen.value().flag);
			}
		} else {
			logger.warn("Nothing to delete for id {} in {}.", id, imapFolder);
		}
	}

	@Override
	public void expunge() {
		imapContext.withImapClient(sc -> {
			sc.select(imapFolder);
			sc.expunge();
			logger.info("{} Expunged {}", imapContext.latd, imapFolder);
			return null;
		});
	}

	@Override
	public void resync() {
		rbac.check(Verb.Write.name());
		long time = System.currentTimeMillis();
		Collection<Integer> imapUids = imapContext.withImapClient(sc -> {
			sc.select(imapFolder);
			return sc.uidSearch(new SearchQuery());
		});
		Set<Long> knownUids = imapUids.stream().map(Integer::longValue).collect(Collectors.toSet());
		List<String> allUids = storeService.allUids();
		List<ItemValue<MailboxRecord>> extraRecords = new ArrayList<>(allUids.size());
		List<ItemValue<MailboxRecord>> unlinkedRecords = new ArrayList<>(allUids.size());
		for (List<String> slice : Lists.partition(allUids, 50)) {
			List<ItemValue<MailboxRecord>> records = storeService.getMultiple(slice);
			for (ItemValue<MailboxRecord> iv : records) {
				if (!knownUids.contains(iv.value.imapUid)) {
					if (!checkExistOnBackend(iv.value.imapUid)) {
						unlinkedRecords.add(iv);
					} else if (!iv.flags.contains(ItemFlag.Deleted)) {
						extraRecords.add(iv);
					}
				}
			}
		}
		logger.debug("Found {} extra record(s), {} unlinked record(s) before resync of {}", extraRecords.size(),
				unlinkedRecords.size(), imapFolder);
		if (!extraRecords.isEmpty()) {
			IDbMailboxRecords recsApi = context.provider().instance(IDbMailboxRecords.class,
					IMailReplicaUids.uniqueId(container.uid));
			List<MailboxRecord> batch = extraRecords.stream().map(iv -> {
				MailboxRecord ret = iv.value;
				ret.flags.add(MailboxItemFlag.System.Deleted.value());
				return ret;
			}).collect(Collectors.toList());
			recsApi.updates(batch);
		}
		if (!unlinkedRecords.isEmpty()) {
			IDbMailboxRecords recsApi = context.provider().instance(IDbMailboxRecords.class,
					IMailReplicaUids.uniqueId(container.uid));
			recsApi.deleteImapUids(unlinkedRecords.stream().map(iv -> iv.value.imapUid).collect(Collectors.toList()));
		}
		time = System.currentTimeMillis() - time;
		logger.debug("{} re-sync completed in {}ms.", imapFolder, time);
	}

	@Override
	public ImapAck updateById(long id, MailboxItem mail) {
		rbac.check(Verb.Write.name());
		if (mail.imapUid == 0) {
			logger.warn("Not updating {} with imapUid 0", id);
			return ImapAck.create(0L, mail.imapUid);
		}
		// has the flags changed ?
		ItemValue<MailboxItem> current = getCompleteById(id);
		MailboxItemFlag mdnSentFlag = new MailboxItemFlag("$MDNSent");
		if (current.value.flags.contains(mdnSentFlag) && !mail.flags.contains(mdnSentFlag)) {
			logger.debug("cannot remove flag $MDNSent (on {})", id);
			mail.flags.add(mdnSentFlag);
		}

		boolean flagsChanged = !new HashSet<>(current.value.flags).equals(new HashSet<>(mail.flags));

		String newSub = Optional.ofNullable(mail.body.subject).orElse("");
		String oldSub = current.value.body.subject;
		boolean subjectChanged = !newSub.equals(oldSub);
		String curHeaders = headersString(current.value);
		String newHeaders = headersString(mail);
		boolean headersChanged = !curHeaders.equals(newHeaders);
		logger.debug("changes are flags:{}, subject:{}, headers:{}", flagsChanged, subjectChanged, headersChanged);
		if (subjectChanged || headersChanged) {
			return mailRewrite(current, mail);
		} else if (flagsChanged) {
			String[] flagNames = mail.flags.stream().map(f -> f.flag).toArray(String[]::new);
			Ack ack = overwriteFlagsImapCommand(Arrays.asList(Long.toString(mail.imapUid)), flagNames);
			return ImapAck.create(ack.version, mail.imapUid);
		} else {
			logger.warn("Subject/Headers/Flags did not change, doing nothing on {} {}.", id, mail);
			return ImapAck.create(current.version, mail.imapUid);
		}
	}

	private String headersString(MailboxItem value) {
		StringBuilder ret = new StringBuilder();
		value.body.headers.stream().sorted((h1, h2) -> h1.name.compareTo(h2.name))
				.forEach(h -> ret.append(h.name).append(':').append(String.join(",", h.values)).append("\n"));
		return ret.toString();
	}

	private ImapAck mailRewrite(ItemValue<MailboxItem> current, MailboxItem newValue) {
		logger.info("Full EML rewrite expected with subject '{}'", newValue.body.subject);
		newValue.body.date = newValue.body.headers.stream()
				.filter(header -> header.name.equals(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE)).findAny()
				.map(h -> new Date(Long.parseLong(h.firstValue()))).orElse(current.value.body.date);
		Part currentStruct = current.value.body.structure;
		Part expectedStruct = newValue.body.structure;
		if (logger.isDebugEnabled()) {
			logger.debug("Shoud go from:\n{} to\n{}", JsonUtils.asString(currentStruct),
					JsonUtils.asString(expectedStruct));
		}
		PartsWalker<Object> walker = new PartsWalker<>(null);
		CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
		AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>(root);
		walker.visit((Object c, Part p) -> {
			logger.debug("Prepare for part @ {}", p.address);
			if (p.address != null && isImapAddress(p.address)) {
				logger.debug("*** preload part {}", p.address);
				String replacedPartUid = UUID.randomUUID().toString();
				File output = partFile(replacedPartUid);
				ref.set(ref.get().thenCompose(v -> {
					logger.debug("Fetching {} part {}...", current.value.imapUid, p.address);
					CompletableFuture<Void> sinkProm = sink(current.value.imapUid, p.address, p.encoding,
							output.toPath());
					p.address = replacedPartUid;
					return ThreadContextHelper.inWorkerThread(sinkProm);
				}));
			}
		}, expectedStruct);

		try {
			ref.get().get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.TIMEOUT);
		} catch (InterruptedException | ExecutionException e1) {
			throw new ServerFault(e1);
		}

		SizedStream updatedEml = createEmlStructure(current.internalId, current.value.body.guid, newValue.body);
		CompletableFuture<ItemChange> completion = ReplicationEvents.onRecordChanged(mailboxUniqueId,
				current.value.imapUid);

		int appended = imapContext.withImapClient(sc -> {

			List<String> allFlags = newValue.flags.stream().map(item -> item.flag).collect(Collectors.toList());

			int newUid = sc.append(imapFolder, updatedEml.input, FlagsList.of(allFlags), newValue.body.date);

			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			fl.add(Flag.SEEN);
			logger.debug("Marking the previous one uid:{} as deleted.", current.value.imapUid);
			try {
				List<Integer> imapUids = Arrays.asList((int) current.value.imapUid);
				boolean selected = sc.select(imapFolder);
				boolean done = sc.uidStore(imapUids, fl, true);
				sc.uidExpunge(imapUids);
				logger.debug("After store => selected: {}, done: {} ", selected, done);
				return newUid;
			} catch (IMAPException ie) {
				throw new ServerFault(ie);
			}

		});
		logger.info("Waiting for old imap uid {} to be updated, the new one is {}...", current.value.imapUid, appended);
		try {
			ItemChange change = completion.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			return ImapAck.create(change.version, appended);
		} catch (TimeoutException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.TIMEOUT);
		} catch (InterruptedException | ExecutionException e) {
			throw new ServerFault(e);
		}
	}

	private boolean isImapAddress(String address) {
		return address.equals("TEXT") || CharMatcher.inRange('0', '9').or(CharMatcher.is('.')).matchesAllOf(address);
	}

	private SizedStream createEmlStructure(long id, String previousBody, MessageBody body) {
		Part structure = body.structure;
		String sid = this.context.getSecurityContext().getSessionId();
		if (structure.mime.equals("message/rfc822")) {
			return EmlBuilder.inputStream(id, previousBody, body.date, structure, container.owner, sid);
		} else {
			try {
				body.headers.add(Header.create(MailApiHeaders.X_BM_INTERNAL_ID,
						container.owner + "#" + InstallationId.getIdentifier() + ":" + id));
				if (previousBody != null) {
					body.headers.add(Header.create(MailApiHeaders.X_BM_PREVIOUS_BODY, previousBody));
				}
				try (Message msg = EmlBuilder.of(body, sid)) {
					return Mime4JHelper.asSizedStream(msg);
				}
			} catch (

			ServerFault sf) {
				throw sf;
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}
	}

	@Override
	public ImapItemIdentifier create(MailboxItem value) {
		rbac.check(Verb.Write.name());
		IOfflineMgmt offlineApi = context.provider().instance(IOfflineMgmt.class, imapContext.user.domainUid,
				imapContext.user.uid);
		IdRange alloc = offlineApi.allocateOfflineIds(1);
		return create(alloc.globalCounter, value);
	}

	@Override
	public ImapAck createById(long id, MailboxItem value) {
		rbac.check(Verb.Write.name());
		ImapItemIdentifier itemIdentifier = create(id, value);
		return ImapAck.create(itemIdentifier.version, itemIdentifier.imapUid);
	}

	private ImapItemIdentifier create(long id, MailboxItem value) {
		logger.debug("create {}", id);
		try {
			return (ImapItemIdentifier) createAsync(id, value).get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException to) {
			throw new ServerFault("Create timed out", ErrorCode.TIMEOUT);
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private CompletableFuture<ItemIdentifier> createAsync(long id, MailboxItem value) {
		logger.info("create 'draft' {}", id);

		ItemValue<MailboxItem> existingItem = getCompleteById(id);
		if (existingItem != null) {
			long existingRefreshDate = existingItem.value.body.date.getTime();
			Optional<Header> newRefreshDate = value.body.headers.stream()
					.filter(header -> header.name.equals(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE)).findAny();
			if (newRefreshDate.isPresent()) {
				if (existingRefreshDate == Long.parseLong(newRefreshDate.get().firstValue())) {
					return CompletableFuture.completedFuture(
							ImapItemIdentifier.of(existingItem.value.imapUid, id, existingItem.version));
				}
			}
			CompletableFuture<ItemIdentifier> ret = new CompletableFuture<>();
			ret.completeExceptionally(new ServerFault("Item " + id
					+ " has been submitted for creation, but already exists having a different version or refresh header",
					ErrorCode.ALREADY_EXISTS));
			return ret;
		}

		SizedStream sizedStream = createEmlStructure(id, null, value.body);
		CompletableFuture<ItemChange> completion = ReplicationEvents.onRecordCreate(mailboxUniqueId, id);

		int addedUid = imapContext.withImapClient(sc -> {
			FlagsList fl = new FlagsList();
			value.flags.forEach(f -> {
				if (f.equals(MailboxItemFlag.System.Answered.value())) {
					fl.add(Flag.ANSWERED);
				} else if (f.equals(MailboxItemFlag.System.Deleted.value())) {
					fl.add(Flag.DELETED);
				} else if (f.equals(MailboxItemFlag.System.Draft.value())) {
					fl.add(Flag.DRAFT);
				} else if (f.equals(MailboxItemFlag.System.Flagged.value())) {
					fl.add(Flag.FLAGGED);
				} else if (f.equals(MailboxItemFlag.System.Seen.value())) {
					fl.add(Flag.SEEN);
				} else if (f.flag.equals("$Forwarded")) {
					fl.add(Flag.FORWARDED);
				}
			});
			logger.debug("Append {}bytes EML into {}", sizedStream.size, imapFolder);
			int added = sc.append(imapFolder, sizedStream.input, fl, value.body.date);
			logger.debug("Added IMAP UID: {} with date {}", added, value.body.date);
			return added;
		});
		if (addedUid > 0) {
			return completion.thenApply(change -> {
				logger.warn("**** CreateById of item {}, latency: {}ms.", change.internalId, change.latencyMs);
				return ImapItemIdentifier.of(addedUid, id, change.version);
			});
		} else {
			CompletableFuture<ItemIdentifier> ret = new CompletableFuture<>();
			ret.completeExceptionally(new ServerFault("Failed to add message in " + imapFolder));
			return ret;
		}
	}

	public List<ItemIdentifier> multiCreate(List<MailboxItem> items) {
		IOfflineMgmt offlineApi = context.provider().instance(IOfflineMgmt.class, imapContext.user.domainUid,
				imapContext.user.uid);
		int total = items.size();
		CompletableFuture<?>[] allOps = new CompletableFuture[total];
		IdRange alloc = offlineApi.allocateOfflineIds(total);
		ItemIdentifier[] ret = new ItemIdentifier[total];
		int i = 0;
		for (MailboxItem mi : items) {
			final int slot = i++;
			allOps[slot] = createAsync(alloc.globalCounter++, mi).thenAccept(ii -> ret[slot] = ii);
		}
		try {
			CompletableFuture.allOf(allOps).get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.TIMEOUT);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return Arrays.asList(ret);

	}

	@Override
	public List<ItemValue<MailboxItem>> multipleById(List<Long> ids) {
		if (ids.size() > 500) {
			throw new ServerFault("multipleById is limited to 500 ids per-call, you asked for " + ids.size());
		}
		rbac.check(Verb.Read.name());
		List<ItemValue<MailboxRecord>> records = storeService.getMultipleById(ids);
		List<String> bodiesToLoad = records.stream().map(iv -> iv.value.messageBody).distinct()
				.collect(Collectors.toList());

		Map<String, MessageBody> bodiesByGuid;
		try {
			bodiesByGuid = bodyStore.multiple(bodiesToLoad).stream().collect(Collectors.toMap(mb -> mb.guid, mb -> mb));
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), e);
		}

		return records.stream().map(v -> {
			ItemValue<MailboxItem> adapted = adapt(v);
			adapted.value.body = bodiesByGuid.get(v.value.messageBody);
			if (adapted.value.body == null) {
				logger.debug("message {} has no body. item uid {}, imap uid {}", v.value.messageBody, v.uid,
						v.value.imapUid);
				return null;
			}

			if (v.value.internalDate != null) {
				adapted.value.body.date = v.value.internalDate;
			}

			return adapted;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private List<ItemValue<MailboxItem>> multipleByIdWithoutBody(List<Long> ids) {
		return storeService.getMultipleById(ids).stream().map(this::adapt).collect(Collectors.toList());
	}

	public ItemIdentifier unexpunge(long itemId) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxRecord> item = storeService.get(itemId, null);
		if (item == null) {
			throw ServerFault.notFound("itemId " + itemId + " not found for unexpunge");
		}
		long imapUid = item.value.imapUid;
		InputStream directRead = fetchCompleteOIO(imapUid);
		CompletableFuture<Long> completion = ReplicationEvents.onMailboxChanged(mailboxUniqueId);
		int readded = imapContext.withImapClient(sc -> {
			int newUid = sc.append(imapFolder, directRead, new FlagsList());
			logger.debug("Previous body re-injected in {} with imapUid {}", imapFolder, newUid);
			return newUid;
		});
		if (readded > 0) {
			try {
				return completion.thenApply(version -> {
					try {
						RecordID itemRec = recordStore.identifiers(readded).iterator().next();
						return new ItemIdentifier(null, itemRec.itemId, version);
					} catch (SQLException e) {
						throw ServerFault.sqlFault(e);
					}
				}).get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new ServerFault(e.getMessage(), ErrorCode.TIMEOUT);
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		} else {
			throw new ServerFault("Failed to re-add message " + item);
		}
	}

	@Override
	public Stream fetchComplete(long imapUid) {
		rbac.check(Verb.Read.name());
		return super.fetchComplete(imapUid);
	}

	@Override
	public Stream fetch(long imapUid, String address, String encoding, String mime, String charset, String filename) {
		rbac.check(Verb.Read.name());

		if (!isImapAddress(address)) {
			return tmpPartFetch(imapUid, address);
		}
		ReadStream<Buffer> partContent = imapFetch(imapUid, address, encoding);
		return VertxStream.stream(partContent, mime, charset, filename);
	}

	private Stream tmpPartFetch(long imapUid, String address) {
		File tmpPart = partFile(address);
		if (!tmpPart.exists()) {
			throw new ServerFault("Trying to fetch a tmp part which doesnt exist");
		}
		// temporary parts are already decoded because getForUpdate already did it
		return VertxStream.stream(
				VertxPlatform.getVertx().fileSystem().openBlocking(tmpPart.getAbsolutePath(), new OpenOptions()));
	}

	private ReadStream<Buffer> imapFetch(long imapUid, String address, String encoding) {
		return imapContext.withImapClient(sc -> {
			if (sc.select(imapFolder)) {
				ByteBuf buf = Unpooled.buffer();
				try (IMAPByteSource fetched = sc.uidFetchPart((int) imapUid, address);
						InputStream raw = fetched.source().openBufferedStream();
						ByteBufOutputStream out = new ByteBufOutputStream(buf)) {
					ByteStreams.copy(decoded(raw, encoding), out);
				}
				return new BufferReadStream(Buffer.buffer(buf));
			} else {
				return new EmptyStream();
			}
		});
	}

	private InputStream decoded(InputStream in, String encoding) {
		if (encoding == null) {
			return in;
		}
		switch (encoding.toLowerCase()) {
		case "base64":
			return new Base64InputStream(in, false);
		case "quoted-printable":
			return new QuotedPrintableInputStream(in, false);
		case "uuencode":
			return new UUDecoderStream(in, true, true);
		default:
			return in;
		}
	}

	private CompletableFuture<Void> sink(long imapUid, String address, String encoding, Path out) {
		return imapContext.withImapClient(sc -> {
			if (sc.select(imapFolder)) {
				IMAPByteSource fetched = sc.uidFetchPart((int) imapUid, address);
				try (InputStream raw = fetched.source().openBufferedStream();
						OutputStream outStream = Files.newOutputStream(out)) {
					ByteStreams.copy(decoded(raw, encoding), outStream);
				}
			}
			return CompletableFuture.completedFuture(null);
		});
	}

	@Override
	public String uploadPart(Stream part) {
		rbac.check(Verb.Write.name());
		long time = System.currentTimeMillis();
		String addr = UUID.randomUUID().toString();
		logger.debug("[{}] Upload starts {}...", addr, part);
		try (ReadInputStream ri = new ReadInputStream(VertxStream.read(part));
				OutputStream out = Files.newOutputStream(partFile(addr).toPath())) {
			ByteStreams.copy(ri, out);
			time = System.currentTimeMillis() - time;
			logger.info("[{}] Upload tooks {}ms", addr, time);
			return addr;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void removePart(String partId) {
		rbac.check(Verb.Read.name());
		try {
			Files.deleteIfExists(partFile(partId).toPath());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private File partFile(String partId) {
		String sid = this.context.getSecurityContext().getSessionId();
		return new File(Bodies.getFolder(sid), partId + ".part");
	}

	private Ack doImapCommand(String imapCommand) {
		CompletableFuture<Long> repEvent = ReplicationEvents.onMailboxChanged(mailboxUniqueId);
		imapContext.withImapClient(sc -> {
			boolean select = sc.select(imapFolder);
			if (!select) {
				logger.error("Failed to select '{}'", imapFolder);
				return null;
			}
			TaggedResult result = sc.tagged(imapCommand);
			logger.debug("{}, Unseen updates ok ? {}", imapCommand, result.isOk());
			if (!result.isOk()) {
				for (int i = 0; i < result.getOutput().length; i++) {
					logger.error(result.getOutput()[i]);
				}
			}
			return null;
		});
		try {
			Long v = repEvent.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			return Ack.create(v);
		} catch (TimeoutException e) {
			throw new ServerFault(
					"TimeOut running '" + imapCommand + "' in folder " + imapFolder + " for " + imapContext.latd,
					ErrorCode.TIMEOUT);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public List<Long> unreadItems() {
		rbac.check(Verb.Read.name());
		List<ImapBinding> unreadBindings = Collections.emptyList();
		try {
			unreadBindings = recordStore.unreadItems();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return unreadBindings.stream().map(b -> b.itemId).collect(Collectors.toList());
	}

	@Override
	public List<Long> recentItems(Date d) {
		rbac.check(Verb.Read.name());
		List<ImapBinding> recentBindings = Collections.emptyList();
		try {
			recentBindings = recordStore.recentItems(d);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return recentBindings.stream().map(b -> b.itemId).collect(Collectors.toList());
	}

	@Override
	public void multipleDeleteById(List<Long> ids) {
		if (ids.isEmpty()) {
			logger.debug("ids list is empty, nothing to delete");
			return;
		}

		List<ItemValue<MailboxItem>> records = multipleByIdWithoutBody(ids);

		List<String> uids = records.stream().filter(r -> !r.flags.contains(ItemFlag.Deleted))
				.map(r -> Long.toString(r.value.imapUid)).collect(Collectors.toList());

		if (uids.isEmpty()) {
			logger.debug("filtered ids list is empty, nothing to delete");
			return;
		}

		logger.info("Delete {} records in {}", uids.size(), imapFolder);
		CompletableFuture<ItemChange> repEvent = ReplicationEvents.onRecordUpdate(mailboxUniqueId,
				Long.parseLong(uids.get(0)));

		long time = System.currentTimeMillis();
		addFlagsImapCommand(uids, Flag.DELETED.toString());
		time = System.currentTimeMillis() - time;
		try {
			ItemChange change = repEvent.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			logger.debug("Delete {} items with a latency of {}ms. (imap time: {}ms)", ids.size(), change.latencyMs,
					time);
		} catch (TimeoutException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.TIMEOUT);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

	}

	@Override
	public Ack addFlag(FlagUpdate flagUpdate) {
		rbac.check(Verb.Write.name());
		List<String> imapUidsToMark = multipleByIdWithoutBody(flagUpdate.itemsId).stream()
				.filter(item -> !item.value.flags.contains(flagUpdate.mailboxItemFlag))
				.map(item -> Long.toString(item.value.imapUid)).collect(Collectors.toList());

		return addFlagsImapCommand(imapUidsToMark, flagUpdate.mailboxItemFlag.flag);
	}

	@Override
	public Ack deleteFlag(FlagUpdate flagUpdate) {
		rbac.check(Verb.Write.name());
		List<String> imapUidsToMark = multipleByIdWithoutBody(flagUpdate.itemsId).stream()
				.filter(item -> item.value.flags.contains(flagUpdate.mailboxItemFlag))
				.map(item -> Long.toString(item.value.imapUid)).collect(Collectors.toList());

		return removeFlagsImapCommand(imapUidsToMark, flagUpdate.mailboxItemFlag.flag);
	}

	private Ack updateFlagsImapCommand(String prefix, List<String> imapUids, String... flags) {
		if (imapUids.isEmpty()) {
			return Ack.create(0L);
		}
		StringBuilder cmd = new StringBuilder("UID STORE ");
		cmd.append(String.join(",", imapUids) + " ");
		cmd.append(prefix + "FLAGS.SILENT (" + String.join(" ", flags) + ")");
		return doImapCommand(cmd.toString());
	}

	private Ack removeFlagsImapCommand(List<String> imapUids, String... flags) {
		return updateFlagsImapCommand("-", imapUids, flags);
	}

	private Ack addFlagsImapCommand(List<String> imapUids, String... flags) {
		return updateFlagsImapCommand("+", imapUids, flags);
	}

	private Ack overwriteFlagsImapCommand(List<String> imapUids, String... flags) {
		return updateFlagsImapCommand("", imapUids, flags);
	}

	@Override
	public ItemValue<MailboxItem> getForUpdate(long id) {
		rbac.check(Verb.Read.name());

		ItemValue<MailboxRecord> record = storeService.get(id, null);

		long imapUid = record.value.imapUid;

		String bodyGuid = record.value.messageBody;
		MessageBody body;
		try {
			body = bodyStore.get(bodyGuid);
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), e);
		}

		ItemValue<MailboxItem> adapted = adapt(record);
		adapted.value.body = body;

		logger.debug("Decomposing parts into tmp files for EML (id=" + id + ", imapUid=" + imapUid + ")");
		PartsWalker<Object> walker = new PartsWalker<>(null);
		CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
		AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>(root);
		walker.visit((Object c, Part p) -> {
			if (!p.mime.startsWith("multipart/")) {
				String replacedPartUid = UUID.randomUUID().toString();
				File output = partFile(replacedPartUid);
				ref.set(ref.get().thenCompose(v -> {
					logger.info("Fetching {} part {}...", imapUid, p.address);
					CompletableFuture<Void> sinkProm = sink(imapUid, p.address, p.encoding, output.toPath());
					p.address = replacedPartUid;
					return ThreadContextHelper.inWorkerThread(sinkProm);
				}));
			}
		}, adapted.value.body.structure);

		try {
			ref.get().get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.TIMEOUT);
		} catch (InterruptedException | ExecutionException e1) {
			throw new ServerFault(e1);
		}

		return adapted;
	}

}
