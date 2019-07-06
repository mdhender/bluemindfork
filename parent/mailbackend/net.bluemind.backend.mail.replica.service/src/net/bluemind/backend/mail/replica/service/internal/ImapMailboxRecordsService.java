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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MailboxItem.SystemFlag;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.SeenUpdate;
import net.bluemind.backend.mail.api.utils.PartsWalker;
import net.bluemind.backend.mail.parsing.EZInputStreamAdapter;
import net.bluemind.backend.mail.parsing.EmlBuilder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.backend.mail.replica.api.utils.UidRanges;
import net.bluemind.backend.mail.replica.api.utils.UidRanges.UidRange;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.backend.mail.replica.persistence.SeenOverlayStore;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.backend.mail.replica.service.ReplicationEvents.ItemChange;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.TaggedResult;
import net.bluemind.imap.vertx.ImapResponseStatus;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.cmd.AppendResponse;
import net.bluemind.imap.vertx.cmd.FetchResponse;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.Mime4JHelper.SizedStream;

public class ImapMailboxRecordsService extends BaseMailboxRecordsService implements IMailboxItems {

	private static final Logger logger = LoggerFactory.getLogger(ImapMailboxRecordsService.class);
	private final String imapFolder;
	private final ImapContext imapContext;
	private final SeenOverlayStore seenOverlays;
	private final Namespace namespace;
	private final MessageBodyStore bodyStore;

	public ImapMailboxRecordsService(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService) {
		super(cont, context, mailboxUniqueId, recordStore, storeService, new ReplicasStore(ds));
		SubtreeLocation recordsLocation = optRecordsLocation
				.orElseThrow(() -> new ServerFault("Missing subtree location"));

		this.imapFolder = recordsLocation.imapPath();
		this.namespace = recordsLocation.namespace();
		logger.debug("imapFolder is {}", imapFolder);

		this.imapContext = ImapContext.of(context);
		this.seenOverlays = new SeenOverlayStore(ds);
		bodyStore = new MessageBodyStore(ds);
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
		if (namespace == Namespace.shared) {
			try {
				SeenOverlay overlay = seenOverlays.byUser(imapContext.latd, mailboxUniqueId);
				logger.debug("Apply overlay {} to {}", overlay, adapted);
				if (overlay != null) {
					List<UidRange> ranges = UidRanges.from(overlay.seenUids);
					if (UidRanges.contains(ranges, adapted.value.imapUid)) {
						adapted.value.systemFlags.add(SystemFlag.seen);
					} else {
						adapted.value.systemFlags.remove(SystemFlag.seen);
					}
				}
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		return adapted;
	}

	@Override
	public void deleteById(long id) {
		rbac.check(Verb.Write.name());
		logger.info("Delete {}", id);
		ItemValue<MailboxItem> toDelete = getCompleteById(id);
		if (toDelete != null) {
			Collection<SystemFlag> curFlags = toDelete.value.systemFlags;
			if (!curFlags.contains(SystemFlag.deleted)) {
				curFlags.add(SystemFlag.deleted);
				curFlags.add(SystemFlag.seen);
				flagsUpdate(toDelete.value);
			}
		} else {
			logger.warn("Nothing to delete for id {} in {}.", id, imapFolder);
		}
	}

	@Override
	public void resync() {
		rbac.check(Verb.Write.name());
		long time = System.currentTimeMillis();
		Collection<Integer> imapUids = imapContext.withImapClient((sc, fast) -> {
			sc.select(imapFolder);
			return sc.uidSearch(new SearchQuery());
		});
		Set<Long> knownUids = imapUids.stream().map(Long::new).collect(Collectors.toSet());
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
		logger.info("Found {} extra record(s), {} unlinked record(s) before resync of {}", extraRecords.size(),
				unlinkedRecords.size(), imapFolder);
		if (!extraRecords.isEmpty()) {
			IDbMailboxRecords recsApi = context.provider().instance(IDbMailboxRecords.class,
					IMailReplicaUids.uniqueId(container.uid));
			List<MailboxRecord> batch = extraRecords.stream().map(iv -> {
				MailboxRecord ret = iv.value;
				ret.systemFlags.add(SystemFlag.deleted);
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
		logger.info("{} re-sync completed in {}ms.", imapFolder, time);
	}

	@Override
	public Ack updateById(long id, MailboxItem mail) {
		rbac.check(Verb.Write.name());
		if (mail.imapUid == 0) {
			logger.warn("Not updating {} with imapUid 0", id);
			return Ack.create(0L);
		}
		// has the flags changed ?
		ItemValue<MailboxItem> current = getCompleteById(id);
		if (current.value.otherFlags.contains("$MDNSent") && !mail.otherFlags.contains("$MDNSent")) {
			logger.info("cannot remove flag $MDNSent (on {})", id);
			mail.otherFlags.add("$MDNSent");
		}
		String curFlags = flagsString(current.value);
		String newFlags = flagsString(mail);
		boolean flagsChanged = !curFlags.equals(newFlags);
		String newSub = Optional.ofNullable(mail.body.subject).orElse("");
		String oldSub = current.value.body.subject;
		boolean subjectChanged = !newSub.equals(oldSub);
		String curHeaders = headersString(current.value);
		String newHeaders = headersString(mail);
		boolean headersChanged = !curHeaders.equals(newHeaders);
		logger.info("changes are flags:{}, subject:{}, headers:{}", flagsChanged, subjectChanged, headersChanged);
		if (subjectChanged || headersChanged) {
			// not complete yet
			return mailRewrite(current, mail);
		} else if (flagsChanged) {
			return flagsUpdate(mail);
		} else {
			logger.warn("Subject/Headers/Flags dit not change, doing nothing on {} {}.", id, mail);
			return Ack.create(current.version);
		}
	}

	private String headersString(MailboxItem value) {
		StringBuilder ret = new StringBuilder();
		value.body.headers.stream().sorted((h1, h2) -> h1.name.compareTo(h2.name))
				.forEach(h -> ret.append(h.name).append(':').append(String.join(",", h.values)).append("\n"));
		return ret.toString();
	}

	private Ack mailRewrite(ItemValue<MailboxItem> current, MailboxItem newValue) {
		logger.warn("Full EML rewrite expected with subject {}.", newValue.body.subject);
		newValue.body.date = current.value.body.date;
		Part currentStruct = current.value.body.structure;
		Part expectedStruct = newValue.body.structure;
		logger.info("Shoud go from:\n{} to\n{}", JsonUtils.asString(currentStruct), JsonUtils.asString(expectedStruct));
		PartsWalker<Object> walker = new PartsWalker<>(null);
		walker.visit((Object c, Part p) -> {
			logger.info("Prepare for part @ {}", p.address);
			if (isImapAddress(p.address)) {
				logger.info("*** preload part {}", p.address);
				ImapResponseStatus<FetchResponse> fetched = imapContext.withImapClient((sc, fast) -> {
					try {
						return fast.select(imapFolder)
								.thenCompose(selec -> fast.fetch(current.value.imapUid, p.address))
								.get(15, TimeUnit.SECONDS);
					} catch (TimeoutException e) {
						throw new ServerFault("Failed to fetch part " + p.address + " from current. Timeout occured");
					}

				});
				if (fetched.status != Status.Ok) {
					throw new ServerFault("Failed to fetch part " + p.address + " from current.");
				}
				String replacedPartUid = UUID.randomUUID().toString();
				File output = new File(Bodies.STAGING, replacedPartUid + ".part");
				try (OutputStream out = Files.newOutputStream(output.toPath());
						ByteBufInputStream in = new ByteBufInputStream(fetched.result.get().data, true)) {
					ByteStreams.copy(in, out);
					logger.info("YEAH Replaced part address from {} to {}", p.address, replacedPartUid);
					p.address = replacedPartUid;
				} catch (Exception e) {
					throw new ServerFault(e);
				}

			}
		}, expectedStruct);
		SizedStream updatedEml = createEmlStructure(current.internalId, current.value.body.guid, newValue.body);
		CompletableFuture<ItemChange> completion = ReplicationEvents.onRecordUpdate(mailboxUniqueId,
				current.value.imapUid);

		ImapResponseStatus<AppendResponse> appended = imapContext.withImapClient((sc, fast) -> {

			List<String> allFlags = new LinkedList<>();
			newValue.systemFlags.forEach(sf -> allFlags.add(sf.imapName));
			allFlags.addAll(newValue.otherFlags);
			ReadStream<VertxInputReadStream> asStream = new VertxInputReadStream(fast.vertx(), updatedEml.input);
			CompletableFuture<ImapResponseStatus<AppendResponse>> append = fast.append(imapFolder,
					current.value.body.date, allFlags, updatedEml.size, asStream);
			try {
				return append.thenCompose(appendResult -> {
					FlagsList fl = new FlagsList();
					fl.add(Flag.DELETED);
					fl.add(Flag.SEEN);
					logger.info("Marking the previous one uid:{} as deleted.", current.value.imapUid);
					try {
						List<Integer> imapUids = Arrays.asList((int) current.value.imapUid);
						boolean selected = sc.select(imapFolder);
						boolean done = sc.uidStore(imapUids, fl, true);
						sc.uidExpunge(imapUids);
						logger.info("After store => selected: {}, done: {} ", selected, done);
						return CompletableFuture.completedFuture(appendResult);
					} catch (IMAPException ie) {
						CompletableFuture<ImapResponseStatus<AppendResponse>> cf = new CompletableFuture<>();
						cf.completeExceptionally(ie);
						return cf;
					}
				}).get(15, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new ServerFault("Failed to append email. Timeout occured");
			}
		});
		logger.info("Waiting for old imap uid {} to be updated, the new one is {}...", current.value.imapUid,
				appended.result.map(r -> r.newUid).orElse(-1L));
		try {
			ItemChange change = completion.get(10, TimeUnit.SECONDS);
			return Ack.create(change.version);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ServerFault(e);
		}
	}

	private boolean isImapAddress(String address) {
		return CharMatcher.inRange('0', '9').or(CharMatcher.is('.')).matchesAllOf(address);
	}

	private Ack flagsUpdate(MailboxItem mail) {
		logger.info("***** flagsUpdate for {} in {}", mail.body.subject, imapFolder);
		CompletableFuture<ItemChange> repEvent = ReplicationEvents.onRecordUpdate(mailboxUniqueId, mail.imapUid);
		long time = System.currentTimeMillis();
		doImapFlagsUpdate(mail.imapUid, mail);
		time = System.currentTimeMillis() - time;
		try {
			ItemChange change = repEvent.get(10, TimeUnit.SECONDS);
			logger.info("Updated item with a latency of {}ms. (imap time: {}ms)", change.latencyMs, time);
			return Ack.create(change.version);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private SizedStream createEmlStructure(long id, String previousBody, MessageBody body) {
		Part structure = body.structure;
		if (structure.mime.equals("message/rfc822")) {
			return EmlBuilder.inputStream(id, previousBody, body.date, structure, container.owner);
		} else {
			try {
				body.headers.add(Header.create(MailApiHeaders.X_BM_INTERNAL_ID,
						container.owner + "#" + InstallationId.getIdentifier() + ":" + id));
				if (previousBody != null) {
					body.headers.add(Header.create(MailApiHeaders.X_BM_PREVIOUS_BODY, previousBody));
				}
				Message msg = EmlBuilder.of(body, container.owner);
				return Mime4JHelper.asSizedStream(msg);
			} catch (ServerFault sf) {
				throw sf;
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}
	}

	@Override
	public ItemIdentifier create(MailboxItem value) {
		rbac.check(Verb.Write.name());
		IOfflineMgmt offlineApi = context.provider().instance(IOfflineMgmt.class, imapContext.user.domainUid,
				imapContext.user.uid);
		IdRange alloc = offlineApi.allocateOfflineIds(1);
		return create(alloc.globalCounter, value);
	}

	@Override
	public Ack createById(long id, MailboxItem value) {
		rbac.check(Verb.Write.name());
		ItemIdentifier itemIdentifier = create(id, value);
		return Ack.create(itemIdentifier.version);
	}

	private ItemIdentifier create(long id, MailboxItem value) {
		logger.info("create 'draft' {}", id);
		CompletableFuture<ItemChange> completion = ReplicationEvents.onRecordCreate(mailboxUniqueId, id);

		int addedUid = imapContext.withImapClient((sc, fast) -> {
			FlagsList fl = new FlagsList();
			for (SystemFlag sf : value.systemFlags) {
				switch (sf) {
				case answered:
					fl.add(Flag.ANSWERED);
					break;
				case deleted:
					fl.add(Flag.DELETED);
					break;
				case draft:
					fl.add(Flag.DRAFT);
					break;
				case flagged:
					fl.add(Flag.FLAGGED);
					break;
				case seen:
					fl.add(Flag.SEEN);
					break;
				default:
					break;
				}
			}
			if (value.otherFlags.contains("$Forwarded")) {
				fl.add(Flag.FORWARDED);
			}
			// fast.append(imapFolder, value.body.date, flags, streamSize, eml)
			SizedStream sizedStream = createEmlStructure(id, null, value.body);
			logger.info("Append {}bytes EML into {}", sizedStream.size, imapFolder);
			int added = sc.append(imapFolder, sizedStream.input, fl, value.body.date);
			logger.info("Added IMAP UID: {} with date {}", added, value.body.date);
			return added;
		});
		if (addedUid > 0) {
			try {
				ItemChange change = completion.get(10, TimeUnit.SECONDS);
				logger.warn("**** CreateById of item {}, latency: {}ms.", change.internalId, change.latencyMs);
				return ItemIdentifier.of(null, id, change.version);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new ServerFault(e);
			}
		}

		throw new ServerFault("Failed to add message in " + imapFolder);
	}

	private void doImapFlagsUpdate(long imapUid, MailboxItem mail) {
		imapContext.withImapClient((sc, fast) -> {
			sc.select(imapFolder);
			String cmd = "UID STORE " + imapUid + " FLAGS.SILENT " + flagsString(mail);
			TaggedResult result = sc.tagged(cmd);
			logger.info("cmd: '{}' => result: {}", cmd, result.isOk());
			return null;
		});
	}

	private void doImapAddFlags(List<Long> uids, List<String> flags) {
		String set = uids.stream().map(Object::toString).collect(Collectors.joining(","));
		imapContext.withImapClient((sc, fast) -> {
			sc.select(imapFolder);
			String cmd = "UID STORE " + set + " +FLAGS.SILENT (" + String.join(" ", flags) + ")";
			TaggedResult result = sc.tagged(cmd);
			logger.info("cmd: '{}' => result: {}", cmd, result.isOk());
			return null;
		});
	}

	private String flagsString(MailboxItem mail) {
		StringBuilder sb = new StringBuilder("(");
		boolean first = true;
		for (SystemFlag f : mail.systemFlags) {
			if (first) {
				first = false;
			} else {
				sb.append(' ');
			}
			sb.append(f.imapName);
		}
		for (String f : mail.otherFlags) {
			if (first) {
				first = false;
			} else {
				sb.append(' ');
			}
			sb.append(f);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public List<ItemValue<MailboxItem>> multipleById(List<Long> ids) {
		rbac.check(Verb.Read.name());
		List<UidRange> rangesTmp = null;
		if (namespace == Namespace.shared) {
			try {
				SeenOverlay overlay = seenOverlays.byUser(imapContext.latd, mailboxUniqueId);
				if (overlay != null) {
					rangesTmp = UidRanges.from(overlay.seenUids);
				}
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		final List<UidRange> ranges = rangesTmp;
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
				logger.info("message {} has no body. item uid {}, imap uid {}", v.value.messageBody, v.uid,
						v.value.imapUid);
				return null;
			}
			if (v.value.internalDate != null) {
				adapted.value.body.date = v.value.internalDate;
			}

			if (namespace == Namespace.shared && ranges != null) {
				if (UidRanges.contains(ranges, adapted.value.imapUid)) {
					adapted.value.systemFlags.add(SystemFlag.seen);
				} else {
					adapted.value.systemFlags.remove(SystemFlag.seen);
				}
			}

			return adapted;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private List<ItemValue<MailboxItem>> multipleByIdWithoutBody(List<Long> ids) {
		List<UidRange> rangesTmp = null;
		if (namespace == Namespace.shared) {
			try {
				SeenOverlay overlay = seenOverlays.byUser(imapContext.latd, mailboxUniqueId);
				if (overlay != null) {
					rangesTmp = UidRanges.from(overlay.seenUids);
				}
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		final List<UidRange> ranges = rangesTmp;
		return storeService.getMultipleById(ids).stream().map(v -> {
			ItemValue<MailboxItem> adapted = adapt(v);
			if (namespace == Namespace.shared && ranges != null) {
				if (UidRanges.contains(ranges, adapted.value.imapUid)) {
					adapted.value.systemFlags.add(SystemFlag.seen);
				} else {
					adapted.value.systemFlags.remove(SystemFlag.seen);
				}
			}
			return adapted;
		}).collect(Collectors.toList());
	}

	@Override
	public Stream fetchComplete(long imapUid) {
		rbac.check(Verb.Read.name());
		return fetch(imapUid, "", null);
	}

	@Override
	public Stream fetch(long imapUid, String address, String encoding) {
		rbac.check(Verb.Read.name());
		ByteBuf downloaded = fetch(imapUid, address);
		Stream stream = null;
		if (encoding != null) {
			try (InputStream in = dec(downloaded, encoding)) {
				stream = VertxStream.stream(new Buffer(Unpooled.wrappedBuffer(ByteStreams.toByteArray(in))));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				stream = VertxStream.stream(new Buffer());
			}
		} else {
			stream = VertxStream.stream(new Buffer(downloaded));
		}
		return stream;
	}

	private InputStream dec(ByteBuf downloaded, String encoding) {
		InputStream ret = new ByteBufInputStream(downloaded, true);
		switch (encoding) {
		case "base64":
			ret = new Base64InputStream(ret);
			break;
		case "quoted-printable":
			ret = new QuotedPrintableInputStream(ret);
			break;
		}
		return ret;
	}

	private ByteBuf fetch(long imapUid, String address) {
		return imapContext.withImapClient((sc, fast) -> {
			try {
				ImapResponseStatus<FetchResponse> fetchResp = null;
				try {
					fetchResp = fast.select(imapFolder).thenCompose(selected -> {
						if (selected.status == Status.Ok) {
							return fast.fetch(imapUid, address);
						}
						logger.warn("Selection status is invalid for folder '{}'", imapFolder);
						ImapResponseStatus<FetchResponse> emptyResp = new ImapResponseStatus<>(Status.Ok,
								new FetchResponse(Unpooled.EMPTY_BUFFER));
						return CompletableFuture.completedFuture(emptyResp);
					}).get(15, TimeUnit.SECONDS);
				} catch (TimeoutException e) {
					throw new ServerFault("Failed to fetch " + imapUid + " .Timeout occured");
				}

				return fetchResp.result.get().data;
			} catch (CompletionException ce) {
				logger.error(ce.getMessage());
				return Unpooled.EMPTY_BUFFER;
			}
		});
	}

	@Override
	public String uploadPart(Stream part) {
		rbac.check(Verb.Write.name());
		String addr = UUID.randomUUID().toString();
		logger.info("[{}] Upload starts {}...", addr, part);
		CompletableFuture<Void> upload = EZInputStreamAdapter.consume(part, oioStream -> {
			logger.info("[{}] Got adapted stream {}", addr, oioStream);
			File output = new File(Bodies.STAGING, addr + ".part");
			try (OutputStream out = Files.newOutputStream(output.toPath())) {
				ByteStreams.copy(oioStream, out);
			} catch (Exception e) {
				throw new ServerFault(e);

			}
			return null;
		});
		try {
			upload.get(18, TimeUnit.SECONDS);
			return addr;
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void removePart(String partId) {
		rbac.check(Verb.Write.name());
		new File(Bodies.STAGING, partId + ".part").delete();
	}

	@Override
	public Ack updateSeens(List<SeenUpdate> updates) {
		rbac.check(Verb.Write.name());
		List<Long> seenMdn = new ArrayList<>(updates.size());
		List<Long> seen = new ArrayList<>(updates.size());
		List<Long> unseen = new ArrayList<>(updates.size());

		for (SeenUpdate su : updates) {
			if (su.mdnSent) {
				seenMdn.add(su.itemId);
			} else {
				if (su.seen) {
					seen.add(su.itemId);
				} else {
					unseen.add(su.itemId);
				}
			}
		}
		Ack version = Ack.create(0L);

		if (!seenMdn.isEmpty()) {
			List<ItemValue<MailboxItem>> toMark = multipleByIdWithoutBody(seenMdn);
			StringBuilder cmd = new StringBuilder("UID STORE ");
			StringBuilder uids = new StringBuilder();
			boolean first = true;
			int toUpdate = 0;
			for (ItemValue<MailboxItem> it : toMark) {
				if (it.value.systemFlags.contains(SystemFlag.seen) && it.value.otherFlags.contains("$MDNSent")) {
					logger.info("{} is already seen and $MDNSent", it);
					continue;
				}
				toUpdate++;
				if (first) {
					first = false;
				} else {
					uids.append(",");
				}
				uids.append(it.value.imapUid);
			}
			if (toUpdate > 0) {
				String allUids = uids.toString();
				cmd.append(allUids);
				cmd.append(" +FLAGS.SILENT (\\Seen $MDNSent)");
				String imapCommand = cmd.toString();
				version = doImapCommand(imapCommand);
			}
		}

		if (!seen.isEmpty()) {
			List<ItemValue<MailboxItem>> toMark = multipleByIdWithoutBody(seen);
			StringBuilder cmd = new StringBuilder("UID STORE ");
			StringBuilder uids = new StringBuilder();
			boolean first = true;
			int toUpdate = 0;
			for (ItemValue<MailboxItem> it : toMark) {
				if (it.value.systemFlags.contains(SystemFlag.seen)) {
					logger.info("{} is already seen", it);
					continue;
				}
				toUpdate++;
				if (first) {
					first = false;
				} else {
					uids.append(",");
				}
				uids.append(it.value.imapUid);
			}
			if (toUpdate > 0) {
				String allUids = uids.toString();
				cmd.append(allUids);
				cmd.append(" +FLAGS.SILENT (\\Seen)");
				String imapCommand = cmd.toString();
				version = doImapCommand(imapCommand);
			}
		}

		if (!unseen.isEmpty()) {
			List<ItemValue<MailboxItem>> toMark = multipleByIdWithoutBody(unseen);
			StringBuilder cmd = new StringBuilder("UID STORE ");
			boolean first = true;
			int toUpdate = 0;
			for (ItemValue<MailboxItem> it : toMark) {
				if (!it.value.systemFlags.contains(SystemFlag.seen)) {
					logger.info("{} is already unseen", it);
					continue;
				}
				toUpdate++;
				if (first) {
					first = false;
				} else {
					cmd.append(",");
				}
				cmd.append(it.value.imapUid);
			}
			if (toUpdate > 0) {
				cmd.append(" -FLAGS.SILENT (\\Seen)");
				String imapCommand = cmd.toString();
				version = doImapCommand(imapCommand);
			}
		}

		return version;
	}

	private Ack doImapCommand(String imapCommand) throws ServerFault {
		CompletableFuture<Long> repEvent = ReplicationEvents.onMailboxChanged(mailboxUniqueId);
		imapContext.withImapClient((sc, fast) -> {
			sc.select(imapFolder);
			TaggedResult ok = sc.tagged(imapCommand);
			logger.info("{}, Unseen updates ok ? {}", imapCommand, ok.isOk());
			return null;
		});
		try {
			Long v = repEvent.get(10, TimeUnit.SECONDS);
			return Ack.create(v);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private static final ItemFlagFilter UNREAD_NOT_DELETED = ItemFlagFilter.create().mustNot(ItemFlag.Deleted,
			ItemFlag.Seen);
	private static final ItemFlagFilter NOT_DELETED = ItemFlagFilter.create().mustNot(ItemFlag.Deleted);

	@Override
	public List<Long> unreadItems() {
		rbac.check(Verb.Read.name());
		List<ImapBinding> unreadBindings = Collections.emptyList();
		try {
			unreadBindings = recordStore.unreadItems();
			if (namespace == Namespace.shared) {
				SeenOverlay overlay = seenOverlays.byUser(imapContext.latd, mailboxUniqueId);
				if (overlay != null) {
					List<UidRange> ranges = UidRanges.from(overlay.seenUids);
					int sizeBefore = unreadBindings.size();
					unreadBindings = UidRanges.notInRange(ranges, unreadBindings);
					logger.info("Unread before overlay {}, after {}", sizeBefore, unreadBindings.size());
				}
			}
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
	public Count getPerUserUnread() {
		rbac.check(Verb.Read.name());
		if (namespace == Namespace.shared) {
			SeenOverlay overlay = null;
			try {
				overlay = seenOverlays.byUser(imapContext.latd, mailboxUniqueId);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
			if (overlay == null) {
				return Count.of(0);
			}
			Count total = count(NOT_DELETED);
			List<UidRange> ranges = UidRanges.from(overlay.seenUids);
			if (ranges.isEmpty()) {
				return total;
			} else {
				int value = total.total;
				for (UidRange r : ranges) {
					value = value - (int) r.size();
				}
				if (value < 0) {
					value = 0;
				}
				return Count.of(value);
			}
		} else {
			return count(UNREAD_NOT_DELETED);
		}
	}

	@Override
	public void multipleDeleteById(List<Long> ids) throws ServerFault {
		rbac.check(Verb.Write.name());
		if (ids.isEmpty()) {
			logger.info("ids list is empty, nothing to delete");
			return;
		}

		List<ItemValue<MailboxItem>> records = multipleByIdWithoutBody(ids);

		List<Long> uids = records.stream().filter(r -> !r.flags.contains(ItemFlag.Deleted)).map(r -> r.value.imapUid)
				.collect(Collectors.toList());

		if (uids.isEmpty()) {
			logger.info("filtered ids list is empty, nothing to delete");
			return;
		}

		logger.info("Delete {} records in {}", uids.size(), imapFolder);
		CompletableFuture<ItemChange> repEvent = ReplicationEvents.onRecordUpdate(mailboxUniqueId, uids.get(0));

		long time = System.currentTimeMillis();
		doImapAddFlags(uids, Arrays.asList(Flag.DELETED.toString()));
		time = System.currentTimeMillis() - time;
		try {
			ItemChange change = repEvent.get(10, TimeUnit.SECONDS);
			logger.info("Delete {} items with a latency of {}ms. (imap time: {}ms)", ids.size(), change.latencyMs,
					time);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

	}

}
