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
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.typesafe.config.Config;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.configfile.core.CoreConfig;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.lib.vertx.VertxContext;
import net.bluemind.lib.vertx.VertxPlatform;

public class SessionsBackingStore {
	private static final Logger logger = LoggerFactory.getLogger(SessionsBackingStore.class);

	private static final String IDENTITY = UUID.randomUUID().toString();

	public static CacheBackingStore<SecurityContext> build() {
		SessionsBackingStore sessionBackingStore = new SessionsBackingStore();
		Config coreConfig = CoreConfig.get();
		Caffeine<Object, Object> cache = Caffeine.newBuilder().recordStats()
				.expireAfterAccess(coreConfig.getDuration(CoreConfig.Sessions.IDLE_TIMEOUT))
				.removalListener((key, value, removalCause) -> {
					if (removalCause != RemovalCause.REPLACED) {
						sessionBackingStore.notifySessionRemovalListeners((String) key, (SecurityContext) value);
					}
				});

		return new CacheBackingStore<>(cache, coreConfig.getString(CoreConfig.Sessions.STORAGE_PATH),
				sessionBackingStore::toJson, sessionBackingStore::fromJson,
				sessionBackingStore::notifyUnkonwnSessionRemovalListeners);
	}

	private JsonObject toJson(SecurityContext sc) {
		JsonObject jsonObject = new JsonObject();

		jsonObject.put("created", sc.getCreated());
		jsonObject.put("sessionId", sc.getSessionId());
		jsonObject.put("subject", sc.getSubject());
		jsonObject.put("subjectDisplayName", sc.getSubjectDisplayName());
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
		SecurityContext sc = new SecurityContext(jsonObject.getLong("created"), //
				jsonObject.getString("sessionId"), //
				jsonObject.getString("subject"), //
				jsonObject.getString("subjectDisplayName", jsonObject.getString("subject")), //
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
		return json == null ? null : json.stream().map(String.class::cast).collect(Collectors.toList());
	}

	private Context getVertxContext() {
		return VertxContext.getOrCreateDuplicatedContext(VertxPlatform.getVertx());
	}

	private void notifyUnkonwnSessionRemovalListeners(String sessionId) {
		Context vertxContext = getVertxContext();
		for (ISessionDeletionListener listener : SessionDeletionListeners.get()) {
			vertxContext.executeBlocking(() -> {
				listener.deleted(IDENTITY, sessionId, null);
				return null;
			}, true).andThen(asyncResult -> {
				if (!asyncResult.succeeded()) {
					logger.error("Unknown session deletion listener {} failed", listener.getClass().getName(),
							asyncResult.cause());
				}
			});
		}
	}

	private void notifySessionRemovalListeners(String sessionId, SecurityContext securityContext) {
		Context vertxContext = getVertxContext();
		for (ISessionDeletionListener listener : SessionDeletionListeners.get()) {
			vertxContext.executeBlocking(() -> {
				listener.deleted(IDENTITY, sessionId, securityContext);
				return null;
			}, true).andThen(asyncResult -> {
				if (!asyncResult.succeeded()) {
					logger.error("Session deletion listener {} failed", listener.getClass().getName(),
							asyncResult.cause());
				}
			});
		}
	}
}
