/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.sessions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.lib.vertx.VertxPlatform;

public class SessionsBackingStore {
	private static final Logger logger = LoggerFactory.getLogger(SessionsBackingStore.class);

	private final String IDENTITY = UUID.randomUUID().toString();

	public static CacheBackingStore<SecurityContext> build() {
		SessionsBackingStore sessionBackingStore = new SessionsBackingStore();

		Caffeine<Object, Object> cache = Caffeine.newBuilder().recordStats().expireAfterAccess(20, TimeUnit.MINUTES)
				.removalListener((key, value, removalCause) -> {
					if (removalCause != RemovalCause.REPLACED && ((SecurityContext) value).isInteractive()) {
						sessionBackingStore.notifySessionRemovalListeners((String) key, (SecurityContext) value);
					}
				});

		return new CacheBackingStore<SecurityContext>(cache, "/var/cache/bm-core/sessions", sessionBackingStore::toJson,
				sessionBackingStore::fromJson, Optional.of(sessionBackingStore::ignore));
	}

	private JsonObject toJson(SecurityContext sc) {
		JsonObject jsonObject = new JsonObject();

		jsonObject.put("created", System.currentTimeMillis());
		jsonObject.put("sessionId", sc.getSessionId());
		jsonObject.put("subject", sc.getSubject());
		jsonObject.put("domainUid", sc.getContainerUid());
		jsonObject.put("lang", sc.getLang());
		jsonObject.put("origin", sc.getOrigin());
		jsonObject.put("interactive", sc.isInteractive());
		jsonObject.put("ownerPrincipal", sc.getOwnerPrincipal());

		jsonObject.put("memberOf", new JsonArray(sc.getMemberOf()));
		jsonObject.put("roles", new JsonArray(sc.getRoles()));
		jsonObject.put("remoteAddresses", new JsonArray(sc.getRemoteAddresses()));
		jsonObject.put("orgUnitsRoles", sc.getRolesByOrgUnits().entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, e -> new ArrayList<>(e.getValue()))));

		return jsonObject;
	}

	private SecurityContext fromJson(JsonObject jsonObject) {
		SecurityContext sc = new SecurityContext(jsonObject.getString("sessionId"), //
				jsonObject.getString("subject"), //
				jsonArrayToList(jsonObject.getJsonArray("memberOf")), //
				jsonArrayToList(jsonObject.getJsonArray("roles")), //
				jsonObject.getJsonObject("orgUnitsRoles") == null ? null
						: jsonObject.getJsonObject("orgUnitsRoles").getMap().entrySet().stream()
								.filter(e -> e.getKey() != null && e.getValue() != null)
								.collect(Collectors.toMap(Entry::getKey,
										e -> new HashSet<>(jsonArrayToList(new JsonArray((List<?>) e.getValue()))))), //
				jsonObject.getString("domainUid"), //
				jsonObject.getString("lang"), //
				jsonObject.getString("origin"), //
				jsonObject.getBoolean("interactive"), jsonObject.getString("ownerPrincipal"));

		return sc.from(jsonArrayToList(jsonObject.getJsonArray("remoteAddresses")));
	}

	private List<String> jsonArrayToList(JsonArray json) {
		return json == null ? null : json.stream().map(e -> String.class.cast(e)).collect(Collectors.toList());
	}

	private boolean ignore(SecurityContext sc) {
		// Don't cache bmhiddensysadmin sudo token as lots of them are not logged out -
		// no need to keep this sessions alive on restart
		return sc.getSubject().equals("bmhiddensysadmin") && sc.getOrigin().equals("sudo");
	}

	private void notifySessionRemovalListeners(String sessionId, SecurityContext securityContext) {
		for (ISessionDeletionListener listener : SessionDeletionListeners.get()) {
			notifySessionRemovalListener(listener, sessionId, securityContext);
		}
	}

	private void notifySessionRemovalListener(ISessionDeletionListener listener, String sessionId,
			SecurityContext securityContext) {
		VertxPlatform.getVertx().executeBlocking(promise -> {
			try {
				listener.deleted(IDENTITY, sessionId, securityContext);
				promise.complete();
			} catch (Exception e) {
				promise.fail(e);
			}
		}, true, asyncResult -> {
			if (!asyncResult.succeeded()) {
				logger.error("Session deletion listener {} failed", listener.getClass().getName(), asyncResult.cause());
			}
		});
	}
}
