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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.google.common.io.ByteSource;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.IContentsImporter;
import net.bluemind.eas.backend.SendMailData;
import net.bluemind.eas.backend.bm.calendar.CalendarBackend;
import net.bluemind.eas.backend.bm.contacts.ContactsBackend;
import net.bluemind.eas.backend.bm.mail.MailBackend;
import net.bluemind.eas.backend.bm.task.TaskBackend;
import net.bluemind.eas.data.calendarenum.AttendeeStatus;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.CollectionSyncRequest.Options.ConflicResolution;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.exception.NotAllowedException;

/**
 * 
 * 
 */
public class ContentsImporter implements IContentsImporter {

	private MailBackend mailBackend;
	private CalendarBackend calBackend;
	private ContactsBackend contactBackend;
	private TaskBackend taskBackend;

	public ContentsImporter(MailBackend mailBackend, CalendarBackend calBackend, ContactsBackend contactBackend,
			TaskBackend taskBackend) {
		this.mailBackend = mailBackend;
		this.calBackend = calBackend;
		this.contactBackend = contactBackend;
		this.taskBackend = taskBackend;
	}

	@Override
	public CollectionItem importMessageChange(BackendSession bs, CollectionId collectionId, ItemDataType type,
			Optional<String> serverId, IApplicationData data, ConflicResolution conflictPolicy, SyncState syncState)
			throws ActiveSyncException {
		CollectionItem ids = null;
		switch (type) {
		case CALENDAR:
			ids = calBackend.store(bs, collectionId, serverId, data, conflictPolicy, syncState);
			break;
		case CONTACTS:
			ids = contactBackend.store(bs, collectionId, serverId, data, conflictPolicy, syncState);
			break;
		case EMAIL:
			ids = mailBackend.store(bs, collectionId, serverId, data);
			break;
		case TASKS:
			ids = taskBackend.store(bs, collectionId, serverId, data, conflictPolicy, syncState);
			break;
		default:
			break;
		}
		return ids;
	}

	@Override
	public void importMessageDeletion(BackendSession bs, ItemDataType type, Collection<CollectionItem> serverIds,
			Boolean moveToTrash) throws ActiveSyncException {
		switch (type) {
		case CALENDAR:
			calBackend.delete(bs, serverIds);
			break;
		case CONTACTS:
			contactBackend.delete(bs, serverIds);
			break;
		case EMAIL:
			mailBackend.delete(bs, serverIds, moveToTrash);
			break;
		case TASKS:
			taskBackend.delete(bs, serverIds);
			break;
		default:
			break;
		}
	}

	@Override
	public List<MoveItemsResponse.Response> importMoveItems(BackendSession bs, ItemDataType type,
			HierarchyNode srcFolder, HierarchyNode dstFolder, List<CollectionItem> items) throws ActiveSyncException {
		switch (type) {
		case CALENDAR:
			return calBackend.move(bs, srcFolder, dstFolder, items);
		case CONTACTS:
			break;
		case EMAIL:
			return mailBackend.move(bs, srcFolder, dstFolder, items);
		case TASKS:
			break;
		default:
			break;
		}

		return null;
	}

	@Override
	public void sendEmail(SendMailData mail) throws ActiveSyncException {
		mailBackend.sendEmail(mail);
	}

	@Override
	public void replyEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String collectionId,
			String serverId, boolean includePrevious) throws ActiveSyncException {
		mailBackend.replyToEmail(bs, mailContent, saveInSent, collectionId, serverId, includePrevious);
	}

	@Override
	public String importCalendarUserStatus(BackendSession bs, String eventUid, AttendeeStatus userResponse,
			Date instanceId) {
		return calBackend.updateUserStatus(bs, eventUid, userResponse, instanceId);
	}

	@Override
	public void forwardEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String collectionId,
			String serverId, boolean includePrevious) throws ActiveSyncException {
		mailBackend.forwardEmail(bs, mailContent, saveInSent, collectionId, serverId, includePrevious);
	}

	@Override
	public void emptyFolderContent(BackendSession bs, HierarchyNode node, boolean deleteSubFolder)
			throws CollectionNotFoundException, NotAllowedException {
		if (ItemDataType.getValue(node.containerType) == ItemDataType.EMAIL) {
			mailBackend.purgeFolder(bs, node, deleteSubFolder);
		} else {
			throw new NotAllowedException("emptyFolderContent is only supported for emails, collection was "
					+ ItemDataType.getValue(node.containerType));
		}

	}

}
