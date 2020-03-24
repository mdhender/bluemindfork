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
package net.bluemind.eas.serdes.foldersync;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.foldersync.FolderSyncResponse;
import net.bluemind.eas.dto.foldersync.FolderSyncResponse.Changes.Change;
import net.bluemind.eas.dto.foldersync.FolderSyncResponse.Status;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class FolderSyncResponseFormatter implements IEasResponseFormatter<FolderSyncResponse> {

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, FolderSyncResponse response,
			Callback<Void> completion) {
		builder.start(NamespaceMapping.FolderSync).text("Status", response.status.xmlValue());

		if (response.syncKey != null) {
			builder.text("SyncKey", response.syncKey);
		}

		if (response.status == Status.Success) {
			builder.container("Changes");

			if (response.hasChanges()) {
				builder.text("Count", Integer.toString(response.changes.count));

				for (Change c : response.changes.update) {
					builder.container("Update");
					builder.text("ServerId", c.serverId);
					builder.text("ParentId", c.parentId);
					builder.text("DisplayName", c.displayName);
					builder.text("Type", Integer.toString(c.type.asInt()));
					builder.endContainer();
				}
				for (String s : response.changes.delete) {
					builder.container("Delete");
					builder.text("ServerId", s);
					builder.endContainer();
				}
				for (Change c : response.changes.add) {
					builder.container("Add");
					builder.text("ServerId", c.serverId);
					builder.text("ParentId", c.parentId);
					builder.text("DisplayName", c.displayName);
					builder.text("Type", Integer.toString(c.type.asInt()));
					builder.endContainer();
				}

			} else {
				builder.text("Count", "0");
			}
			builder.endContainer();
		}
		builder.end(completion);
	}

}
