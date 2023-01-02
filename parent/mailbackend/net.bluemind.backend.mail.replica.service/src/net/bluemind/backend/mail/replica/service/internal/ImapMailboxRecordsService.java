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
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxItems;
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
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IMailboxRecordExpunged;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.utils.ReadInputStream;
import net.bluemind.core.rest.vertx.BufferReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.ThreadContextHelper;
import net.bluemind.delivery.conversationreference.api.IConversationReference;
import net.bluemind.imap.vertx.stream.EmptyStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.Mime4JHelper.HashedBuffer;
import net.bluemind.mime4j.common.OffloadedBodyFactory;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class ImapMailboxRecordsService extends BaseMailboxRecordsService implements IMailboxItems {

	private static final Logger logger = LoggerFactory.getLogger(ImapMailboxRecordsService.class);
	public static final Integer DEFAULT_TIMEOUT = 18; // sec
	private final String imapFolder;
	private final Namespace namespace;
	private final MessageBodyStore bodyStore;
	private final Supplier<IDbMailboxRecords> writeDelegate;
	private final Supplier<IDbByContainerReplicatedMailboxes> foldersWriteDelegate;
	private long folderItemId;

	public ImapMailboxRecordsService(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService) {
		super(cont, context, mailboxUniqueId, recordStore, storeService, new ReplicasStore(ds));
		SubtreeLocation recordsLocation = optRecordsLocation
				.orElseThrow(() -> new ServerFault("Missing subtree location"));

		this.imapFolder = recordsLocation.imapPath(context);
		this.namespace = recordsLocation.namespace();
		this.folderItemId = recordsLocation.folderItemId;

		logger.debug("namespace {}, subtree {}", namespace, recordsLocation);

		bodyStore = new MessageBodyStore(ds);
		this.writeDelegate = Suppliers
				.memoize(() -> context.provider().instance(IDbMailboxRecords.class, mailboxUniqueId));
		this.foldersWriteDelegate = Suppliers.memoize(() -> context.provider()
				.instance(IDbByContainerReplicatedMailboxes.class, recordsLocation.subtreeContainer));
	}

	public String imapFolder() {
		return imapFolder;
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
		this.multipleDeleteById(Arrays.asList(id));
	}

	@Override
	public void expunge() {
		IDbMailboxRecords writer = writeDelegate.get();
		List<Long> toExpunge = writer.imapIdSet("1:*", "+deleted");
		for (List<Long> slice : Lists.partition(toExpunge, 1024)) {
			List<MailboxRecord> recs = writer.slice(slice).stream().map(iv -> iv.value).collect(Collectors.toList());// NOSONAR
			for (MailboxRecord item : recs) {
				item.internalFlags.add(InternalFlag.expunged);
			}
			writer.updates(recs);
		}
		logger.info("Expunged {} record(s)", toExpunge.size());
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
			IDbMailboxRecords writer = writeDelegate.get();
			MailboxRecord toUpd = writer.getCompleteById(id).value;
			toUpd.flags = mail.flags;
			Ack updAck = writer.updates(Collections.singletonList(toUpd));
			return ImapAck.create(updAck.version, toUpd.imapUid);
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
		logger.info("[{}] EML rewrite expected with subject '{}'", mailboxUniqueId, newValue.body.subject);
		newValue.body.date = newValue.body.headers.stream()
				.filter(header -> header.name.equals(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE)).findAny()
				.map(h -> new Date(Long.parseLong(h.firstValue()))).orElse(current.value.body.date);
		Part currentStruct = current.value.body.structure;
		Part expectedStruct = newValue.body.structure;
		if (logger.isDebugEnabled()) {
			logger.debug("Shoud go from:\n{} to\n{}", JsonUtils.asString(currentStruct),
					JsonUtils.asString(expectedStruct));
		}
		decomposeToTempParts(current.value.imapUid, expectedStruct).orTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS).join();

		HashedBuffer sizedStream = createEmlStructure(current.internalId, current.value.body.guid, newValue.body);

		int messageMaxSize = LocalSysconfCache.get().integerValue(SysConfKeys.message_size_limit.name(),
				10 * 1024 * 1024);
		if (sizedStream.nettyBuffer().readableBytes() > messageMaxSize) {
			String errorMsg = "Rewritten Eml exceeds max message size (so it has not been submitted to Cyrus).";
			throw new ServerFault(errorMsg, ErrorCode.ENTITY_TOO_LARGE);
		}

		String partition = CyrusPartition.forServerAndDomain(DataSourceRouter.location(context, container.uid),
				container.domainUid).name;
		IDbMessageBodies bodiesWrite = context.provider().instance(IDbMessageBodies.class, partition);
		bodiesWrite.create(sizedStream.sha1(), VertxStream.stream(Buffer.buffer(sizedStream.nettyBuffer())));

		AppendTx rewriteTx = foldersWriteDelegate.get().prepareAppend(folderItemId, 1);

		MailboxRecord mr = new MailboxRecord();
		mr.flags = newValue.flags;
		mr.imapUid = rewriteTx.imapUid;
		mr.lastUpdated = new Date(rewriteTx.internalStamp);
		mr.internalDate = newValue.body.date != null ? newValue.body.date : mr.lastUpdated;
		mr.messageBody = sizedStream.sha1();
		mr.conversationId = conversationId(sizedStream);

		IDbMailboxRecords recWriter = writeDelegate.get();
		Ack upd = recWriter.updateById(current.internalId, mr);
		return ImapAck.create(upd.version, mr.imapUid);
	}

	private Long conversationId(HashedBuffer mb) {
		IConversationReference conversationReferenceApi = context.provider().instance(IConversationReference.class,
				container.domainUid, container.owner);
		return conversationReferenceApi.lookup(mb.messageId(), mb.refs());
	}

	private boolean isImapAddress(String address) {
		return address.equals("TEXT") || address.equals("HEADER")
				|| CharMatcher.inRange('0', '9').or(CharMatcher.is('.')).matchesAllOf(address);
	}

	private HashedBuffer createEmlStructure(long id, String previousBody, MessageBody body) {
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
					return Mime4JHelper.mmapedEML(msg);
				}
			} catch (ServerFault sf) {
				throw sf;
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}
	}

	@Override
	public ImapItemIdentifier create(MailboxItem value) {
		rbac.check(Verb.Write.name());
		IOfflineMgmt offlineApi = context.provider().instance(IOfflineMgmt.class, container.domainUid, container.owner);
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
			return createAsync(id, value);
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private ImapItemIdentifier createAsync(long id, MailboxItem value) {
		logger.info("create 'draft' {}", id);

		ItemValue<MailboxItem> existingItem = getCompleteById(id);
		if (existingItem != null) {
			long existingRefreshDate = existingItem.value.body.date.getTime();
			Optional<Header> newRefreshDate = value.body.headers.stream()
					.filter(header -> header.name.equals(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE)).findAny();
			if (newRefreshDate.isPresent()
					&& existingRefreshDate == Long.parseLong(newRefreshDate.get().firstValue())) {
				return ImapItemIdentifier.of(existingItem.value.imapUid, id, existingItem.version);
			}
			throw new ServerFault("Item " + id
					+ " has been submitted for creation, but already exists having a different version or refresh header",
					ErrorCode.ALREADY_EXISTS);
		}

		HashedBuffer sizedStream = createEmlStructure(id, null, value.body);

		String partition = CyrusPartition.forServerAndDomain(DataSourceRouter.location(context, container.uid),
				container.domainUid).name;
		IDbMessageBodies bodiesWrite = context.provider().instance(IDbMessageBodies.class, partition);
		bodiesWrite.create(sizedStream.sha1(), VertxStream.stream(Buffer.buffer(sizedStream.nettyBuffer())));

		AppendTx unexpTx = foldersWriteDelegate.get().prepareAppend(folderItemId, 1);

		MailboxRecord mr = new MailboxRecord();
		mr.flags = value.flags;
		mr.imapUid = unexpTx.imapUid;
		mr.lastUpdated = new Date(unexpTx.internalStamp);
		mr.internalDate = value.body.date != null ? value.body.date : mr.lastUpdated;
		mr.messageBody = sizedStream.sha1();
		mr.conversationId = conversationId(sizedStream);
		IDbMailboxRecords recWriter = writeDelegate.get();
		Ack ack = recWriter.createById(id, mr);

		return ImapItemIdentifier.of(mr.imapUid, id, ack.version);
	}

	public List<ItemIdentifier> multiCreate(List<MailboxItem> items) {
		IOfflineMgmt offlineApi = context.provider().instance(IOfflineMgmt.class, container.domainUid, container.owner);
		int total = items.size();
		IdRange alloc = offlineApi.allocateOfflineIds(total);
		ItemIdentifier[] ret = new ItemIdentifier[total];
		int i = 0;
		for (MailboxItem mi : items) {
			final int slot = i++;
			ret[slot] = createAsync(alloc.globalCounter++, mi);
		}
		return Arrays.asList(ret);
	}

	@Override
	public List<ItemValue<MailboxItem>> multipleGetById(List<Long> ids) {
		if (ids.size() > 500) {
			throw new ServerFault("multipleGetById is limited to 500 ids per-call, you asked for " + ids.size());
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

	public ItemIdentifier unexpunge(long itemId) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxRecord> item = storeService.get(itemId, null);
		if (item == null) {
			throw ServerFault.notFound("itemId " + itemId + " not found for unexpunge");
		}

		AppendTx unexpTx = foldersWriteDelegate.get().prepareAppend(container.id, 1);
		MailboxRecord freshRec = item.value.copy();
		freshRec.imapUid = unexpTx.imapUid;
		ItemVersion itemRec = writeDelegate.get().create(unexpTx.imapUid + ".", freshRec);
		ItemValue<MailboxItem> fullFresh = getCompleteById(itemRec.id);
		IMailboxRecordExpunged expungeApi = context.provider().instance(IMailboxRecordExpunged.class,
				IMailReplicaUids.uniqueId(container.uid));
		expungeApi.delete(itemId);
		return fullFresh.identifier();

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
			return tmpPartFetch(address);
		}
		ReadStream<Buffer> partContent = imapFetch(imapUid, address);
		return VertxStream.stream(partContent, mime, charset, filename);
	}

	private Stream tmpPartFetch(String address) {
		File tmpPart = partFile(address);
		if (!tmpPart.exists()) {
			throw new ServerFault("Trying to fetch tmp part " + address + " which doesnt exist");
		}
		// temporary parts are already decoded because getForUpdate already did it
		AsyncFile openBlocking = VertxPlatform.getVertx().fileSystem().openBlocking(tmpPart.getAbsolutePath(),
				new OpenOptions());
		return VertxStream.stream(openBlocking, v -> {
			try {
				openBlocking.close();
			} catch (IllegalStateException e) {
			}
		});
	}

	private ReadStream<Buffer> imapFetch(long imapUid, String address) {
		InputStream stream = fetchCompleteOIO(imapUid);
		logger.info("Got stream {} for {}", stream, imapUid);
		try (Message parsed = Mime4JHelper.parse(stream, new OffloadedBodyFactory())) {
			logger.info("Parsed {} as {}", stream, parsed);
			SingleBody body = null;
			if (parsed.isMultipart()) {
				Multipart mp = (Multipart) parsed.getBody();
				body = Mime4JHelper.expandTree(mp.getBodyParts()).stream()
						.filter(ae -> address.equals(ae.getMimeAddress())).findAny()
						.map(ae -> (SingleBody) ae.getBody()).orElseGet(() -> {
							logger.warn("Part {} not found for imapUid {}", address, imapUid);
							return null;
						});
			} else if (address.equals("1") || address.equals("TEXT")) {
				body = (SingleBody) parsed.getBody();
			}
			if (body == null) {
				logger.warn("body not found for uid {} part {}", imapUid, address);
				return new EmptyStream();
			} else {
				ByteBuf buf = Unpooled.buffer();
				logger.info("Found body {}", body);
				try (InputStream part = body.getInputStream();
						InputStream decIfNeeded = dec(body, part);
						ByteBufOutputStream out = new ByteBufOutputStream(buf)) {
					long copied = ByteStreams.copy(decIfNeeded, out);
					logger.info("Copied {} byte(s) for uid {} part {}", copied, imapUid, address);
				}
				logger.info("Returning {}", buf);
				return new BufferReadStream(Buffer.buffer(buf));
			}

		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private InputStream dec(SingleBody body, InputStream part) {
		if ("uuencode".equalsIgnoreCase(body.getParent().getContentTransferEncoding())) {
			return new com.sun.mail.util.UUDecoderStream(part, true, true); // NOSONAR
		} else {
			return part;
		}
	}

	private CompletableFuture<Void> sink(long imapUid, String address, Path out) {
		ReadStream<Buffer> stream = imapFetch(imapUid, address);
		return GenericStream.asyncStreamToFile(stream, out.toFile(), StandardOpenOption.CREATE_NEW);
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

		rbac.check(Verb.Write.name());

		FlagUpdate flagUpdate = FlagUpdate.of(ids, MailboxItemFlag.System.Deleted.value());
		addFlag(flagUpdate);

	}

	@Override
	public Ack addFlag(FlagUpdate flagUpdate) {
		rbac.check(Verb.Write.name());

		return touchFlag(flagUpdate, rec -> {
			rec.value.flags.add(flagUpdate.mailboxItemFlag);
			if (MailboxItemFlag.System.Deleted.value().equals(flagUpdate.mailboxItemFlag)) {
				rec.value.internalFlags.add(InternalFlag.expunged);
			}
			return rec.value;
		});
	}

	@FunctionalInterface
	private interface FlagOperation {
		MailboxRecord touchFlags(WithId<MailboxRecord> mr);
	}

	private Ack touchFlag(FlagUpdate target, FlagOperation op) {
		rbac.check(Verb.Write.name());

		IDbMailboxRecords writer = writeDelegate.get();
		List<WithId<MailboxRecord>> slice = writer.slice(target.itemsId);
		if (slice.isEmpty()) {
			return Ack.create(0);
		}
		return writer.updates(slice.stream().map(op::touchFlags).toList());
	}

	@Override
	public Ack deleteFlag(FlagUpdate flagUpdate) {
		rbac.check(Verb.Write.name());
		return touchFlag(flagUpdate, rec -> {
			rec.value.flags.remove(flagUpdate.mailboxItemFlag);
			return rec.value;
		});
	}

	@Override
	public ItemValue<MailboxItem> getForUpdate(long id) {
		rbac.check(Verb.Read.name());

		ItemValue<MailboxRecord> mbRec = storeService.get(id, null);
		if (mbRec == null) {
			throw ServerFault.notFound("Record " + id + " not found in " + container.uid + " (aka " + imapFolder + ")");
		}

		long imapUid = mbRec.value.imapUid;

		String bodyGuid = mbRec.value.messageBody;
		MessageBody body;
		try {
			body = bodyStore.get(bodyGuid);
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), e);
		}

		ItemValue<MailboxItem> adapted = adapt(mbRec);
		adapted.value.body = body;

		CompletableFuture<Void> ref = decomposeToTempParts(imapUid, adapted.value.body.structure);

		ref.orTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS).join();

		return adapted;
	}

	/**
	 * Parts with imap addresses will be changed to temporary parts
	 * 
	 * @param imapUid
	 * @param rootPart
	 * @return
	 */
	private CompletableFuture<Void> decomposeToTempParts(long imapUid, Part rootPart) {
		logger.debug("Decomposing parts into tmp files for EML (imapUid={})", imapUid);
		PartsWalker<Object> walker = new PartsWalker<>(null);
		CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
		AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>(root);
		walker.visit((Object c, Part p) -> {
			if (p.address != null && isImapAddress(p.address) && !p.mime.startsWith("multipart/")) {
				String replacedPartUid = UUID.randomUUID().toString();
				File output = partFile(replacedPartUid);
				ref.set(ref.get().thenCompose(v -> {
					logger.info("Fetching {} part {}...", imapUid, p.address);
					CompletableFuture<Void> sinkProm = sink(imapUid, p.address, output.toPath());
					p.address = replacedPartUid;
					return ThreadContextHelper.inWorkerThread(sinkProm);
				}));
			}
		}, rootPart);
		return ref.get();
	}

}
