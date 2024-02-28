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
package net.bluemind.eas.backend.bm.mail;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Response;
import org.slf4j.MDC;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.ImapItemIdentifier;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.api.MessageSearchResult.Mbox;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.api.SearchSort;
import net.bluemind.backend.mail.api.flags.FlagUpdate;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventCounter.CounterOriginator;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.BufferByteSource;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSAttachementData;
import net.bluemind.eas.backend.MSEmail;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.SendMailData;
import net.bluemind.eas.backend.SendMailData.Mode;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.base.Range;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.email.Importance;
import net.bluemind.eas.dto.find.FindRequest;
import net.bluemind.eas.dto.find.FindRequest.Options;
import net.bluemind.eas.dto.find.FindRequest.Query;
import net.bluemind.eas.dto.find.FindResponse;
import net.bluemind.eas.dto.find.FindResponse.Response.Result.Properties;
import net.bluemind.eas.dto.find.FindResponse.Status;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.exception.NotAllowedException;
import net.bluemind.eas.exception.ObjectNotFoundException;
import net.bluemind.eas.exception.ServerErrorException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.mime4j.common.IMailRewriter;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.RewriteMode;
import net.bluemind.mime4j.common.RewriterBuilder;
import net.bluemind.proxy.support.AHCWithProxy;

public class MailBackend extends CoreConnect {

	private final EmailManager emailManager;
	private final ISyncStorage storage;

	public MailBackend(ISyncStorage storage) {
		emailManager = EmailManager.getInstance();
		this.storage = storage;
	}

	public Changes getContentChanges(BackendSession bs, SyncState state, CollectionId collectionId,
			BodyOptions bodyOptions, boolean hasFilterTypeChanged) throws ActiveSyncException {

		if (!bs.getUser().hasMailbox()) {
			logger.info("MailRouting == NONE for user {}. Return no changes.", bs.getLoginAtDomain());
			return new Changes();
		}

		final Optional<ZonedDateTime> filteredDate = (state.version == 0 || hasFilterTypeChanged)
				? Optional.of(state.date)
				: Optional.empty();

		MailFolder folder = storage.getMailFolder(bs, collectionId);

		IMailboxItems service = getMailboxItemsService(bs, folder.uid);

		ContainerChangeset<ItemVersion> changeset = service.filteredChangesetById(state.version,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		Changes changes = new Changes();
		changes.version = changeset.version;

		if (!filteredDate.isPresent()) {
			changeset.created.forEach(itemVersion -> {
				ItemChangeReference ic = new ItemChangeReference(ItemDataType.EMAIL);
				ic.setServerId(CollectionItem.of(collectionId, itemVersion.id));
				ic.setChangeType(ChangeType.ADD);
				changes.items.add(ic);
			});
		} else {
			ZonedDateTime deliveredAfter = filteredDate.get();
			List<List<ItemVersion>> createdParts = Lists.partition(changeset.created, 250);
			boolean stopLoading = false;
			int addedToSync = 0;
			for (List<ItemVersion> slice : createdParts) {
				if (stopLoading) {
					break;
				}
				List<ItemValue<MailboxItem>> items = service
						.multipleGetById(slice.stream().map(v -> v.id).collect(Collectors.toList()));
				for (ItemValue<MailboxItem> item : items) {
					if (item != null && item.value != null) {
						if (deliveredAfter.isBefore(ZonedDateTime.ofInstant(
								Instant.ofEpochMilli(item.value.body.date.getTime()), ZoneId.systemDefault()))) {
							ItemChangeReference ic = new ItemChangeReference(ItemDataType.EMAIL);
							ic.setServerId(CollectionItem.of(collectionId, item.internalId));
							ic.setChangeType(ChangeType.ADD);
							changes.items.add(ic);
							addedToSync++;
						} else {
							MDC.put("user", bs.getLoginAtDomain().replace("@", "_at_"));
							logger.debug("[{}] Stop loading at email {} ({} is before {}), {} / {}", //
									bs.getLoginAtDomain(), item.value, item.value.body.date, deliveredAfter,
									addedToSync, changeset.created.size());
							MDC.put("user", "anonymous");
							// stop loading as the changeset is sorted by
							// delivery date
							stopLoading = true;
							break;
						}
					} else {
						logger.warn("Item or value is null : {}", item);
					}
				}
			}
		}

		List<List<ItemVersion>> updatedParts = Lists.partition(changeset.updated, 250);
		for (List<ItemVersion> slice : updatedParts) {
			List<ItemValue<MailboxItem>> items = service
					.multipleGetById(slice.stream().map(v -> v.id).collect(Collectors.toList()));
			items.forEach(item -> {
				if (item != null) {
					ItemChangeReference ic = new ItemChangeReference(ItemDataType.EMAIL);
					ic.setServerId(CollectionItem.of(collectionId, item.internalId));
					ic.setChangeType(ChangeType.CHANGE);
					boolean isDraft = item.value.flags.contains(MailboxItemFlag.System.Draft.value())
							|| "Drafts".equals(folder.fullName);

					if (isDraft) {
						ic.setData(toAppData(bs, bodyOptions, folder, ic.getServerId().itemId));
					} else {
						ic.setData(AppData.of(FlagsChange.asEmailResponse(item.value), LazyLoaded.NOOP));
					}
					changes.items.add(ic);
				}
			});
		}

		changeset.deleted.forEach(itemVersion -> {
			ItemChangeReference ic = new ItemChangeReference(ItemDataType.EMAIL);
			ic.setServerId(CollectionItem.of(collectionId, itemVersion.id));
			ic.setChangeType(ChangeType.DELETE);
			changes.items.add(ic);
		});

		return changes;

	}

	public void delete(BackendSession bs, Collection<CollectionItem> serverIds, Boolean moveToTrash)
			throws CollectionNotFoundException {
		if (serverIds != null && !serverIds.isEmpty()) {
			HashMap<String, MailFolder> collections = new HashMap<>();
			HashMap<MailFolder, List<Long>> items = new HashMap<>();
			for (CollectionItem serverId : serverIds) {
				String collectionId = serverId.collectionId.getValue();
				if (!collections.containsKey(collectionId)) {
					MailFolder folder = storage.getMailFolder(bs, serverId.collectionId);
					collections.put(collectionId, folder);
					items.put(folder, new ArrayList<Long>());
				}

				items.get(collections.get(collectionId)).add(serverId.itemId);
			}

			for (Entry<MailFolder, List<Long>> entry : items.entrySet()) {
				MailFolder folder = entry.getKey();
				if (moveToTrash.booleanValue()) {

					String mailboxUid = bs.getUser().getUid();
					if (folder.collectionId.getSubscriptionId().isPresent()) {
						IOwnerSubscriptions subscriptionsService = getService(bs, IOwnerSubscriptions.class,
								bs.getUser().getDomain(), bs.getUser().getUid());
						ItemValue<ContainerSubscriptionModel> sub = subscriptionsService
								.getCompleteById(folder.collectionId.getSubscriptionId().get());
						mailboxUid = sub.value.owner;
					}
					IMailboxFolders service = getMailboxFoldersServiceByCollection(bs, folder.collectionId);
					ItemValue<MailboxFolder> source = service.getComplete(folder.uid);
					HierarchyNode sourceHierarchyNode = storage.getHierarchyNode(bs.getUniqueIdentifier(),
							bs.getUser().getDomain(), mailboxUid,
							ContainerHierarchyNode.uidFor(IMailReplicaUids.mboxRecords(source.uid), "mailbox_records",
									bs.getUser().getDomain()));

					CyrusPartition part = CyrusPartition.forServerAndDomain(bs.getUser().getDataLocation(),
							bs.getUser().getDomain());
					IMailboxFolders mboxFolders = getService(bs, IMailboxFolders.class, part.name,
							"user." + bs.getUser().getUid().replace('.', '^'));
					ItemValue<MailboxFolder> trash = mboxFolders.byName("Trash");
					HierarchyNode trashHierarchyNode = storage.getHierarchyNode(bs.getUniqueIdentifier(),
							bs.getUser().getDomain(), bs.getUser().getUid(),
							ContainerHierarchyNode.uidFor(IMailReplicaUids.mboxRecords(trash.uid), "mailbox_records",
									bs.getUser().getDomain()));

					emailManager.moveItems(bs, sourceHierarchyNode, trashHierarchyNode,
							items.get(folder).stream().map(i -> (long) i).collect(Collectors.toList()));
				} else {
					IMailboxItems service = getMailboxItemsService(bs, folder.uid);
					entry.getValue().forEach(id -> {
						logger.info("[{}] Delete mail {}", bs.getUser().getUid(), id);
						try {
							service.deleteById(id);
						} catch (ServerFault sf) {
							if (sf.getCode() != ErrorCode.TIMEOUT) {
								throw sf;
							}
						}
					});
				}

			}
		}
	}

	public void sendDraft(BackendSession bs, String serverId, IApplicationData data) throws ActiveSyncException {

		MSEmail email = (MSEmail) data;

		CollectionItem ci = CollectionItem.of(serverId);

		MailFolder folder = storage.getMailFolder(bs, ci.collectionId);
		IMailboxItems service = getMailboxItemsService(bs, folder.uid);

		ItemValue<MailboxItem> draft = service.getCompleteById(ci.itemId);
		MessageImpl message = email.getMessage();
		mergeDraft(draft, message);

		SendMailData mailData = new SendMailData();
		mailData.backendSession = bs;
		if (email.getMimeContent() != null) {
			mailData.mailContent = email.getMimeContent();
		} else {
			try {
				mailData.mailContent = BufferByteSource.of(Mime4JHelper.mmapedEML(message).nettyBuffer());
			} catch (Exception e) {
				throw new ActiveSyncException(e.getMessage());
			}
		}
		mailData.saveInSent = true;
		mailData.mode = Mode.Send;

		sendEmail(mailData);
	}

	public CollectionItem store(BackendSession bs, CollectionId collectionId, Optional<String> serverId,
			IApplicationData data) throws ActiveSyncException {

		MailFolder folder = storage.getMailFolder(bs, collectionId);
		IMailboxItems service = getMailboxItemsService(bs, folder.uid);
		MSEmail email = (MSEmail) data;

		if (serverId.isPresent()) {
			CollectionItem ci = CollectionItem.of(serverId.get());
			if ("Drafts".equals(folder.fullName)) {
				// update body in Drafts folder only
				updateBody(service, email, ci);
			} else {
				validateFlag(email.isRead(), MailboxItemFlag.System.Seen.value(), service, ci.itemId);
				validateFlag(email.isStarred(), MailboxItemFlag.System.Flagged.value(), service, ci.itemId);
			}
			return ci;
		} else {
			// store new email (Draft)
			MessageBody messageBody;
			if (email.getMimeContent() != null) {
				messageBody = uploadPart(service, email.getMimeContent());
			} else {
				try {
					messageBody = uploadPart(service,
							BufferByteSource.of(Mime4JHelper.mmapedEML(email.getMessage()).nettyBuffer()));
				} catch (Exception e) {
					throw new ActiveSyncException(e.getMessage());
				}
			}

			MailboxItem mailboxItem = new MailboxItem();
			mailboxItem.body = messageBody;
			mailboxItem.flags = Arrays.asList(MailboxItemFlag.System.Draft.value(),
					MailboxItemFlag.System.Seen.value());
			ImapItemIdentifier created = service.create(mailboxItem);
			return CollectionItem.of(collectionId, created.id);
		}
	}

	private void updateBody(IMailboxItems service, MSEmail email, CollectionItem ci) throws ActiveSyncException {
		MessageBody messageBody = null;
		ItemValue<MailboxItem> draft = null;
		try {
			draft = service.getForUpdate(ci.itemId);
		} catch (Exception e) {
		}
		if (draft == null) {
			throw new ObjectNotFoundException();
		}
		if (email.getMimeContent() != null) {
			// MIME, replace the current draft
			messageBody = uploadPart(service, email.getMimeContent());
		} else {
			MessageImpl message = email.getMessage();
			mergeDraft(draft, message);
			try {
				messageBody = uploadPart(service, BufferByteSource.of(Mime4JHelper.mmapedEML(message).nettyBuffer()));
			} catch (IOException e) {
				logger.error("Failed to update draft {}, '{}'", ci, message.getSubject());
			}
		}
		if (messageBody != null) {
			draft.value.body = messageBody;
			draft.value.flags = Arrays.asList(MailboxItemFlag.System.Draft.value(),
					MailboxItemFlag.System.Seen.value());
			service.updateById(ci.itemId, draft.value);
		}
	}

	private void mergeDraft(ItemValue<MailboxItem> draft, MessageImpl message) {
		if (message.getTo() == null) {
			message.setTo(parseRecipients(draft.value.body.recipients, RecipientKind.Primary));
		}
		if (message.getCc() == null) {
			message.setCc(parseRecipients(draft.value.body.recipients, RecipientKind.CarbonCopy));
		}
		if (message.getBcc() == null) {
			message.setBcc(parseRecipients(draft.value.body.recipients, RecipientKind.BlindCarbonCopy));
		}
		if (message.getSubject() == null) {
			message.setSubject(draft.value.body.subject);
		}
	}

	private List<Mailbox> parseRecipients(List<Recipient> recipients, RecipientKind kind) {
		List<Recipient> filtered = recipients.stream().filter(r -> r.kind == kind).toList();
		List<Mailbox> mailboxes = new ArrayList<>();
		filtered.forEach(recip -> {
			String[] address = recip.address.split("@");
			mailboxes.add(new Mailbox(recip.dn, address[0], address[1]));
		});
		return mailboxes;
	}

	private MessageBody uploadPart(IMailboxItems service, ByteSource content) throws ActiveSyncException {
		try {
			Stream eml = streamFromByteSource(content);
			String partId = service.uploadPart(eml);
			Part part = Part.create(null, "message/rfc822", partId);
			MessageBody messageBody = new MessageBody();
			messageBody.structure = part;
			return messageBody;
		} catch (Exception e) {
			throw new ActiveSyncException("Failed to store message");
		}
	}

	private static Stream streamFromByteSource(ByteSource content) throws IOException {
		ByteBufOutputStream os = new ByteBufOutputStream(Unpooled.buffer());
		content.copyTo(os);
		return VertxStream.stream(Buffer.buffer(os.buffer()));
	}

	private void validateFlag(Boolean property, MailboxItemFlag flag, IMailboxItems service, long itemId) {
		if (property != null) {
			if (property.booleanValue()) {
				service.addFlag(FlagUpdate.of(itemId, flag));
			} else {
				service.deleteFlag(FlagUpdate.of(itemId, flag));
			}
		}

	}

	public List<MoveItemsResponse.Response> move(BackendSession bs, HierarchyNode srcFolder, HierarchyNode dstFolder,
			List<CollectionItem> items) {
		return emailManager.moveItems(bs, srcFolder, dstFolder,
				items.stream().map(v -> v.itemId).collect(Collectors.toList()));
	}

	/**
	 * @param mail
	 */
	public void sendEmail(SendMailData mail) throws ActiveSyncException {

		BackendSession bs = mail.backendSession;

		if (!bs.getUser().hasMailbox()) {
			logger.info("MailRouting == NONE for user {}. Do not try to send mail", bs.getLoginAtDomain());
			return;
		}

		try {
			Message m = MessageServiceFactory.newInstance().newMessageBuilder()
					.parseMessage(mail.mailContent.openBufferedStream());
			IMIPInfos infos = IMIPParserFactory.create().parse(m);

			if (infos == null) {
				IMailRewriter rewriter = Mime4JHelper.untouched(getUserEmail(bs));
				send(bs, mail.mailContent, rewriter, mail.saveInSent);
			} else {
				processImipInfos(bs, infos);
			}
		} catch (Exception e) {
			throw new ServerErrorException(e);
		}
	}

	private void processImipInfos(BackendSession bs, IMIPInfos infos) {
		// BM-4930, ACMS-196, and many more...
		if (infos.method == ITIPMethod.REPLY || infos.method == ITIPMethod.CANCEL
				|| infos.method == ITIPMethod.COUNTER) {
			logger.info(" **** Device {} sends IMIP email, method {}. user {}", bs.getDevId(), infos.method,
					bs.getUser().getLoginAtDomain());
			for (ICalendarElement element : infos.iCalendarElements) {
				for (Attendee attendee : element.attendees) {
					String email = attendee.mailto;
					if (email != null
							&& (email.equals(bs.getLoginAtDomain()) || email.equals(bs.getUser().getDefaultEmail()))) {
						ICalendar cs = getService(bs, ICalendar.class,
								ICalendarUids.defaultUserCalendar(bs.getUser().getUid()));

						ItemValue<VEventSeries> event = cs.getComplete(infos.uid);
						if (event != null) {
							element = checkForRecurrenceException(element, event);
							if (element instanceof VEventOccurrence) {
								VEventOccurrence uOccurr = (VEventOccurrence) element;
								VEventOccurrence occu = event.value.occurrence(uOccurr.recurid);
								if (occu != null) {
									updateStatus(occu, attendee);
									updateCounter(infos, attendee, event, occu, element);
									cs.update(infos.uid, event.value, true);
								} else {
									logger.warn("did not found in {} occurrence with recurid {}", infos.uid,
											uOccurr.recurid);
								}
							} else {
								updateStatus(event.value.main, attendee);
								updateCounter(infos, attendee, event,
										VEventOccurrence.fromEvent(event.value.main, null), element);
								cs.update(infos.uid, event.value, true);
							}
						} else {
							logger.warn("did not found event with uid {}", infos.uid);
						}

					}
				}
			}
		} else {
			logger.warn(" **** Device {} tried to send an IMIP email, we prevented it. Method: {}, user: {}",
					bs.getDevId(), infos.method, bs.getUser().getLoginAtDomain());
		}
	}

	private ICalendarElement checkForRecurrenceException(ICalendarElement element, ItemValue<VEventSeries> event) {
		if (event.value.main == null && event.value.occurrences.size() == 1) {
			return VEventOccurrence.fromEvent((VEvent) element, event.value.occurrences.get(0).recurid);
		}
		return element;
	}

	private void updateCounter(IMIPInfos infos, Attendee attendee, ItemValue<VEventSeries> event,
			VEventOccurrence occurrence, ICalendarElement element) {
		if (infos.method == ITIPMethod.COUNTER || attendee.counter != null) {
			VEventCounter counter = new VEventCounter();
			counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);

			// fix dates
			VEvent vevent = (VEvent) element;
			if (attendee.counter != null) {
				long duration = BmDateTimeWrapper.toTimestamp(occurrence.dtend.iso8601, occurrence.dtend.timezone)
						- BmDateTimeWrapper.toTimestamp(occurrence.dtstart.iso8601, occurrence.dtstart.timezone);

				occurrence.dtstart = BmDateTimeWrapper.fromTimestamp(
						BmDateTimeWrapper.toTimestamp(attendee.counter.iso8601, attendee.counter.timezone));
				occurrence.dtend = BmDateTimeWrapper.fromTimestamp(
						BmDateTimeWrapper.toTimestamp(occurrence.dtstart.iso8601, occurrence.dtstart.timezone)
								+ duration);
			} else {
				occurrence.dtstart = vevent.dtstart;
				occurrence.dtend = vevent.dtend;
			}

			counter.counter = occurrence;

			event.value.counters = Arrays.asList(counter);
		}
	}

	private void updateStatus(VEvent event, Attendee attendee) {
		Iterator<Attendee> it = event.attendees.iterator();
		while (it.hasNext()) {
			Attendee a = it.next();
			if (attendee.mailto.equals(a.mailto)) {
				// Set new part status
				a.partStatus = attendee.partStatus;
			}
		}
	}

	/**
	 * @param bs
	 * @param mailContent
	 * @param saveInSent
	 * @param collectionId
	 * @param serverId
	 * @throws ServerErrorException
	 */
	public void replyToEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String collectionId,
			String serverId, boolean includePrevious) throws ServerErrorException {
		try {
			MailFolder folder = storage.getMailFolder(bs, CollectionId.of(collectionId));
			long id = CollectionItem.of(serverId).itemId;

			IMailRewriter rewriter = Mime4JHelper.untouched(getUserEmail(bs));
			if (includePrevious) {
				try (InputStream is = emailManager.fetchMimeStream(bs, folder, id)) {
					if (is != null) {
						RewriterBuilder rb = new RewriterBuilder();
						rb.setMode(RewriteMode.REPLY);
						rb.setKeepAttachments(false);
						rb.setIncludedContent(is);
						rb.setFrom(getUserEmail(bs));
						rewriter = rb.build();
					}
				}
			}
			IMailboxItems service = getMailboxItemsService(bs, folder.uid);
			service.addFlag(FlagUpdate.of(id, MailboxItemFlag.System.Answered.value()));

			send(bs, mailContent, rewriter, saveInSent);
		} catch (Exception e) {
			throw new ServerErrorException(e);
		}

	}

	public void forwardEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String collectionId,
			String serverId, boolean includePrevious) {
		try {
			MailFolder folder = storage.getMailFolder(bs, CollectionId.of(collectionId));
			long id = CollectionItem.of(serverId).itemId;

			IMailRewriter rewriter = Mime4JHelper.untouched(getUserEmail(bs));
			if (includePrevious) {
				try (InputStream is = emailManager.fetchMimeStream(bs, folder, id)) {
					if (is != null) {
						RewriterBuilder rb = new RewriterBuilder();
						rb.setMode(RewriteMode.FORWARD_INLINE);
						rb.setKeepAttachments(true);
						rb.setIncludedContent(is);
						rb.setFrom(getUserEmail(bs));
						rewriter = rb.build();
					}
				}
			}

			IMailboxItems service = getMailboxItemsService(bs, folder.uid);
			service.addFlag(FlagUpdate.of(id, new MailboxItemFlag("$Forwarded")));

			send(bs, mailContent, rewriter, saveInSent);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private Mailbox getUserEmail(BackendSession bs) {
		MSUser u = bs.getUser();
		String from = u.getDefaultEmail();
		String dn = u.getDisplayName();
		String[] split = from.split("@");
		return new Mailbox(dn, split[0], split[1]);
	}

	private void send(BackendSession bs, ByteSource mailContent, IMailRewriter handler, Boolean saveInSent)
			throws Exception {
		MimeStreamParser parser = Mime4JHelper.parser();
		parser.setContentHandler(handler);
		parser.parse(mailContent.openBufferedStream());
		emailManager.sendEmail(bs, handler, saveInSent);
	}

	/**
	 * @param bs
	 * @param attachmentId
	 * @return
	 * @throws ObjectNotFoundException
	 */
	public MSAttachementData getAttachment(BackendSession bs, String attachmentId) throws ObjectNotFoundException {
		if (attachmentId != null && !attachmentId.isEmpty()) {
			Map<String, String> parsedAttId = AttachmentHelper.parseAttachmentId(attachmentId);
			try {

				String type = parsedAttId.get(AttachmentHelper.TYPE);
				if (AttachmentHelper.BM_FILEHOSTING.equals(type)) {
					String url = parsedAttId.get(AttachmentHelper.URL);
					String contentType = parsedAttId.get(AttachmentHelper.CONTENT_TYPE);

					try (FileBackedOutputStream fbos = new FileBackedOutputStream(32000, "bm-eas-getattachment");
							AsyncHttpClient ahc = AHCWithProxy.build(storage.getSystemConf())) {
						return ahc.prepareGet(url).execute(new AsyncCompletionHandler<MSAttachementData>() {

							@Override
							public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
								fbos.write(bodyPart.getBodyPartBytes());
								return State.CONTINUE;
							}

							@Override
							public MSAttachementData onCompleted(Response response) throws Exception {
								return new MSAttachementData(contentType, DisposableByteSource.wrap(fbos));
							}
						}).get(20, TimeUnit.SECONDS);
					}
				}

				String collectionId = parsedAttId.get(AttachmentHelper.COLLECTION_ID);
				String messageId = parsedAttId.get(AttachmentHelper.MESSAGE_ID);
				String mimePartAddress = parsedAttId.get(AttachmentHelper.MIME_PART_ADDRESS);
				String contentType = parsedAttId.get(AttachmentHelper.CONTENT_TYPE);
				String contentTransferEncoding = parsedAttId.get(AttachmentHelper.CONTENT_TRANSFER_ENCODING);
				logger.info(
						"attachmentId: [colId:{}] [emailUid:{}] [partAddress:{}] [contentType:{}] [transferEncoding:{}]",
						collectionId, messageId, mimePartAddress, contentType, contentTransferEncoding);

				MailFolder folder = storage.getMailFolder(bs, CollectionId.of(collectionId));

				InputStream is = emailManager.fetchAttachment(bs, folder, Integer.parseInt(messageId), mimePartAddress,
						contentTransferEncoding);
				byte[] bytes = ByteStreams.toByteArray(is);
				is.close();

				return new MSAttachementData(contentType, DisposableByteSource.wrap(bytes));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		throw new ObjectNotFoundException(String.format("Failed to fetch attachment %s", attachmentId));
	}

	public void purgeFolder(BackendSession bs, CollectionId collectionId, boolean deleteSubFolder)
			throws NotAllowedException {
		try {
			MailFolder folder = storage.getMailFolder(bs, collectionId);

			if (!"Trash".equals(folder.fullName)) {
				throw new NotAllowedException("Only the Trash folder can be purged.");
			}

			emailManager.purgeFolder(bs, folder, collectionId, deleteSubFolder);

		} catch (Exception e) {
			throw new NotAllowedException(e);
		}
	}

	public AppData fetch(BackendSession bs, BodyOptions bodyParams, ItemChangeReference ic) throws ActiveSyncException {
		try {
			MailFolder folder = storage.getMailFolder(bs, ic.getServerId().collectionId);
			return toAppData(bs, bodyParams, folder, ic.getServerId().itemId);
		} catch (ActiveSyncException ase) {
			throw ase;
		} catch (Exception e) {
			throw new ActiveSyncException("Shit happens", e);
		}
	}

	public Map<Long, AppData> fetchMultiple(BackendSession bs, BodyOptions bodyParams, CollectionId collectionId,
			List<Long> ids) throws ActiveSyncException {

		MailFolder folder = storage.getMailFolder(bs, collectionId);

		Map<Long, AppData> res = new HashMap<>(ids.size());
		ids.stream().forEach(id -> {
			try {
				AppData data = toAppData(bs, bodyParams, folder, id);
				res.put(id, data);
			} catch (Exception e) {
				logger.warn("Fail to convert email {}, folder {}. Skip it.", id, folder);
			}
		});

		return res;
	}

	private AppData toAppData(BackendSession bs, BodyOptions bodyParams, MailFolder folder, Long id) {
		EmailResponse er = EmailManager.getInstance().loadStructure(bs, folder, id);
		LazyLoaded<BodyOptions, AirSyncBaseResponse> bodyProvider = BodyLoaderFactory.from(bs, folder, id, bodyParams);
		return AppData.of(er, bodyProvider);
	}

	public FindResponse.Response find(BackendSession bs, FindRequest query) throws CollectionNotFoundException {
		Optional<MailFolder> folder = Optional.empty();

		FindResponse.Response response = new FindResponse.Response();

		if (query.executeSearch.mailBoxSearchCriterion != null) {
			Query searchQuery = query.executeSearch.mailBoxSearchCriterion.query;
			Options options = query.executeSearch.mailBoxSearchCriterion.options;

			if (!Strings.isNullOrEmpty(searchQuery.collectionId)) {
				try {
					folder = Optional.of(storage.getMailFolder(bs, CollectionId.of(searchQuery.collectionId)));
				} catch (CollectionNotFoundException e) {
					logger.warn("Failed to find folder {}", searchQuery.collectionId);
					throw e;
				}
			}

			MailboxFolderSearchQuery mailboxSearchQuery = new MailboxFolderSearchQuery();
			mailboxSearchQuery.query = new SearchQuery();
			mailboxSearchQuery.sort = SearchSort.byField("date", SearchSort.Order.Desc);
			mailboxSearchQuery.query.recordQuery = "-is:deleted";
			mailboxSearchQuery.query.query = searchQuery.freeText;
			mailboxSearchQuery.query.scope = new net.bluemind.backend.mail.api.SearchQuery.SearchScope();
			if (folder.isPresent()) {
				mailboxSearchQuery.query.scope.folderScope = new net.bluemind.backend.mail.api.SearchQuery.FolderScope();
				mailboxSearchQuery.query.scope.folderScope.folderUid = folder.get().uid;
				if (options != null) {
					mailboxSearchQuery.query.scope.isDeepTraversal = options.deepTraversal;
				}
			}

			if (options != null && options.range != null) {
				mailboxSearchQuery.query.offset = options.range.min;
				mailboxSearchQuery.query.maxResults = options.range.max;
			}

			IMailboxFolders mailboxFolderService = getMailboxFoldersService(bs);
			SearchResult results = mailboxFolderService.searchItems(mailboxSearchQuery);

			IContainersFlatHierarchy flatH = getService(bs, IContainersFlatHierarchy.class, bs.getUser().getDomain(),
					bs.getUser().getUid());

			response.results = new ArrayList<>(results.results.size());
			results.results.forEach(messageSearchResult -> {
				FindResponse.Response.Result result = new FindResponse.Response.Result();

				String collectionId = searchQuery.collectionId;
				if (Strings.isNullOrEmpty(collectionId)) {
					String nodeUid = ContainerHierarchyNode.uidFor(messageSearchResult.containerUid,
							IMailReplicaUids.MAILBOX_RECORDS, bs.getUser().getDomain());
					ItemValue<ContainerHierarchyNode> hierarchyNode = flatH.getComplete(nodeUid);
					collectionId = Long.toString(hierarchyNode.internalId);
				}

				result.serverId = CollectionItem.of(collectionId, messageSearchResult.itemId).toString();
				result.collectionId = collectionId;

				Properties properties = new FindResponse.Response.Result.Properties();
				properties.subject = messageSearchResult.subject;
				properties.dateReceived = messageSearchResult.date;
				properties.displayTo = mboxToString(messageSearchResult.to);
				properties.importance = messageSearchResult.flagged ? Importance.HIGH : Importance.NORMAL;
				properties.read = messageSearchResult.seen;
				properties.preview = truncate(messageSearchResult.preview, 255);
				properties.from = mboxToString(messageSearchResult.from);
				result.properties = properties;

				response.results.add(result);
			});
			response.total = results.totalResults;
			response.range = Range.create(options.range.min, (options.range.max + response.total.intValue() - 1));

			return response;
		}

		response.status = Status.INVALID_REQUEST;
		return response;
	}

	private String mboxToString(Mbox mbox) {
		if (!Strings.isNullOrEmpty(mbox.displayName)) {
			return "\"" + mbox.displayName + "\" <" + mbox.address + ">";
		}
		return mbox.address;
	}

	private String truncate(String text, int length) {
		if (text.length() <= length) {
			return text;
		} else {
			return text.substring(0, length);
		}
	}

}
