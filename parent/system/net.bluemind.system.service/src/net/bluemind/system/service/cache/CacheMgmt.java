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
package net.bluemind.system.service.cache;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ICacheMgmt;

public class CacheMgmt implements ICacheMgmt {

	private static final Logger logger = LoggerFactory.getLogger(CacheMgmt.class);
	private final RBACManager rbac;
	private final BmContext context;

	public CacheMgmt(BmContext context) {
		this.context = context;
		rbac = new RBACManager(context);
	}

	@Override
	public void flushCaches() throws ServerFault {
		rbac.check(BasicRoles.ROLE_MANAGE_SYSTEM_CONF);
		OOPMessage hqMsg = MQ.newMessage();
		MQ.getProducer(Topic.CACHE_FLUSH).send(hqMsg);
	}

	@Override
	public Stream dumpContent() {
		CacheRegistry reg = context.provider().instance(CacheRegistry.class);
		// Using a treemap to have key sorting
		Map<String, Cache<?, ?>> content = new TreeMap<>(reg.getAll());

		JsonObject root = new JsonObject();
		for (Entry<String, Cache<?, ?>> e : content.entrySet()) {

			Map<?, ?> map = e.getValue().asMap();
			if (map == null) {
				logger.warn("Cache {} returned null map", e.getKey());
				continue;
			}
			if (map.isEmpty()) {
				root.put(e.getKey(), new JsonObject());
				continue;
			}
			String serialized = JsonUtils.asString(map);
			if (serialized != null) {
				JsonObject c = new JsonObject(JsonUtils.asString(e.getValue().asMap()));
				root.put(e.getKey(), c);
			} else {
				logger.warn("Cache {} serialized to null string ({} values)", e.getKey(), e.getValue().estimatedSize());
			}
		}
		return VertxStream.stream(root.toBuffer());
	}
}
