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
package net.bluemind.ui.common.client.forms.autocomplete;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.SuggestOracle;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;

public class EntitySuggestOracle<T, TQ> extends SuggestOracle {

	private static final int DELAY = 100; // ms

	protected IEntityFinder<T, TQ> finder;

	private Request request;
	private Callback callback;
	private Timer timer;

	public EntitySuggestOracle() {
		this.timer = new Timer() {

			@Override
			public void run() {
				if (request != null && callback != null) {
					doFind(request, callback);
				}
			}
		};

	}

	public EntitySuggestOracle(IEntityFinder<T, TQ> finder) {
		this();
		setFinder(finder);
	}

	public void setFinder(IEntityFinder<T, TQ> finder) {
		this.finder = finder;
	}

	@Override
	public void requestSuggestions(Request request, Callback callback) {
		this.request = request;
		this.callback = callback;
		timer.cancel();
		if (request.getQuery().trim().length() > 0) {
			timer.schedule(DELAY);
		}
	}

	private void doFind(final Request request, final Callback callback) {
		final String query = request.getQuery();
		TQ qObject = finder.queryFromString(query);

		finder.find(qObject, new AsyncHandler<ListResult<T>>() {

			@Override
			public void success(ListResult<T> value) {
				Collection<Suggestion> sg = getSuggestions(query, value);
				Response response = new Response(sg);
				callback.onSuggestionsReady(request, response);
			}

			@Override
			public void failure(Throwable e) {
				// TODO Auto-generated method stub

			}
		});

	}

	@Override
	public boolean isDisplayStringHTML() {
		return true;
	}

	/**
	 * @param query
	 * @param result
	 * @return
	 */
	protected Collection<Suggestion> getSuggestions(final String query, ListResult<T> result) {
		Collection<Suggestion> sg = new ArrayList<SuggestOracle.Suggestion>(result.values.size());
		for (T t : result.values) {
			sg.add(new EntitySuggestion<T, TQ>(t, finder, query));
		}
		return sg;
	}
}
