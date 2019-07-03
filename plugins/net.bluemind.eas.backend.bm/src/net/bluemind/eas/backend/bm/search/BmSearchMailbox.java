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
package net.bluemind.eas.backend.bm.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.bm.mail.BodyLoaderFactory;
import net.bluemind.eas.backend.bm.mail.EmailManager;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchResult;
import net.bluemind.eas.dto.search.StoreName;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.search.ISearchSource;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class BmSearchMailbox implements ISearchSource {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	private final ISyncStorage store;

	public BmSearchMailbox() {
		store = Backends.internalStorage();
	}

	@Override
	public StoreName getStoreName() {
		return StoreName.mailbox;
	}

	@Override
	public Results<SearchResult> search(BackendSession bs, SearchRequest request) {

		MailFolder folder = null;
		if (request.store.query.and.collectionId == null) {
			try {
				folder = store.getMailFolderByName(bs, "INBOX");
			} catch (CollectionNotFoundException e) {
				logger.error(e.getMessage(), e);
				return new Results<SearchResult>();
			}
		} else {
			Integer collectionId = Integer.parseInt(request.store.query.and.collectionId);
			try {
				folder = store.getMailFolder(bs, collectionId);
			} catch (CollectionNotFoundException e) {
				logger.error(e.getMessage(), e);
				return new Results<SearchResult>();
			}
		}

		Client client = ESearchActivator.getClient();
		SearchRequestBuilder searchBuilder = client.prepareSearch("mailspool_alias_" + bs.getUser().getUid());

		BoolQueryBuilder bq = QueryBuilders.boolQuery();
		bq.must(QueryBuilders.termQuery("in", folder.uid));
		bq.mustNot(QueryBuilders.termQuery("is", "deleted"));

		String pattern = "content:\"" + request.store.query.and.freeText + "\"";
		searchBuilder.setQuery(
				bq.must(JoinQueryBuilders.hasParentQuery("body", QueryBuilders.queryStringQuery(pattern), false)));

		searchBuilder.addStoredField("itemId");
		searchBuilder.addSort("date", SortOrder.DESC);
		searchBuilder.setFrom(request.store.options.range.min);
		searchBuilder.setSize(request.store.options.range.max);

		logger.debug("{}", searchBuilder);

		SearchResponse sr = searchBuilder.execute().actionGet();
		SearchHits searchHits = sr.getHits();

		List<Integer> uids = new ArrayList<Integer>();
		for (SearchHit sh : searchHits.getHits()) {
			uids.add((Integer) sh.field("itemId").getValue());
		}

		Results<SearchResult> ret = new Results<SearchResult>();
		for (int uid : uids) {
			EmailResponse er = EmailManager.getInstance().loadStructure(bs, folder, uid);
			LazyLoaded<BodyOptions, AirSyncBaseResponse> lazy = BodyLoaderFactory.from(bs, folder, uid,
					request.store.options.bodyOptions);
			AppData appData = AppData.of(er, lazy);
			ret.add(asSearchResult(folder, appData, uid));
		}
		ret.setNumFound(searchHits.getTotalHits());

		return ret;
	}

	private SearchResult asSearchResult(MailFolder folder, AppData appData, long mailUid) {
		final SearchResult res = new SearchResult();
		res.collectionId = folder.collectionId;
		res.clazz = "Email";
		res.longId = (long) res.collectionId << 32 | mailUid & 0xFFFFFFFFL;
		res.searchProperties.email = appData.metadata.email;
		final CountDownLatch cdl = new CountDownLatch(1);
		appData.body.load(new Callback<AirSyncBaseResponse>() {

			@Override
			public void onResult(AirSyncBaseResponse data) {
				res.searchProperties.airSyncBase = data;
				cdl.countDown();
			}
		});
		try {
			cdl.await();
		} catch (InterruptedException e) {
		}

		return res;
	}
}
