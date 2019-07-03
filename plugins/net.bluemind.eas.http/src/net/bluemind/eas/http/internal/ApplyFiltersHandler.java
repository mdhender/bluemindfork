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
package net.bluemind.eas.http.internal;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestFilter;
import net.bluemind.eas.http.IEasRequestFilter.FilterChain;

public class ApplyFiltersHandler implements Handler<AuthenticatedEASQuery> {

	private Handler<AuthenticatedEASQuery> next;
	private static List<IEasRequestFilter> filters = new ArrayList<>(Filters.get());

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ApplyFiltersHandler.class);

	public ApplyFiltersHandler(Handler<AuthenticatedEASQuery> next) {
		this.next = next;
	}

	@Override
	public void handle(AuthenticatedEASQuery event) {
		int len = filters.size();
		event.request().pause();
		handle(event, 0, len);
	}

	private static class ChainImpl implements FilterChain {
		private final AuthenticatedEASQuery event;
		private final int idx;
		private final int len;
		private final ApplyFiltersHandler self;

		public ChainImpl(ApplyFiltersHandler self, AuthenticatedEASQuery event, int idx, int len) {
			this.event = event;
			this.idx = idx;
			this.len = len;
			this.self = self;
		}

		@Override
		public void filter(AuthenticatedEASQuery query) {
			self.handle(event, idx, len);
		}

		@Override
		public void filter(AuthorizedDeviceQuery query) {
		}
	}

	private void handle(AuthenticatedEASQuery event, final int idx, int len) {
		if (idx >= len) {
			event.request().resume();
			next.handle(event);
		} else {
			IEasRequestFilter filter = filters.get(idx);
			filter.filter(event, new ChainImpl(this, event, idx + 1, len));
		}
	}

}
