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
package net.bluemind.eas.store;

import java.util.List;
import java.util.Map;

import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.FolderSyncVersions;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.dto.device.DeviceId;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.CollectionNotFoundException;

public interface ISyncStorage {

	long findLastHeartbeat(DeviceId deviceId);

	void updateLastHearbeat(DeviceId deviceId, long hearbeat);

	/**
	 * Update device lastSync
	 * 
	 * @param bs
	 */
	void updateLastSync(BackendSession bs);

	/**
	 * Fetches the value of an EAS system conf.
	 * 
	 * @param key
	 * @return the value or null if no value is defined
	 */
	String getSystemConf(String key);

	/**
	 * @return
	 */
	List<String> getWipedDevices();

	// Folder

	HierarchyNode getHierarchyNode(String origin, String domainUid, String userUid, String nodeUid)
			throws CollectionNotFoundException;

	HierarchyNode getHierarchyNode(BackendSession bs, CollectionId collectionId) throws CollectionNotFoundException;

	MailFolder getMailFolder(BackendSession bs, CollectionId collectionId) throws CollectionNotFoundException;

	MailFolder getMailFolderByName(BackendSession bs, String name) throws CollectionNotFoundException;

	/**
	 * Create + autosubscription
	 * 
	 * @param bs
	 * @param folderName
	 * @return
	 */
	CollectionId createFolder(BackendSession bs, ItemDataType type, String folderName);

	boolean deleteFolder(BackendSession bs, ItemDataType type, HierarchyNode node);

	boolean updateFolder(BackendSession bs, ItemDataType type, HierarchyNode node, String folderName);

	// Reset
	boolean needReset(BackendSession bs);

	void resetFolder(BackendSession bs);

	/**
	 * Saves the client used by SendMail to identity resends
	 * 
	 * @param clientId
	 */
	void insertClientId(String clientId);

	/**
	 * Returns true if the clientId is known and the email must not be sent by the
	 * SendMail command
	 * 
	 * @param clientId
	 * @return
	 */
	boolean isKnownClientId(String clientId);

	// Folder Sync
	public void setFolderSyncVersions(FolderSyncVersions versions);

	public Map<String, String> getFolderSyncVersions(Account account);

}
