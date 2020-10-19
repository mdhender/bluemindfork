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
package net.bluemind.eas.backend;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.google.common.io.ByteSource;

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
 * Content management interface, ie. CRUD API.
 * 
 * 
 */
public interface IContentsImporter {

	CollectionItem importMessageChange(BackendSession bs, CollectionId collectionId, ItemDataType type,
			Optional<String> serverId, IApplicationData data, ConflicResolution conflictPolicy, SyncState syncState)
			throws ActiveSyncException;

	void importMessageDeletion(BackendSession bs, ItemDataType type, Collection<CollectionItem> serverIds,
			Boolean moveToTrash) throws ActiveSyncException;

	List<MoveItemsResponse.Response> importMoveItems(BackendSession bs, ItemDataType type, HierarchyNode srcFolder,
			HierarchyNode dstFolder, List<CollectionItem> items) throws ActiveSyncException;

	void sendEmail(SendMailData mail) throws ActiveSyncException;

	void replyEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String collectionId, String serverId,
			boolean includePrevious) throws ActiveSyncException;

	String importCalendarUserStatus(BackendSession bs, long itemId, AttendeeStatus userResponse, Date instanceId);

	void forwardEmail(BackendSession bs, ByteSource mailContent, Boolean saveInSent, String collectionId,
			String serverId, boolean includePrevious) throws ActiveSyncException;

	void emptyFolderContent(BackendSession bs, HierarchyNode node, CollectionId collectionId, boolean deleteSubFolder)
			throws CollectionNotFoundException, NotAllowedException;

}
