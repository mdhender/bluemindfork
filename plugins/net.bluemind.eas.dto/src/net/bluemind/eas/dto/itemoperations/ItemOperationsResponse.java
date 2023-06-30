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
package net.bluemind.eas.dto.itemoperations;

import java.util.LinkedList;
import java.util.List;

import net.bluemind.eas.dto.base.AppData;

public class ItemOperationsResponse {

	public Status status;
	public List<Response> responses = new LinkedList<>();
	public ResponseStyle style;
	public boolean gzip;

	public enum Status {

		SUCCESS(1), //
		PROTOCOLE_ERROR(2), //
		SERVER_ERROR(3), //
		SPECIFIED_URI_BAD(4), //
		ACCESS_DENIED(5), //
		OBJECT_NOT_FOUND(6), //
		FAILED_CONNECT_SERVER(7), //
		BYTE_RANGE_INVALID(8), //
		STORE_UNKNOWN(9), //
		FILE_EMPTY(10), //
		REQUESTED_DATA_TOO_LARGE(11), //
		FAIL_DOWNLOAD_FILE_IO_FAILURE(12), //
		ITEM_FAILED_CONVERSION(14), //
		ATTACHEMENT_INVALID(15), //
		RESOURCE_ACCESS_DENIED(16), //
		PARTIAL_SUCCESS(17), //
		CREDENTIAL_REQUIRED(18), //
		PROTOCOL_ERROR(155), //
		ACTION_NOT_SUPPORTED(156);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static class Response {
		public Status status;
	}

	public static class Move extends Response {
		public String conversationId;
	}

	public static class EmptyFolderContents extends Response {
		public String collectionId;
	}

	public static class Fetch extends Response {
		public String collectionId;
		public String serverId;
		public String longId;
		public String dataClass;
		public String linkId;
		public String fileReference;

		// FIXME choose a type
		public AppData properties;
	}
}
