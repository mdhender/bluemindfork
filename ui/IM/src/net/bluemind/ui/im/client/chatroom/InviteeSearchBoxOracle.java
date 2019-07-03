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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import net.bluemind.im.api.gwt.endpoint.InstantMessagingGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;

public class InviteeSearchBoxOracle extends SuggestOracle {

	@Override
	public void requestSuggestions(final Request request, final Callback callback) {
		String query = request.getQuery();
		DirEntryQuery dq = new DirEntryQuery();
		dq.nameOrEmailFilter = query;
		dq.kindsFilter = Arrays.asList(Kind.USER, Kind.GROUP);
		dq.size = 10;

		IDirectoryAsync directory = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid());
		directory.search(dq, new AsyncHandler<ListResult<ItemValue<DirEntry>>>() {

			@Override
			public void success(ListResult<ItemValue<DirEntry>> result) {
				final List<InviteeSearchSuggestion> suggestions = new ArrayList<InviteeSearchSuggestion>();
				validateEntry(result.values, suggestions, callback, request);
			}

			@Override
			public void failure(Throwable caught) {
				GWT.log(caught.getMessage());
			}

		});

	}

	protected void validateEntry(final List<ItemValue<DirEntry>> values,
			final List<InviteeSearchSuggestion> suggestions, final Callback callback, final Request request) {

		List<CompletableFuture<?>> futures = new ArrayList<>(values.size());
		for (int i = 0; i < values.size(); i++) {

			final ItemValue<DirEntry> de = values.get(i);

			if (de.value.archived || de.value.system) {
				continue;
			}
			String uid = de.uid;
			if (de.uid.contains("/")) {
				uid = de.uid.substring(de.uid.lastIndexOf("/") + 1);
			}

			switch (de.value.kind) {
			case USER:
				CompletableFuture<?> future = new InstantMessagingGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi()
						.isActiveUser(uid).thenApply(value -> {
							if (value.booleanValue()) {
								suggestions.add(new InviteeSearchSuggestion(de.value));
							}
							return null;
						}).exceptionally(t -> {
							suggestions.add(new InviteeSearchSuggestion(de.value));
							return null;
						});

				futures.add(future);
				break;
			case GROUP:
				suggestions.add(new InviteeSearchSuggestion(de.value));
				break;
			default:
				suggestions.add(new InviteeSearchSuggestion(de.value));
				break;
			}
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
			Response response = new Response();
			response.setSuggestions(suggestions);
			callback.onSuggestionsReady(request, response);
		}).exceptionally(t -> {
			Response response = new Response();
			response.setSuggestions(suggestions);
			callback.onSuggestionsReady(request, response);
			return null;
		});
	}
}
