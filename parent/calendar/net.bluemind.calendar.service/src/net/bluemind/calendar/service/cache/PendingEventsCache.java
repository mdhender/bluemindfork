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
package net.bluemind.calendar.service.cache;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.vertx.VertxPlatform;

public class PendingEventsCache implements ICacheRegistration {
	private static final Cache<String, ListResult<ItemValue<VEventSeries>>> contCache = CacheBuilder.newBuilder()
			.recordStats()
			.expireAfterWrite(5, TimeUnit.MINUTES)
			.build();

	@Override
	public void registerCaches(CacheRegistry cr) {
		cr.register(PendingEventsCache.class, contCache);

		VertxPlatform.getVertx().eventBus().consumer(CalendarHookAddress.CHANGED, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				invalidate(event.body().getString("container"));
			}
		});
	}

	public static ListResult<ItemValue<VEventSeries>> getIfPresent(String uid) {
		return contCache.getIfPresent(uid);
	}

	public static void put(String uid, ListResult<ItemValue<VEventSeries>> c) {
		contCache.put(uid, c);
	}

	public static void invalidate(String uid) {
		contCache.invalidate(uid);
	}

}
