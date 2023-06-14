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
package net.bluemind.keycloak.api;

public interface IKeycloakUids {
	public static final String KEYCLOAK_FLOW_ALIAS = "browser";
	public static final String BLUEMIND_FLOW_ALIAS = "browser-bluemind";

	public static String defaultHost(String host, String realmId) {
		return String.format("http://%s:8099/realms/%s/.well-known/openid-configuration", host, realmId);
	}

	public static String clientId(String realmId) {
		return realmId + "-cli";
	}

	public static String bmProviderId(String realmId) {
		return realmId + "-bmprovider";
	}

	public static String kerberosComponentName(String realmId) {
		return realmId + "-kerberos";
	}

}
