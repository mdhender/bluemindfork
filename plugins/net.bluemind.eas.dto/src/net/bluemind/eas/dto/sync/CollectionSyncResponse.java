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
package net.bluemind.eas.dto.sync;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.CollectionItem;

public class CollectionSyncResponse {

	public static class ServerChange {
		public static enum ChangeType {
			Add, Change, Delete, SoftDelete;
		}

		public CollectionItem item;
		public ChangeType type;
		public Optional<AppData> data;

	}

	/**
	 * Response items for client changes & fetch requests
	 */
	public static class ServerResponse {
		public static enum Operation {
			Add, Change, Delete, Fetch;
		}

		public SyncStatus ackStatus;
		public Operation operation;
		public CollectionItem item;

		/**
		 * Non-null for Add operation
		 */
		public String clientId;

		/**
		 * data is present on Fetch
		 */
		public Optional<AppData> fetch;

	}

	public String syncKey;

	/**
	 * global status for all server changes (commands)
	 */
	public SyncStatus status;

	public String collectionId;

	/**
	 * Holds the changes that occured on server
	 */
	public List<ServerChange> commands = new LinkedList<>();

	/**
	 * holds the response items for client changes & fetch requests
	 */
	public List<ServerResponse> responses = new LinkedList<>();

	public boolean moreAvailable;

	public boolean forceResponse;

}
