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
package net.bluemind.mailbox.service.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.lib.vertx.VertxPlatform;

public class UnreadMessagesCacheRegistry implements ICacheRegistration {

	private static final Cache<String, Integer> contCache = CacheBuilder.newBuilder().build();

	@Override
	public void registerCaches(CacheRegistry cr) {
		cr.register(UnreadMessagesCacheRegistry.class, contCache);

		VertxPlatform.getVertx().eventBus().consumer("bm.mailbox.hook.changed",
				(Message<JsonObject> event) -> invalidate(event.body().getString("container")));
	}

	public static Integer getIfPresent(String uid) {
		return contCache.getIfPresent(uid);
	}

	public static void put(String uid, Integer c) {
		contCache.put(uid, c);
	}

	public static void invalidate(String uid) {
		contCache.invalidate(uid);
	}

}
