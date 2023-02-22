/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.server.forward;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.network.topology.Topology;

public class ForwardedLocation {
	private static final Logger logger = LoggerFactory.getLogger(ForwardedLocation.class);

	private final String pathPrefix;
	private final boolean auth;
	private final ConcurrentHashMap<String, String> whitelist;
	private final ArrayList<Pattern> regexWhiteList;
	private final String role;
	private boolean cspEnabled;
	private final String targetUrl;

	public ForwardedLocation(String pathPrefix, String target, String auth, String role) {
		if (pathPrefix == null) {
			throw new NullPointerException("pathPrefix cannot be null");
		}
		if (role == null || role.isEmpty()) {
			this.role = null;
		} else {
			this.role = role;
		}
		this.pathPrefix = pathPrefix;
		this.auth = Boolean.valueOf(auth);
		this.whitelist = new ConcurrentHashMap<>();
		this.regexWhiteList = new ArrayList<>();
		this.cspEnabled = true;
		this.targetUrl = target;
	}

	public String getPathPrefix() {
		return pathPrefix;
	}

	public void whiteList(String uri) {
		whitelist.put(uri, uri);
	}

	public void whiteListRegex(String regex) {
		try {
			regexWhiteList.add(Pattern.compile(regex));
		} catch (PatternSyntaxException e) {
			logger.error("invalid regular expression", e);
		}
	}

	public boolean isWhitelisted(String uri) {
		for (String wl : whitelist.keySet()) {
			if (uri.contains(wl)) {
				logger.debug("uri {} contains {}", uri, wl);
				return true;
			}
		}
		for (Pattern p : regexWhiteList) {
			if (p.matcher(uri).matches()) {
				logger.debug("uri {} matches {}", uri, p);
				return true;
			}
		}
		return false;
	}

	public String getRole() {
		return role;
	}

	public void cspEnabled(boolean cspEnabled) {
		this.cspEnabled = cspEnabled;
	}

	public boolean cspEnabled() {
		return cspEnabled;
	}

	public boolean needAuth() {
		return auth;
	}

	public static class ResolvedLoc {
		public ResolvedLoc(String host, int port) {
			this.host = host;
			this.port = port;
		}

		public String host;
		public int port;
	}

	public Optional<ResolvedLoc> resolve() {
		if (Strings.isNullOrEmpty(targetUrl)) {
			return Optional.empty();
		}
		String tgtUrl = targetUrl;
		if (tgtUrl.startsWith("locator://")) {
			int portIndex = tgtUrl.lastIndexOf(':');
			String tag = tgtUrl.substring("locator://".length(), portIndex);
			String host = Topology.get().anyIfPresent(tag).map(s -> s.value.address()).orElse("127.0.0.1");
			int port = Integer.parseInt(tgtUrl.substring(portIndex + 1, tgtUrl.indexOf('/', portIndex)));
			return Optional.of(new ResolvedLoc(host, port));
		} else {
			int portIndex = tgtUrl.lastIndexOf(':');
			String host = tgtUrl.substring("http://".length(), portIndex);
			int port = Integer.parseInt(tgtUrl.substring(portIndex + 1, tgtUrl.indexOf('/', portIndex)));
			return Optional.of(new ResolvedLoc(host, port));
		}
	}

}
