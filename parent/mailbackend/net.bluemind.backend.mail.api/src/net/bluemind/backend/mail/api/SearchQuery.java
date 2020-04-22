/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;

/**
 * Defines message search parameters
 */
@BMApi(version = "3")
public class SearchQuery {

	/**
	 * An optional id associated to this query
	 */
	public String searchSessionId;

	/**
	 * An Elasticsearch compliant query string
	 * 
	 * body document only
	 */
	public String query;

	/**
	 * An Elasticsearch compliant query string
	 * 
	 * record document only
	 */
	public String recordQuery;

	/**
	 * Enables the search by the MessageId header
	 */
	public String messageId;
	/**
	 * Enables the search by the References header
	 */
	public String references;
	/**
	 * Enables the search by the specific header values
	 */
	public HeaderQuery headerQuery;
	/**
	 * Maximum results
	 */
	public long maxResults;
	/**
	 * Result Set offset
	 */
	public long offset;
	/**
	 * The scope of this search
	 */
	public SearchScope scope;

	/**
	 * The scope of a search
	 */
	@BMApi(version = "3")
	public static class SearchScope {
		/**
		 * Restricts the search to a specific {@link MailboxFolder}
		 */
		public FolderScope folderScope;
		/**
		 * True if the search is recursive
		 */
		public boolean isDeepTraversal;
	}

	/**
	 * Definition of a {@link MailboxFolder} search restriction
	 */
	@BMApi(version = "3")
	public static class FolderScope {
		/**
		 * UID of the {@link MailboxFolder}
		 */
		public String folderUid;
	}

	/**
	 * Header search
	 */
	@BMApi(version = "3")
	public static class HeaderQuery {

		/**
		 * Defines the search operator of the requested header search values
		 */
		public LogicalOperator logicalOperator;
		/**
		 * List of requested {@link Header}}
		 */
		public List<Header> query;
	}

	/**
	 * Defines the search operator of the requested header search values
	 */
	@BMApi(version = "3")
	public enum LogicalOperator {
		/**
		 * Search matches if ALL requested headers are present having the requested
		 * value
		 */
		AND,
		/**
		 * Search matches if one of requested headers is present having the requested
		 * value
		 */
		OR
	}

	/**
	 * Header key/value pair
	 */
	@BMApi(version = "3")
	public static class Header {

		/**
		 * Header name
		 */
		public String name;
		/**
		 * Header value
		 */
		public String value;
	}

	public static SearchQuery allHeaders(Map<String, String> headerValues, LogicalOperator operator) {
		SearchQuery query = new SearchQuery();
		query.headerQuery = new HeaderQuery();
		query.headerQuery.logicalOperator = operator;
		query.headerQuery.query = headerValues.keySet().stream().map(key -> {
			Header header = new Header();
			header.name = key;
			header.value = headerValues.get(key);
			return header;
		}).collect(Collectors.toList());
		return query;
	}
}
