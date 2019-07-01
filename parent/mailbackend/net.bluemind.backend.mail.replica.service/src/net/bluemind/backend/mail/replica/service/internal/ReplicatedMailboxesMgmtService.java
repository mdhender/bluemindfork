/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchQuery.SearchScope;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecordItemUri;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.rest.BmContext;
import net.bluemind.index.MailIndexActivator;

public class ReplicatedMailboxesMgmtService implements IReplicatedMailboxesMgmt {

	private static final Logger logger = LoggerFactory.getLogger(ReplicatedMailboxesMgmtService.class);
	private final BmContext context;

	public ReplicatedMailboxesMgmtService(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MailboxRecordItemUri> getBodyGuidReferences(String guid) {
		Set<MailboxRecordItemUri> refs = new HashSet<>();
		readRecordsByGuid(guid, refs);
		return refs;
	}

	@Override
	public List<Set<MailboxRecordItemUri>> getImapUidReferences(String mailbox, Long uid) {
		List<Set<MailboxRecordItemUri>> refs = new ArrayList<Set<MailboxRecordItemUri>>();
		for (DataSource ds : context.getAllMailboxDataSource()) {
			MailboxRecordStore store = new MailboxRecordStore(ds);
			try {
				Set<String> bodyGuids = store.getImapUidReferences(uid, mailbox);
				bodyGuids.forEach(guid -> refs.add(getBodyGuidReferences(guid)));
			} catch (SQLException e) {
				logger.warn("Cannot read referenced message body", e);
			}
		}
		return refs;
	}

	@Override
	public List<Set<MailboxRecordItemUri>> queryReferences(String mailbox, String query) {
		List<Set<MailboxRecordItemUri>> refs = new ArrayList<Set<MailboxRecordItemUri>>();

		MailboxFolderSearchQuery indexQuery = new MailboxFolderSearchQuery();
		indexQuery.query = new SearchQuery();
		indexQuery.query.maxResults = 999;
		indexQuery.query.query = query;
		indexQuery.query.scope = new SearchScope();
		SearchResult searchResult = MailIndexActivator.getService().searchItems(mailbox, indexQuery);

		Set<String> bodyGuids = new HashSet<>();
		searchResult.results.forEach(ret -> {
			context.getAllMailboxDataSource().forEach(ds -> {
				MailboxRecordStore store = new MailboxRecordStore(ds);
				try {
					MailboxRecord mailboxRecord = store.get(Item.create(null, ret.itemId));
					if (mailboxRecord != null) {
						bodyGuids.add(mailboxRecord.messageBody);
					}
				} catch (SQLException e) {
					logger.warn("Cannot read referenced message body", e);
				}
			});

		});
		bodyGuids.forEach(guid -> refs.add(getBodyGuidReferences(guid)));

		return refs;
	}

	private void readRecordsByGuid(String guid, Set<MailboxRecordItemUri> refs) {
		select(refs, store -> {
			try {
				return store.getBodyGuidReferences(guid);
			} catch (SQLException e) {
				logger.warn("Cannot read referenced message bodies by imap-uid", e);
				return Collections.emptyList();
			}
		});
	}

	private void select(Set<MailboxRecordItemUri> refs, Function<MailboxRecordStore, List<MailboxRecordItemUri>> func) {
		context.getAllMailboxDataSource().forEach(ds -> {
			MailboxRecordStore store = new MailboxRecordStore(ds);
			refs.addAll(func.apply(store));
		});
	}

}
