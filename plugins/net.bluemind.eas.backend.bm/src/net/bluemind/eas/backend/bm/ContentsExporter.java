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
package net.bluemind.eas.backend.bm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.IContentsExporter;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSAttachementData;
import net.bluemind.eas.backend.bm.calendar.CalendarBackend;
import net.bluemind.eas.backend.bm.contacts.ContactsBackend;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.backend.bm.mail.AttachmentHelper;
import net.bluemind.eas.backend.bm.mail.MailBackend;
import net.bluemind.eas.backend.bm.task.TaskBackend;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.email.AttachmentResponse;
import net.bluemind.eas.dto.find.FindRequest;
import net.bluemind.eas.dto.find.FindResponse.Response;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient.Availability;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient.Type;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.FilterType;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.exception.ObjectNotFoundException;

public class ContentsExporter extends CoreConnect implements IContentsExporter {

	private MailBackend mailBackend;
	private CalendarBackend calBackend;
	private ContactsBackend contactsBackend;
	private TaskBackend taskBackend;
	private Map<String, FilterType> filterTypeCache;

	public ContentsExporter(MailBackend mailBackend, CalendarBackend calendarExporter, ContactsBackend contactsBackend,
			TaskBackend taskBackend) {
		this.mailBackend = mailBackend;
		this.calBackend = calendarExporter;
		this.contactsBackend = contactsBackend;
		this.taskBackend = taskBackend;
		filterTypeCache = new ConcurrentHashMap<>();
	}

	private boolean processFilterType(BackendSession bs, SyncState state, FilterType filterType,
			CollectionId collectionId) {
		String key = bs.getDeviceId().getIdentifier() + "-" + collectionId.getFolderId();

		if (filterType == null) {
			// iOS no filtertype == ALL_ITEMS
			filterType = FilterType.ALL_ITEMS;
		}

		FilterType current = null;
		if (filterTypeCache.containsKey(key)) {
			current = filterTypeCache.get(key);
		}

		boolean hasChanged = current != null && current != filterType;
		filterTypeCache.put(key, filterType);
		filterType.filter(state, hasChanged);

		return hasChanged;
	}

	@Override
	public Changes getChanged(BackendSession bs, SyncState state, FilterType filterType, CollectionId collectionId)
			throws ActiveSyncException {
		Changes changes = new Changes();
		switch (state.type) {
		case CALENDAR:
			changes = calBackend.getContentChanges(bs, state.version, collectionId);
			break;
		case CONTACTS:
			changes = contactsBackend.getContentChanges(bs, state.version, collectionId);
			break;
		case EMAIL:
			boolean hasFilterTypeChanged = processFilterType(bs, state, filterType, collectionId);
			changes = mailBackend.getContentChanges(bs, state, collectionId, hasFilterTypeChanged);
			break;
		case TASKS:
			changes = taskBackend.getContentChanges(bs, state.version, collectionId);
			break;
		default:
			break;
		}

		logger.info("Get changes from version {} collectionId {}, type {}, changes {}. New version {}", state.version,
				collectionId, state.type, changes.items.size(), changes.version);

		return changes;
	}

	@Override
	public AppData loadStructure(BackendSession bs, BodyOptions bodyOptions, ItemChangeReference ic)
			throws ActiveSyncException {
		switch (ic.getType()) {
		case CALENDAR:
			return calBackend.fetch(bs, ic);
		case CONTACTS:
			return contactsBackend.fetch(bs, ic);
		case EMAIL:
			return mailBackend.fetch(bs, bodyOptions, ic);
		case TASKS:
			return taskBackend.fetch(bs, ic);
		default:
			throw new ActiveSyncException("Unsupported dataType " + ic.getType());
		}
	}

	@Override
	public Map<Long, AppData> loadStructures(BackendSession bs, BodyOptions bodyOptions, ItemDataType type,
			CollectionId collectionId, List<Long> ids) throws ActiveSyncException {
		switch (type) {
		case CALENDAR:
			return calBackend.fetchMultiple(bs, collectionId, ids);
		case CONTACTS:
			return contactsBackend.fetchMultiple(bs, collectionId, ids);
		case TASKS:
			return taskBackend.fetchMultiple(bs, collectionId, ids);
		case EMAIL:
			return mailBackend.fetchMultiple(bs, bodyOptions, collectionId, ids);
		default:
			throw new ActiveSyncException("Unsupported dataType " + type);

		}
	}

	@Override
	public MSAttachementData getAttachment(BackendSession bs, String attachmentId) throws ActiveSyncException {

		Map<String, String> parsedAttId = AttachmentHelper.parseAttachmentId(attachmentId);
		String type = parsedAttId.get(AttachmentHelper.TYPE);

		if (AttachmentHelper.BM_FILEHOSTING_EVENT.equals(type)) {
			return calBackend.getAttachment(bs, parsedAttId);
		} else {
			return mailBackend.getAttachment(bs, attachmentId);
		}

	}

	@Override
	public AttachmentResponse getAttachmentMetadata(BackendSession bs, String attachmentId)
			throws ObjectNotFoundException {
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

	@Override
	public List<Recipient> resolveRecipients(BackendSession bs, List<String> emails,
			ResolveRecipientsRequest.Options.Picture picture) {
		IDirectory dir = getService(bs, IDirectory.class, bs.getUser().getDomain());
		List<Recipient> ret = new ArrayList<>(emails.size());
		try {
			for (String to : emails) {
				Recipient recip = new Recipient();
				recip.to = to;
				DirEntry dirEntry = dir.getByEmail(to);
				if (dirEntry != null) {
					recip.emailAddress = to;
					recip.type = Type.GAL;
					recip.displayName = dirEntry.displayName;
					recip.entryUid = dirEntry.entryUid;
					if (picture != null) {
						ResolveRecipientsResponse.Response.Recipient.Picture pic = new ResolveRecipientsResponse.Response.Recipient.Picture();
						pic.status = ResolveRecipientsResponse.Response.Recipient.Picture.Status.NoPhoto;
						recip.picture = pic;
					}
				} else {
					recip.emailAddress = to;
					recip.type = Type.Contact;
					recip.displayName = to;
					if (picture != null) {
						ResolveRecipientsResponse.Response.Recipient.Picture pic = new ResolveRecipientsResponse.Response.Recipient.Picture();
						pic.status = ResolveRecipientsResponse.Response.Recipient.Picture.Status.NoPhoto;
						recip.picture = pic;
					}
				}

				ret.add(recip);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return ret;
	}

	@Override
	public Availability fetchAvailability(BackendSession bs, String entryUid, Date startTime, Date endTime) {
		return calBackend.fetchAvailability(bs, entryUid, startTime, endTime);
	}

	@Override
	public Response find(BackendSession bs, FindRequest query) throws CollectionNotFoundException {
		return mailBackend.find(bs, query);
	}

}
