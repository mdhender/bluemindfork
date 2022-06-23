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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.MessageSearchResult;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchSort;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.config.Token;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.bm.mail.BodyLoaderFactory;
import net.bluemind.eas.backend.bm.mail.EmailManager;
import net.bluemind.eas.backend.bm.state.InternalState;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchResult;
import net.bluemind.eas.dto.search.StoreName;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.search.ISearchSource;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

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
				return new Results<>();
			}
		} else {
			try {
				folder = store.getMailFolder(bs, CollectionId.of(request.store.query.and.collectionId));
			} catch (CollectionNotFoundException e) {
				logger.error(e.getMessage(), e);
				return new Results<>();
			}
		}
		InternalState is = bs.getInternalState();
		ClientSideServiceProvider prov = ClientSideServiceProvider.getProvider(is.coreUrl, Token.admin0())
				.setOrigin("bm-eas-BmSearchMailbox");
		IContainers cont = prov.instance(IContainers.class);
		ContainerDescriptor asContainer = cont.get(IMailReplicaUids.mboxRecords(folder.uid));
		IMailboxes mboxApi = prov.instance(IMailboxes.class, asContainer.domainUid);
		ItemValue<Mailbox> mbox = mboxApi.getComplete(asContainer.owner);
		String subtree = IMailReplicaUids.subtreeUid(asContainer.domainUid, mbox);
		prov = ClientSideServiceProvider.getProvider(is.coreUrl, is.sid).setOrigin("bm-eas-BmSearchMailbox");
		IMailboxFolders folders = prov.instance(IMailboxFoldersByContainer.class, subtree);
		logger.debug("Searching in {}", subtree);

		MailboxFolderSearchQuery mfq = new MailboxFolderSearchQuery();
		mfq.query = new SearchQuery();
		mfq.sort = SearchSort.byField("date", SearchSort.Order.Desc);
		mfq.query.recordQuery = "-is:deleted";
		mfq.query.query = "\"" + request.store.query.and.freeText + "\"";
		mfq.query.scope = new net.bluemind.backend.mail.api.SearchQuery.SearchScope();
		mfq.query.scope.folderScope = new net.bluemind.backend.mail.api.SearchQuery.FolderScope();
		mfq.query.scope.folderScope.folderUid = folder.uid;
		mfq.query.offset = request.store.options.range.min;
		mfq.query.maxResults = request.store.options.range.max;

		net.bluemind.backend.mail.api.SearchResult results = folders.searchItems(mfq);

		List<Integer> uids = new ArrayList<>(results.results.size());
		for (MessageSearchResult r : results.results) {
			uids.add(r.itemId);

		}

		Results<SearchResult> ret = new Results<>();
		for (int uid : uids) {
			EmailResponse er = EmailManager.getInstance().loadStructure(bs, folder, uid);
			LazyLoaded<BodyOptions, AirSyncBaseResponse> lazy = BodyLoaderFactory.from(bs, folder, uid,
					request.store.options.bodyOptions);
			AppData appData = AppData.of(er, lazy);
			ret.add(asSearchResult(folder, appData, uid));
		}
		ret.setNumFound(results.totalResults);

		return ret;
	}

	private SearchResult asSearchResult(MailFolder folder, AppData appData, long mailUid) {
		final SearchResult res = new SearchResult();
		res.collectionId = folder.collectionId;
		res.clazz = "Email";
		res.longId = (long) res.collectionId.getFolderId() << 32 | mailUid & 0xFFFFFFFFL;
		res.searchProperties.email = appData.metadata.email;
		final CountDownLatch cdl = new CountDownLatch(1);
		appData.body.load((AirSyncBaseResponse data) -> {
			res.searchProperties.airSyncBase = data;
			cdl.countDown();
		});
		try {
			cdl.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return res;
	}
}
