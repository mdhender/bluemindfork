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
package net.bluemind.eas.dto.foldersync;

import java.util.ArrayList;
import java.util.List;

public class FolderSyncResponse {

	public static enum Status {

		Success(1), ServerError(6), InvalidSyncKey(9), InvalidRequest(10), //
		UnknownError(11), CodeUnknown(12);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Changes {

		public static final class Change {
			public String serverId;
			public String parentId;
			public String displayName;
			public FolderType type;
		}

		public int count;
		public List<Change> update = new ArrayList<>();
		public List<String> delete = new ArrayList<>();
		public List<Change> add = new ArrayList<>();
	}

	public Status status;
	public String syncKey;
	public Changes changes = new Changes();

	public boolean hasChanges() {
		return !changes.update.isEmpty() || !changes.delete.isEmpty() || !changes.add.isEmpty();
	}
}
