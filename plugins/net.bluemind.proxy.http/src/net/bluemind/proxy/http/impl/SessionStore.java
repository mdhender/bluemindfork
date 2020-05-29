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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.proxy.http.auth.api.IAuthEnforcer.IAuthProtocol;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;

/**
 * Stores mapping between our cookie and the custom auth session id.
 * 
 * 
 */
public class SessionStore implements ISessionStore {

	private ConcurrentMap<String, String> cookieSids;
	private ConcurrentMap<String, String> sidCookies;
	private ConcurrentMap<String, IAuthProtocol> sidProtocol;

	private static final Logger logger = LoggerFactory.getLogger(SessionStore.class);

	public SessionStore() {

		cookieSids = new ConcurrentHashMap<>();
		sidCookies = new ConcurrentHashMap<>();
		sidProtocol = new ConcurrentHashMap<>();
	}

	public String getOrAllocateCookie(String sessionId, IAuthProtocol protocol) {
		String ret = sidCookies.get(sessionId);

		if (ret == null) {
			StringBuilder cookie = new StringBuilder();
			cookie.append(UUID.randomUUID().toString());
			// cookie.append(System.currentTimeMillis());
			ret = cookie.toString();

			cookieSids.put(ret, sessionId);
			sidCookies.put(sessionId, ret);
			sidProtocol.put(sessionId, protocol);
		}

		return ret;
	}

	public String getSessionId(String cookie) {
		String ret = cookieSids.get(cookie);
		if (ret == null) {
			logger.debug("No session for cookie {}", cookie);
		}
		return ret;
	}

	public void purgeSession(String sessionId) {
		String cookie = sidCookies.remove(sessionId);
		logger.info("purge session {} with cookie {}", sessionId, cookie);
		if (cookie != null) {
			cookieSids.remove(cookie);
		}
		sidProtocol.remove(sessionId);
	}

	@Override
	public String newSession(String providerSession, IAuthProtocol protocol) {
		return getOrAllocateCookie(providerSession, protocol);
	}

	public void purgeAllSessions() {
		cookieSids.clear();
		sidCookies.clear();
		sidProtocol.clear();
	}

	@Override
	public IAuthProtocol getProtocol(String sessionId) {
		if (sessionId == null) {
			return null;
		}

		IAuthProtocol protocol = sidProtocol.get(sessionId);
		if (protocol == null) {
			logger.warn("No protocol for session " + sessionId);
		}

		return protocol;
	}
}
