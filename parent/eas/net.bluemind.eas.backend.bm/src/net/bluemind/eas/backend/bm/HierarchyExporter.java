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

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.FolderChanges;
import net.bluemind.eas.backend.IHierarchyExporter;
import net.bluemind.eas.dto.sync.SyncState;

public class HierarchyExporter implements IHierarchyExporter {

	private final FolderBackend folderBackend;

	public HierarchyExporter(FolderBackend folderBackend) {
		this.folderBackend = folderBackend;
	}

	@Override
	public FolderChanges getChanges(BackendSession bs, SyncState state) throws Exception {
		FolderChanges changes = new FolderChanges();
		changes = folderBackend.getChanges(bs, state);
		return changes;
	}

}
