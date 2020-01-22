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

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.MessageServiceFactoryImpl;
import org.apache.james.mime4j.parser.MimeStreamParser;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag.AnsweredFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag.FlaggedFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag.SeenFlag;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSAttachementData;
import net.bluemind.eas.backend.MSEmail;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.SendMailData;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.email.AttachmentResponse;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
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

public class MailBackend extends CoreConnect {

	private final EmailManager emailManager;
	private final ISyncStorage storage;

	public MailBackend(ISyncStorage storage) {
		emailManager = EmailManager.getInstance();
		this.storage = storage;
	}

	public Changes getContentChanges(BackendSession bs, SyncState state, Integer collectionId,
			boolean hasFilterTypeChanged) throws ActiveSyncException {

		if (!bs.getUser().hasMailbox()) {
			logger.info("MailRouting == NONE for user " + bs.getLoginAtDomain() + ". Return no changes.");
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
				ic.setServerId(CollectionItem.of(collectionId, Long.valueOf(itemVersion.id).toString()));
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
						.multipleById(slice.stream().map(v -> v.id).collect(Collectors.toList()));
				for (ItemValue<MailboxItem> item : items) {
					if (item != null && item.value != null) {
						if (deliveredAfter.isBefore(ZonedDateTime.ofInstant(
								Instant.ofEpochMilli(item.value.body.date.getTime()), ZoneId.systemDefault()))) {
							ItemChangeReference ic = new ItemChangeReference(ItemDataType.EMAIL);
							ic.setServerId(CollectionItem.of(collectionId, Long.valueOf(item.internalId).toString()));
							ic.setChangeType(ChangeType.ADD);
							changes.items.add(ic);
							addedToSync++;
						} else {
							logger.info("[{}] Stop loading at email {} ({} is before {}), {} / {}", //
									bs.getLoginAtDomain(), item.value, item.value.body.date, deliveredAfter,
									addedToSync, changeset.created.size());
							stopLoading = true;
							// stop loading as the changeset is sorted by
							// delivery date
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
					.multipleById(slice.stream().map(v -> v.id).collect(Collectors.toList()));
			items.forEach(item -> {
				if (item != null) {
					ItemChangeReference ic = new ItemChangeReference(ItemDataType.EMAIL);
					ic.setServerId(CollectionItem.of(collectionId, Long.valueOf(item.internalId).toString()));
					ic.setChangeType(ChangeType.CHANGE);
					ic.setData(AppData.of(FlagsChange.asEmailResponse(item.value), LazyLoaded.NOOP));
					changes.items.add(ic);
				}
			});
		}

		changeset.deleted.forEach(itemVersion -> {
			ItemChangeReference ic = new ItemChangeReference(ItemDataType.EMAIL);
			ic.setServerId(CollectionItem.of(collectionId, Long.valueOf(itemVersion.id).toString()));
			ic.setChangeType(ChangeType.DELETE);
			changes.items.add(ic);
		});

		return changes;

	}

	public void delete(BackendSession bs, Collection<CollectionItem> serverIds, Boolean moveToTrash)
			throws CollectionNotFoundException {
		if (serverIds != null && !serverIds.isEmpty()) {
			HashMap<Integer, MailFolder> collections = new HashMap<Integer, MailFolder>();
			HashMap<MailFolder, List<Integer>> items = new HashMap<MailFolder, List<Integer>>();
			for (CollectionItem serverId : serverIds) {
				Integer collectionId = serverId.collectionId;
				if (!collections.containsKey(collectionId)) {
					MailFolder folder = storage.getMailFolder(bs, collectionId);
					collections.put(collectionId, folder);
					items.put(folder, new ArrayList<Integer>());
				}

				Integer uid = Integer.parseInt(serverId.itemId);
				items.get(collections.get(collectionId)).add(uid);
			}

			for (MailFolder folder : items.keySet()) {
				if (moveToTrash) {
					IMailboxFolders service = getIMailboxFoldersService(bs);
					ItemValue<MailboxFolder> source = service.getComplete(folder.uid);

					ItemValue<MailboxFolder> trash = service.byName("Trash");

					HierarchyNode dstCollection = storage.getHierarchyNode(bs.getUser().getDomain(),
							bs.getUser().getUid(),
							ContainerHierarchyNode.uidFor(IMailReplicaUids.mboxRecords(trash.uid), "mailbox_records",
									bs.getUser().getDomain()));

					emailManager.moveItems(bs, source.internalId, trash.internalId, items.get(folder),
							folder.collectionId, dstCollection.collectionId);
				} else {
					IMailboxItems service = getMailboxItemsService(bs, folder.uid);
					items.get(folder).forEach(id -> {
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

	public CollectionItem store(BackendSession bs, int collectionId, Optional<String> serverId, IApplicationData data)
			throws ActiveSyncException {

		if (serverId.isPresent()) {
			CollectionItem ci = CollectionItem.of(serverId.get());

			MailFolder folder = storage.getMailFolder(bs, collectionId);
			IMailboxItems service = getMailboxItemsService(bs, folder.uid);

			long id = Long.parseLong(ci.itemId);
			ItemValue<MailboxItem> item = service.getCompleteById(id);
			if (item == null) {
				logger.warn("[{}] Fail to fetch mailboxItem {} in {}", bs.getUser().getDefaultEmail(), id,
						folder.fullName);
				return null;
			}

			MSEmail email = (MSEmail) data;
			if (email.isRead() != null) {
				if (email.isRead()) {
					item.value.flags.add(new SeenFlag());
				} else {
					item.value.flags.removeIf(f -> f == new SeenFlag());
				}
			}

			if (email.isStarred() != null) {
				if (email.isStarred()) {
					item.value.flags.add(new FlaggedFlag());
				} else {
					item.value.flags.removeIf(f -> f.equals(new FlaggedFlag()));
				}
			}
			try {
				service.updateById(id, item.value);
			} catch (ServerFault sf) {
				if (sf.getCode() != ErrorCode.TIMEOUT) {
					throw sf;
				}
			}

			return ci;
		}

		return null;
	}

	public List<MoveItemsResponse.Response> move(BackendSession bs, HierarchyNode srcFolder, HierarchyNode dstFolder,
			List<CollectionItem> items) {
		IMailboxFolders service = getIMailboxFoldersService(bs);
		ItemValue<MailboxFolder> source = service.getComplete(IMailReplicaUids.uniqueId(srcFolder.containerUid));
		ItemValue<MailboxFolder> destination = service.getComplete(IMailReplicaUids.uniqueId(dstFolder.containerUid));

		return emailManager.moveItems(bs, source.internalId, destination.internalId,
				items.stream().map(v -> Integer.parseInt(v.itemId)).collect(Collectors.toList()),
				srcFolder.collectionId, dstFolder.collectionId);
	}

	/**
	 * @param mail
	 */
	public void sendEmail(SendMailData mail) throws ActiveSyncException {

		BackendSession bs = mail.backendSession;

		if (!bs.getUser().hasMailbox()) {
			logger.info("MailRouting == NONE for user " + bs.getLoginAtDomain() + ". Do not try to send mail");
			return;
		}

		try {
			Message m = MessageServiceFactoryImpl.newInstance().newMessageBuilder()
					.parseMessage(mail.mailContent.openBufferedStream());
			IMIPInfos infos = IMIPParserFactory.create().parse(m);

			if (infos == null) {
				IMailRewriter rewriter = Mime4JHelper.untouched(getUserEmail(bs));
				send(bs, mail.mailContent, rewriter, mail.saveInSent);
			} else {

				// BM-4930, ACMS-196, and many more...
				if (infos.method == ITIPMethod.REPLY || infos.method == ITIPMethod.CANCEL) {
					logger.info(" **** Device {} sends IMIP email, method {}. user {}", bs.getDevId(), infos.method,
							bs.getUser().getLoginAtDomain());
					for (ICalendarElement element : infos.iCalendarElements) {
						for (Attendee attendee : element.attendees) {
							String email = attendee.mailto;
							if (email != null && (email.equals(bs.getLoginAtDomain())
									|| email.equals(bs.getUser().getDefaultEmail()))) {
								ICalendar cs = getService(bs, ICalendar.class,
										ICalendarUids.defaultUserCalendar(bs.getUser().getUid()));

								ItemValue<VEventSeries> event = cs.getComplete(infos.uid);
								if (event != null) {
									if (element instanceof VEventOccurrence) {
										VEventOccurrence uOccurr = (VEventOccurrence) element;
										VEventOccurrence occu = event.value.occurrence(uOccurr.recurid);
										if (occu != null) {
											updateStatus(occu, attendee);
											cs.update(infos.uid, event.value, true);
										} else {
											logger.warn("did not found in {} occurrence with recurid {}", infos.uid,
													uOccurr.recurid);
										}
									} else {
										updateStatus(event.value.main, attendee);
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
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerErrorException(e);
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
	public void replyToEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String cid, String serverId,
			boolean includePrevious) throws ServerErrorException {
		try {
			MailFolder folder = storage.getMailFolder(bs, Integer.parseInt(cid));
			Integer uid = Integer.parseInt(CollectionItem.of(serverId).itemId);

			IMailRewriter rewriter = Mime4JHelper.untouched(getUserEmail(bs));
			if (includePrevious) {
				try (InputStream is = emailManager.fetchMimeStream(bs, folder, uid)) {
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
			ItemValue<MailboxItem> item = service.getCompleteById(uid);
			item.value.flags.add(new AnsweredFlag());
			service.updateById(uid, item.value);

			send(bs, mailContent, rewriter, saveInSent);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerErrorException(e);
		}

	}

	public void forwardEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String collectionId,
			String serverId, boolean includePrevious) {
		try {
			MailFolder folder = storage.getMailFolder(bs, Integer.parseInt(collectionId));
			Integer uid = Integer.parseInt(CollectionItem.of(serverId).itemId);

			IMailRewriter rewriter = Mime4JHelper.untouched(getUserEmail(bs));
			if (includePrevious) {
				try (InputStream is = emailManager.fetchMimeStream(bs, folder, uid)) {
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
			ItemValue<MailboxItem> item = service.getCompleteById(uid);
			item.value.flags.add(new MailboxItemFlag("$Forwarded"));
			service.updateById(uid, item.value);

			send(bs, mailContent, rewriter, saveInSent);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private Mailbox getUserEmail(BackendSession bs) throws Exception {
		MSUser u = bs.getUser();
		String from = u.getDefaultEmail();
		String dn = u.getDisplayName();
		String[] split = from.split("@");
		Mailbox mbox = new Mailbox(dn, split[0], split[1]);
		return mbox;
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
				String collectionId = parsedAttId.get(AttachmentHelper.COLLECTION_ID);
				String messageId = parsedAttId.get(AttachmentHelper.MESSAGE_ID);
				String mimePartAddress = parsedAttId.get(AttachmentHelper.MIME_PART_ADDRESS);
				String contentType = parsedAttId.get(AttachmentHelper.CONTENT_TYPE);
				String contentTransferEncoding = parsedAttId.get(AttachmentHelper.CONTENT_TRANSFER_ENCODING);
				logger.info("attachmentId= [collectionId:" + collectionId + "] [emailUid:" + messageId
						+ "] [mimePartAddress:" + mimePartAddress + "] [contentType:" + contentType
						+ "] [contentTransferEncoding:" + contentTransferEncoding + "]");

				MailFolder folder = storage.getMailFolder(bs, Integer.parseInt(collectionId));

				InputStream is = emailManager.fetchAttachment(bs, folder, Integer.parseInt(messageId), mimePartAddress,
						contentTransferEncoding);
				byte[] bytes = ByteStreams.toByteArray(is);
				is.close();

				return new MSAttachementData(contentType, DisposableByteSource.wrap(bytes));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		throw new ObjectNotFoundException();
	}

	public AttachmentResponse getAttachmentMetadata(String attachmentId) throws ObjectNotFoundException {
		if (attachmentId != null && !attachmentId.isEmpty()) {
			Map<String, String> parsedAttId = AttachmentHelper.parseAttachmentId(attachmentId);
			if (parsedAttId != null) {
				String contentType = parsedAttId.get(AttachmentHelper.CONTENT_TYPE);
				AttachmentResponse ar = new AttachmentResponse();
				ar.contentType = contentType;
				return ar;
			}
		}
		throw new ObjectNotFoundException();
	}

	public void purgeFolder(BackendSession bs, HierarchyNode node, boolean deleteSubFolder) throws NotAllowedException {
		try {
			MailFolder folder = storage.getMailFolder(bs, (int) node.collectionId);

			if (!"Trash".equals(folder.fullName)) {
				throw new NotAllowedException("Only the Trash folder can be purged.");
			}

			emailManager.purgeFolder(bs, folder, deleteSubFolder);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new NotAllowedException(e);
		}
	}

	public AppData fetch(BackendSession bs, BodyOptions bodyParams, ItemChangeReference ic) throws ActiveSyncException {
		try {
			MailFolder folder = storage.getMailFolder(bs, ic.getServerId().collectionId);
			AppData data = toAppData(bs, bodyParams, folder, ic.getServerId().itemId);
			return data;
		} catch (ActiveSyncException ase) {
			logger.error(ase.getMessage(), ase);
			throw ase;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ActiveSyncException("Shit happens", e);
		}
	}

	public Map<String, AppData> fetchMultiple(BackendSession bs, BodyOptions bodyParams, int collectionId,
			List<String> ids) throws ActiveSyncException {

		MailFolder folder = storage.getMailFolder(bs, collectionId);

		Map<String, AppData> res = new HashMap<String, AppData>(ids.size());
		ids.stream().forEach(id -> {
			try {
				AppData data = toAppData(bs, bodyParams, folder, id);
				res.put(id, data);
			} catch (Exception e) {
				logger.error("Fail to convert email {}, folder {}", id, folder, e);
			}
		});

		return res;
	}

	private AppData toAppData(BackendSession bs, BodyOptions bodyParams, MailFolder folder, String id) {
		EmailResponse er = EmailManager.getInstance().loadStructure(bs, folder, Integer.parseInt(id));
		LazyLoaded<BodyOptions, AirSyncBaseResponse> bodyProvider = BodyLoaderFactory.from(bs, folder,
				Integer.parseInt(id), bodyParams);
		AppData ret = AppData.of(er, bodyProvider);
		return ret;
	}

}
