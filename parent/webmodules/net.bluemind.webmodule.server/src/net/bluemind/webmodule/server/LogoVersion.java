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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.server;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.http.HttpServerRequest;

// FIXME move me in the correct plugin
public class LogoVersion {

	private static final String URI = "images/logo-bluemind.png";
	private static Map<String, String> logos = new HashMap<String, String>();

	public static String getUri() {
		return URI;
	}

	public static String getVersion(HttpServerRequest request) {
		String domainUid = request.headers().get("BMUserDomainId");
		if (logos.containsKey(domainUid)) {
			return logos.get(domainUid);
		}

		if (logos.containsKey("installation")) {
			return logos.get("installation");
		}

		return logos.get("default");
	}

	/**
	 * @param identifier
	 *            "installation" for the global logo, or domainUid
	 * @param version
	 */
	public static void setVersion(String identifier, String version) {
		logos.put(identifier, version);
	}

	public static void deleteVersion(String identifier) {
		logos.remove(identifier);
	}
}
