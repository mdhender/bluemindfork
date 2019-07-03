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

import org.vertx.java.core.Handler;

import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestFilter;
import net.bluemind.eas.http.IEasRequestFilter.FilterChain;

public class AuthorizedDevicesFiltersHandler implements Handler<AuthorizedDeviceQuery> {

	private Handler<AuthorizedDeviceQuery> next;
	private static List<IEasRequestFilter> filters = new ArrayList<>(Filters.get());

	public AuthorizedDevicesFiltersHandler(Handler<AuthorizedDeviceQuery> next) {
		this.next = next;
	}

	@Override
	public void handle(AuthorizedDeviceQuery event) {
		int len = filters.size();
		event.request().pause();
		handle(event, 0, len);
	}

	private static class ChainImpl implements FilterChain {
		private final AuthorizedDeviceQuery event;
		private final int idx;
		private final int len;
		private final AuthorizedDevicesFiltersHandler self;

		public ChainImpl(AuthorizedDevicesFiltersHandler self, AuthorizedDeviceQuery event, int idx, int len) {
			this.event = event;
			this.idx = idx;
			this.len = len;
			this.self = self;
		}

		@Override
		public void filter(AuthenticatedEASQuery query) {
		}

		@Override
		public void filter(AuthorizedDeviceQuery query) {
			self.handle(event, idx, len);
		}
	}

	private void handle(AuthorizedDeviceQuery event, final int idx, int len) {
		if (idx >= len) {
			event.request().resume();
			next.handle(event);
		} else {
			IEasRequestFilter filter = filters.get(idx);
			filter.filter(event, new ChainImpl(this, event, idx + 1, len));
		}
	}

}
