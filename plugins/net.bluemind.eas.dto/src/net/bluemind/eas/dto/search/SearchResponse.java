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

public class SearchResponse {

	public static enum Status {

		Success(1), ServerError(2);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Store {
		public static enum Status {

			Success(1), InvalidRequest(2), ServerError(2), //
			BadLink(4), AccessDenied(5), NotFound(6), ConnectionFailed(7), //
			TooComplex(8), TimeOut(10), FolderSyncRequired(11), EndOfRetrievableRange(12), //
			AccessBlocked(13), CredentialsRequired(14);

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
