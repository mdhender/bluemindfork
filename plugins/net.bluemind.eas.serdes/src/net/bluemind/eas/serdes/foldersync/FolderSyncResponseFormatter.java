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

import java.util.Collections;
import java.util.Comparator;

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

				Collections.sort(response.changes.update, c());
				for (Change c : response.changes.update) {
					builder.container("Update");
					builder.text("ServerId", Long.toString(c.serverId));
					builder.text("ParentId", Long.toString(c.parentId));
					builder.text("DisplayName", c.displayName);
					builder.text("Type", Integer.toString(c.type.asInt()));
					builder.endContainer();
				}
				Collections.sort(response.changes.delete, cs());
				for (Long s : response.changes.delete) {
					builder.container("Delete");
					builder.text("ServerId", Long.toString(s));
					builder.endContainer();
				}
				Collections.sort(response.changes.add, c());
				for (Change c : response.changes.add) {
					builder.container("Add");
					builder.text("ServerId", Long.toString(c.serverId));
					builder.text("ParentId", Long.toString(c.parentId));
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

	private Comparator<? super Long> cs() {
		return new Comparator<Long>() {

			@Override
			public int compare(Long o1, Long o2) {
				return (o1 < o2) ? -1 : 1;
			}
		};
	}

	private Comparator<? super Change> c() {
		return new Comparator<Change>() {

			@Override
			public int compare(Change o1, Change o2) {
				long s1 = o1.serverId;
				long s2 = o2.serverId;
				return (s1 < s2) ? -1 : 1;
			}
		};
	}

}
