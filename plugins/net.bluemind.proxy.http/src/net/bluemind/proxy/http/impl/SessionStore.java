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
package net.bluemind.proxy.http.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.IAuthProtocol;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;
import net.bluemind.proxy.http.impl.vertx.SudoProtocol;

/**
 * Stores mapping between our cookie and the custom auth session id.
 * 
 * 
 */
public class SessionStore implements ISessionStore {
	@SuppressWarnings("serial")
	public class SidDataNotFound extends RuntimeException {
	}

	public static class SidData {
		public final String cookie;
		public final IAuthProtocol protocol;
		public boolean needcheck = false;

		public SidData(String cookie, IAuthProtocol protocol) {
			this.cookie = cookie;
			this.protocol = protocol;
		}

		public SidData(String cookie, IAuthProtocol protocol, boolean needCheck) {
			this.cookie = cookie;
			this.protocol = protocol;
			this.needcheck = needCheck;
		}

		public SidData checked() {
			this.needcheck = false;
			return this;
		}
	}

	private Optional<List<IAuthEnforcer>> authEnforcers = Optional.empty();

	private final CacheBackingStore<String> cookieSids = new CacheBackingStore<>(Caffeine.newBuilder(),
			"/var/cache/bm-hps/cookies", this::cookieToJson, this::cookieFromJson, Optional.empty());
	private final CacheBackingStore<SidData> sidSidData = new CacheBackingStore<>(Caffeine.newBuilder(),
			"/var/cache/bm-hps/sids", this::sidDataToJson, this::sidDataFromJson, Optional.empty());

	private static final Logger logger = LoggerFactory.getLogger(SessionStore.class);

	private JsonObject cookieToJson(String sid) {
		return new JsonObject().put("s", sid);
	}

	private String cookieFromJson(JsonObject jsonObject) {
		return jsonObject.getString("s");
	}

	private JsonObject sidDataToJson(SidData sidData) {
		return new JsonObject().put("c", sidData.cookie).put("p", sidData.protocol.getKind());
	}

	private SidData sidDataFromJson(JsonObject jsonObject) {
		String cookie = jsonObject.getString("c");
		if (Strings.isNullOrEmpty(cookie)) {
			return null;
		}

		return loadProtocol(jsonObject.getString("p")).map(protocol -> new SidData(cookie, protocol, true))
				.orElse(null);
	}

	private Optional<IAuthProtocol> loadProtocol(String protocolId) {
		if (Strings.isNullOrEmpty(protocolId)) {
			return Optional.empty();
		}

		if (SudoProtocol.KIND.equals(protocolId)) {
			return Optional.of(new SudoProtocol());
		}

		return authEnforcers.flatMap(aes -> aes.stream().map(ae -> ae.getProtocol())
				.filter(p -> p.getKind().equals(protocolId)).findFirst());
	}

	public void addAuthEnforcers(List<IAuthEnforcer> authEnforcers) {
		this.authEnforcers = Optional.ofNullable(authEnforcers);
	}

	public String getOrAllocateCookie(String sessionId, IAuthProtocol protocol) {
		SidData ret = sidSidData.getIfPresent(sessionId);

		if (ret != null) {
			return ret.cookie;
		}

		String cookie = UUID.randomUUID().toString();

		cookieSids.getCache().put(cookie, sessionId);
		sidSidData.getCache().put(sessionId, new SidData(cookie, protocol));

		return cookie;
	}

	public String getSessionId(String cookie) {
		String ret = cookieSids.getIfPresent(cookie);

		if (ret == null && logger.isDebugEnabled()) {
			logger.debug("No session for cookie {}", cookie);
		}
		return ret;
	}

	public void purgeSession(String sessionId) {
		SidData sidData = sidSidData.getIfPresent(sessionId);
		if (sidData == null) {
			return;
		}

		logger.info("purge session {} with cookie {}", sessionId, sidData.cookie);
		sidSidData.getCache().invalidate(sessionId);
		cookieSids.getCache().invalidate(sidData.cookie);
	}

	@Override
	public String newSession(String providerSession, IAuthProtocol protocol) {
		return getOrAllocateCookie(providerSession, protocol);
	}

	@Override
	public IAuthProtocol getProtocol(String sessionId) {
		if (sessionId == null) {
			return null;
		}

		SidData sidData = sidSidData.getIfPresent(sessionId);
		if (sidData == null || sidData.protocol == null) {
			logger.warn("No authentication protocol for session: {}", sessionId);
			return null;
		}

		return sidData.protocol;
	}

	@Override
	public boolean needCheck(String sessionId) {
		return Optional.ofNullable(sidSidData.getIfPresent(sessionId)).map(sidData -> sidData.needcheck)
				.orElseThrow(() -> new SidDataNotFound());
	}

	public void checkAll() {
		sidSidData.getCache().asMap().values().forEach(sidData -> sidData.needcheck = true);
	}

	@Override
	public void checked(String sessionId) {
		Optional.ofNullable(sidSidData.getCache().getIfPresent(sessionId)).map(SidData::checked)
				.ifPresent(sidData -> sidSidData.getCache().put(sessionId, sidData));
	}

	public void cleanUp() {
		sidSidData.cleanUp();
		sidSidData.cleanUpStore();

		cookieSids.cleanUp();
		sidSidData.cleanUpStore();
	}
}
