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

/**
 * Possible values for the status element in Sync responses
 */
public enum SyncStatus {

	OK(1), // 1
	PROTOCOL_VERSION_MISMATCH(2), // 2
	INVALID_SYNC_KEY(3), // 3
	PROTOCOL_ERROR(4), // 4
	SERVER_ERROR(5), // 5
	CONVERSATION_ERROR(6), // 6
	CONFLICT(7), // 7 Conflict matching the client and server object.
	OBJECT_NOT_FOUND(8), // 8
	OUT_OF_DISK_SPACE(9), // 9
	NOTIFICATION_GUID_ERROR(10), // 10
	NOT_YET_PROVISIONNED(11), // 11
	HIERARCHY_CHANGED(12), // 12
	PARTIAL_REQUEST(13), // 13
	WAIT_INTERVAL_OUT_OF_RANGE(14), // 14
	TOO_MUCH_FOLDER_TO_MONITOR(15), // 15
	NEED_RETRY(16); // 16

	private final String value;

	private SyncStatus(int intValue) {
		value = Integer.toString(intValue);
	}

	public String asXmlValue() {
		return value;
	}
}
