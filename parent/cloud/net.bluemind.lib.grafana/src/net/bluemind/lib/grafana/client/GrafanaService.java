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
package net.bluemind.lib.grafana.client;

public class GrafanaService {

	private static final String DASHDB = "/api/dashboards/db";
	private static final String DASH = "/api/dashboards";
	private static final String DATA = "/api/datasources";
	private static final String SERVICE = "/api/serviceaccounts";

	private GrafanaService() {
	}

	public static String dashboardsDbPath() {
		return DASHDB;
	}

	public static String dashboardsPath(String uid) {
		return DASH + "/uid/" + uid;
	}

	public static String datasourcesPath() {
		return DATA;
	}

	public static String datasourcesPath(String name) {
		return datasourcesPath() + "/name/" + name;
	}

	public static String serviceAccountsPath() {
		return SERVICE;
	}

	public static String serviceAccountsTokenPath(Integer accountId) {
		return serviceAccountsPath() + "/" + accountId + "/tokens";
	}

	public static String serviceAccountsPath(Integer accountId) {
		return serviceAccountsPath() + "/" + accountId;
	}
}
