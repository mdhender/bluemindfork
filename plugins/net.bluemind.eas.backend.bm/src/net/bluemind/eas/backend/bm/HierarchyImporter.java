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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IHierarchyImporter;
import net.bluemind.eas.backend.SyncFolder;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;

public class HierarchyImporter implements IHierarchyImporter {

	private static final Logger logger = LoggerFactory.getLogger(HierarchyImporter.class);

	private FolderBackend folderBackend;

	public HierarchyImporter(FolderBackend folderBackend) {
		this.folderBackend = folderBackend;
	}

	@Override
	public Long importFolderCreate(BackendSession bs, HierarchyNode parent, SyncFolder sf) {
		Long id = null;
		switch (sf.getPimDataType()) {
		case CALENDAR:
			id = folderBackend.createFolder(bs, ItemDataType.CALENDAR, sf.getDisplayName());
			break;
		case CONTACTS:
			logger.info("Create contacts folder is not implemented");
			break;
		case EMAIL:
			id = folderBackend.createMailFolder(bs, parent, sf);
			break;
		case TASKS:
			id = folderBackend.createFolder(bs, ItemDataType.TASKS, sf.getDisplayName());
			break;
		default:
			break;
		}
		return id;
	}

	@Override
	public boolean importFolderDelete(BackendSession bs, int serverId) {
		HierarchyNode node = null;
		boolean ret = false;
		try {
			node = folderBackend.getHierarchyNode(bs, serverId);
		} catch (ActiveSyncException e) {
			logger.error(e.getMessage(), e);
		}
		if (node != null) {
			switch (ItemDataType.getValue(node.containerType)) {
			case CALENDAR:
				ret = folderBackend.deleteFolder(bs, ItemDataType.CALENDAR, node);
				break;
			case CONTACTS:
				logger.info("Delete contacts folder is not implemented");
				break;
			case EMAIL:
				ret = folderBackend.deleteMailFolder(bs, node);
				break;
			case TASKS:
				ret = folderBackend.deleteFolder(bs, ItemDataType.TASKS, node);
				break;
			default:
				break;
			}
		}
		return ret;
	}

	@Override
	public boolean importFolderUpdate(BackendSession bs, SyncFolder sf) {
		HierarchyNode node = null;
		boolean ret = false;
		try {
			node = folderBackend.getHierarchyNode(bs, sf.getServerId());
		} catch (ActiveSyncException e) {
			logger.error(e.getMessage(), e);
		}

		if (node != null) {
			switch (ItemDataType.getValue(node.containerType)) {
			case CALENDAR:
				ret = folderBackend.updateFolder(bs, ItemDataType.CALENDAR, node, sf.getDisplayName());
				break;
			case CONTACTS:
				logger.info("Update contacts folder is not implemented");
				break;
			case EMAIL:
				ret = folderBackend.updateMailFolder(bs, node, sf.getDisplayName());
				break;
			case TASKS:
				ret = folderBackend.updateFolder(bs, ItemDataType.TASKS, node, sf.getDisplayName());
				break;
			default:
				break;
			}
		}
		return ret;
	}

}
