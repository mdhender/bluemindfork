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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardedLocation {
	private static final Logger logger = LoggerFactory.getLogger(ForwardedLocation.class);

	private final String pathPrefix;
	private final boolean auth;
	private final ConcurrentHashMap<String, String> whitelist;
	private final ArrayList<Pattern> regexWhiteList;
	private final String role;
	private boolean cspEnabled;

	public ForwardedLocation(String pathPrefix, String auth, String role, String authenticator) {
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

}
