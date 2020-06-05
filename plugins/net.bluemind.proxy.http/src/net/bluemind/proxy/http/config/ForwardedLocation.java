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
package net.bluemind.proxy.http.config;

import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.network.topology.Topology;

public final class ForwardedLocation {

	private final String pathPrefix;
	private String targetUrl;
	private String requiredAuthKind;
	private final ConcurrentHashMap<String, String> whitelist;
	private final String role;
	private final boolean authenticator;

	public static class ResolvedLoc {
		public ResolvedLoc(String host, int port) {
			this.host = host;
			this.port = port;
		}

		public String host;
		public int port;
	}

	public ForwardedLocation(String pathPrefix, String targetUrl, String role, String authenticator) {
		if (pathPrefix == null || targetUrl == null) {
			throw new NullPointerException("path prefix & target url cannot be null");
		}
		if (role == null || role.isEmpty()) {
			this.role = null;
		} else {
			this.role = role;
		}
		this.pathPrefix = pathPrefix;
		this.targetUrl = targetUrl;
		this.requiredAuthKind = AuthKind.NONE.name();
		this.whitelist = new ConcurrentHashMap<>();
		this.authenticator = Boolean.valueOf(authenticator);
	}

	public ResolvedLoc resolve() {
		String tgtUrl = getTargetUrl();
		if (tgtUrl.startsWith("locator://")) {
			int portIndex = tgtUrl.lastIndexOf(':');
			String tag = tgtUrl.substring("locator://".length(), portIndex);
			String host = Topology.get().anyIfPresent(tag).map(s -> s.value.address()).orElse("127.0.0.1");
			int port = Integer.parseInt(tgtUrl.substring(portIndex + 1, tgtUrl.indexOf('/', portIndex)));
			return new ResolvedLoc(host, port);
		} else {
			int portIndex = tgtUrl.lastIndexOf(':');
			String host = tgtUrl.substring("http://".length(), portIndex);
			int port = Integer.parseInt(tgtUrl.substring(portIndex + 1, tgtUrl.indexOf('/', portIndex)));
			return new ResolvedLoc(host, port);
		}
	}

	public String getPathPrefix() {
		return pathPrefix;
	}

	public String getRequiredAuthKind() {
		return requiredAuthKind;
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public void setRequiredAuthKind(String attribute) {
		this.requiredAuthKind = attribute;
	}

	public void whiteList(String uri) {
		whitelist.put(uri, uri);
	}

	public boolean isWhitelisted(String uri) {
		for (String wl : whitelist.keySet()) {
			if (uri.contains(wl)) {
				return true;
			}
		}
		return false;
	}

	public String getRole() {
		return role;
	}

	public boolean isAuthenticator() {
		return authenticator;
	}
}
