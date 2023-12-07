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

package net.bluemind.core.auditlogs.client.es.config;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.ESearchActivator.Authentication;
import net.bluemind.lib.elasticsearch.ESearchActivator.AuthenticationCredential;

@BMApi(version = "3")
public class AuditLogElasticStoreConfig extends AuditLogConfig {

	private AuditLogElasticStoreConfig() {
		super();
	}

	public static class AuditLogStore {
		private AuditLogStore() {

		}

		public static final String STORE_TYPE = "auditlog.store.type";
		public static final String STORE_HOST = "auditlog.store.server";
		public static final String STORE_PORT = "auditlog.store.port";
	}

	public static ExternalESConfig getExternalEsConfig() {
		String host = getOrDefaultStr(AuditLogStore.STORE_HOST);
		int port = getOrDefaultInt(AuditLogStore.STORE_PORT);
		if (host == null && port == 0) {
			return null;
		}
		return new ExternalESConfig(host, port);
	}

	public static AuthenticationCredential getAuthenticationMethod() {
		if (!AuditLogConfig.get().hasPath("auditlog.store.authentication")) {
			return new AuthenticationCredential(ESearchActivator.Authentication.NONE, null, null);
		}

		if (AuditLogConfig.get().hasPath("auditlog.store.authentication.mode")) {
			String authMode = AuditLogConfig.get().getString("auditlog.store.authentication.mode");
			String user = AuditLogConfig.get().getString("auditlog.store.authentication.user");
			String password = AuditLogConfig.get().getString("auditlog.store.authentication.password");
			switch (authMode.toLowerCase()) {
			case "basic": {
				return new AuthenticationCredential(ESearchActivator.Authentication.BASIC, user, password);
			}
			case "apikey":
				return new AuthenticationCredential(ESearchActivator.Authentication.API_KEY, user, password);
			default:
				return new AuthenticationCredential(ESearchActivator.Authentication.NONE, null, null);
			}
		}
		return new AuthenticationCredential(ESearchActivator.Authentication.NONE, null, null);
	}

	public record AuthConfig(Authentication auth, String user, String password) {

	}

	public record ExternalESConfig(String ip, int port) {

	}
}
