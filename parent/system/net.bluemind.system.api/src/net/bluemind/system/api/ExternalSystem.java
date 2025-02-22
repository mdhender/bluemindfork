/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.api;

import java.util.Collections;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ExternalSystem {

	public String identifier;

	public String description;

	public AuthKind authKind;

	public Map<String, String> properties;

	public ExternalSystem() {

	}

	public ExternalSystem(String identifier, String description, AuthKind authKind) {
		this(identifier, description, authKind, Collections.emptyMap());
	}

	public ExternalSystem(String identifier, String description, AuthKind authKind, Map<String, String> properties) {
		this.identifier = identifier;
		this.description = description;
		this.authKind = authKind;
		this.properties = properties;
	}

	@BMApi(version = "3")
	public static enum AuthKind {
		NONE, SIMPLE_CREDENTIALS, API_KEY, OPEN_ID_PKCE
	}

}
