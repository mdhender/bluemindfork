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
package net.bluemind.keycloak.utils.endpoints;

public class KeycloakEndpoints {

	private static final String ROOT = "/keycloak/realms/";

	private static final String BACKEND_ROOT = "http://127.0.0.1:8099/realms/";

	private static final String PROTO = "/protocol/openid-connect";

	private KeycloakEndpoints() {

	}

	public static String tokenEndpoint(String domainUid) {
		return BACKEND_ROOT + domainUid + PROTO + "/token";
	}

	public static String jkwsUriEndpoint(String domainUid) {
		return BACKEND_ROOT + domainUid + PROTO + "/certs";
	}

	public static String authorizationEndpoint(String domainUid) {
		return issuerEndpoint(domainUid) + PROTO + "/auth";
	}

	public static String endSessionEndpoint(String domainUid) {
		return issuerEndpoint(domainUid) + PROTO + "/logout";
	}

	public static String issuerEndpoint(String domainUid) {
		return ROOT + domainUid;
	}

}
