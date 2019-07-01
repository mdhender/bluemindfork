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

		Success(1), //
		ProtocoleError(2), //
		ServerError(3), //
		SpecifiedURIBad(4), //
		AccessDenied(5), //
		ObjectNotFound(6), //
		FailedConnectServer(7), //
		ByteRangeInvalid(8), //
		StoreUnknown(9), //
		FileEmpty(10), //
		RequestedDataTooLarge(11), //
		FailDownloadFileIOFailure(12), //
		ItemFailedConversion(14), //
		AttachementInvalid(15), //
		ResourceAccessDenied(16), //
		PartialSuccess(17), //
		CredentialRequired(18), //
		ProtocolError(155), //
		ActionNotSupported(156);

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
