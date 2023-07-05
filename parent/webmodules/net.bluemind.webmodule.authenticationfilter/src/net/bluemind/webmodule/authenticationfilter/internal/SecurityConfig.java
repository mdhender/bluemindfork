/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.webmodule.authenticationfilter.internal;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SecurityConfig {

	public static final boolean secureCookies;
	public static final boolean cspHeader;

	static {
		secureCookies = !Files.exists(Paths.get("/root/dev-unsecure-cookies"));
		cspHeader = !Files.exists(Paths.get("/root/dev-no-csp"));
	}

	private SecurityConfig() {

	}

}
