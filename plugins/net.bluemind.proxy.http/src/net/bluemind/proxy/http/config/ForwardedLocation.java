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

public final class ForwardedLocation {

	private final String pathPrefix;
	private String targetUrl;
	private String requiredAuthKind;
	private final ConcurrentHashMap<String, String> whitelist;
	private int port;
	private String host;
	private final String role;
	private final boolean authenticator;

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
		this.whitelist = new ConcurrentHashMap<String, String>();
		this.authenticator = Boolean.valueOf(authenticator);
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

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getRole() {
		return role;
	}

	public boolean isAuthenticator() {
		return authenticator;
	}
}
