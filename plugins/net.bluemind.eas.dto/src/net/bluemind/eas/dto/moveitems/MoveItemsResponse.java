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

		public static enum Status {
			InvalidSourceCollectionId(1), // 1 Invalid source collection ID.
			InvalidDestinationCollectionId(2), // 2 Invalid destination
												// collection ID.
			Success(3), // 3 Success
			SameSourceAndDestinationCollectionId(4), // 4 Source and
														// destination
														// collection IDs are
														// the same.
			ServerError(5), // 5 A failure occurred during the MoveItem
							// operation.
			ItemAlreadyExistsAtDestination(6), // 6 An item with that name
												// already exists at the
												// destination.
			SourceOrDestinationLocked(7);// 7 Source or destination item was
											// locked.

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
