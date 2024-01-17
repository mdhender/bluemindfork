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
package net.bluemind.eas.dto.search;

import java.util.List;

import net.bluemind.eas.dto.base.Range;

public class SearchResponse {

	public enum Status {

		SUCCESS(1), SERVER_ERROR(2);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Store {
		public enum Status {

			SUCCESS(1), INVALID_REQUEST(2), SERVER_ERROR(2), //
			BAD_LINK(4), ACCESS_DENIED(5), NOT_FOUND(6), CONNECTION_FAILED(7), //
			TOO_COMPLEX(8), TIMEOUT(10), FOLDER_SYNC_REQUIRED(11), END_OF_RETRIEVABLE_RANGE(12), //
			ACCESS_BLOCKED(13), CREDENTIALS_REQUIRED(14);

			private final String xmlValue;

			private Status(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public Status status;
		public List<SearchResult> results;

	}

	public Status status;
	public Store store;
	public Long total;
	public Range range;

}
