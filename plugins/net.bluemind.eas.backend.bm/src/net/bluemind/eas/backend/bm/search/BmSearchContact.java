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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooks;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.bm.contacts.ContactConverter;
import net.bluemind.eas.backend.bm.state.InternalState;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchResult;
import net.bluemind.eas.dto.search.StoreName;
import net.bluemind.eas.search.ISearchSource;

/**
 * 
 * 
 */
public class BmSearchContact implements ISearchSource {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	protected String coreHost;

	public StoreName getStoreName() {
		return StoreName.gal;
	}

	public Results<SearchResult> search(BackendSession bs, SearchRequest request) {
		Results<SearchResult> ret = new Results<>();
		try {
			InternalState is = bs.getInternalState();

			IAddressBooks addressBooksService = ClientSideServiceProvider.getProvider(is.coreUrl, is.sid)
					.setOrigin("bm-eas-BmSearchContact-" + bs.getUniqueIdentifier()).instance(IAddressBooks.class);

			VCardQuery query = new VCardQuery();

			// Where is the default search field ???
			String v = request.store.query.value;
			query.query = String
					.format("value.identification.formatedName.value:%s OR value.communications.emails.value:%s", v, v);
			query.escapeQuery = true;

			ListResult<ItemContainerValue<VCardInfo>> result = addressBooksService.search(query);

			logger.debug("Found {} results for query {}", result.values.size(), query.query);

			ContactConverter cc = new ContactConverter();

			for (ItemContainerValue<VCardInfo> value : result.values) {
				logger.info("found uid '{}' in '{}'", value.uid, value.containerUid);
				IAddressBook service = ClientSideServiceProvider.getProvider(is.coreUrl, is.sid)
						.setOrigin("bm-eas-BmSearchContact-" + bs.getUniqueIdentifier())
						.instance(IAddressBook.class, value.containerUid);
				ItemValue<VCard> item = service.getComplete(value.uid);
				SearchResult res = cc.convertToSearchResult(item, request.store.options.picture, service);
				if (res != null) {
					ret.add(res);
				}

			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}
}
