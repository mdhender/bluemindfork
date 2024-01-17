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
package net.bluemind.eas.dto.moveitems;

import java.util.List;

public class MoveItemsResponse {

	public List<Response> moveItems;

	public static class Response {
		public String srcMsgId;
		public Status status;
		public String dstMsgId;

		public enum Status {
			INVALID_SOURCE_COLLECTION_ID(1), INVALID_DESTINATION_COLLECTION_ID(2), SUCCESS(3),
			SAME_SOURCE_AND_DESTINATION_COLLECTION_ID(4), SERVER_ERROR(5), ITEM_ALREADY_EXISTS_AT_DESTINATION(6),
			SOURCE_OR_DESTINATION_LOCKED(7);

			private final String xmlValue;

			private Status(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}

		}

	}
}
