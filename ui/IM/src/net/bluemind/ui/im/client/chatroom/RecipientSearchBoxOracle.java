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
package net.bluemind.ui.im.client.chatroom;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.SuggestOracle;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectoryAsync;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;

public class RecipientSearchBoxOracle extends SuggestOracle {

	@Override
	public void requestSuggestions(final Request request, final Callback callback) {
		DirEntryQuery cq = DirEntryQuery.filterKind(Kind.USER, Kind.GROUP, Kind.MAILSHARE);
		cq.from = 0;
		cq.size = 10;
		cq.nameFilter = request.getQuery();
		IDirectoryAsync dir = new DirectoryGwtEndpoint(net.bluemind.ui.common.client.forms.Ajax.TOKEN.getSessionId(),
				Ajax.TOKEN.getContainerUid());

		dir.search(cq, new AsyncHandler<ListResult<ItemValue<DirEntry>>>() {

			@Override
			public void success(ListResult<ItemValue<DirEntry>> value) {
				Response response = new Response();
				List<RecipientSearchSuggestion> suggestions = new ArrayList<RecipientSearchSuggestion>();
				for (ItemValue<DirEntry> c : value.values) {
					if (c.value.email != null) {
						suggestions.add(new RecipientSearchSuggestion(c));
					}
				}
				response.setSuggestions(suggestions);
				callback.onSuggestionsReady(request, response);
			}

			@Override
			public void failure(Throwable e) {
				GWT.log(e.getMessage());
			}
		});

	}
}
