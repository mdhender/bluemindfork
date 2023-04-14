/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.api.utils;

import java.util.List;

import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchQuery;

public class MailIndexQuery extends MailboxFolderSearchQuery {

	public List<String> folderUids;

	public static MailIndexQuery simpleQuery(MailboxFolderSearchQuery searchQuery) {
		return MailIndexQuery.folderQuery(searchQuery, null);
	}

	public static MailIndexQuery folderQuery(MailboxFolderSearchQuery searchQuery, List<String> folderUids) {
		MailIndexQuery query = new MailIndexQuery();
		query.folderUids = folderUids;
		query.sort = searchQuery.sort;
		if (searchQuery.query != null) {
			query.query = new SearchQuery();
			query.query.searchSessionId = searchQuery.query.searchSessionId;
			query.query = searchQuery.query;
			query.query.recordQuery = searchQuery.query.recordQuery;
			query.query.messageId = searchQuery.query.messageId;
			query.query.references = searchQuery.query.references;
			query.query.headerQuery = searchQuery.query.headerQuery;
			query.query.maxResults = searchQuery.query.maxResults;
			query.query.offset = searchQuery.query.offset;
			query.query.scope = searchQuery.query.scope;
		}
		return query;
	}

}
