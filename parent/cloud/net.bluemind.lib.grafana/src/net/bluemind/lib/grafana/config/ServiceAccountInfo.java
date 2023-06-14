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
package net.bluemind.lib.grafana.config;

import io.vertx.core.json.JsonObject;

public class ServiceAccountInfo {

	private ServiceAccount account;
	private ServiceAccountToken accountToken;

	public static class ServiceAccount {
		Integer id;
		String name;
		Boolean isDisabled;

		public static ServiceAccount create(Integer id, String name, Boolean isDisabled) {
			ServiceAccount sa = new ServiceAccount();
			sa.id = id;
			sa.name = name;
			sa.isDisabled = isDisabled;
			return sa;
		}

		public static ServiceAccount createFromJson(String json) {
			JsonObject jsonObject = new JsonObject(json);
			ServiceAccount sa = new ServiceAccount();
			sa.id = jsonObject.getInteger("id");
			sa.name = jsonObject.getString("name");
			sa.isDisabled = jsonObject.getBoolean("isDisabled");
			return sa;
		}
	}

	public static class ServiceAccountToken {
		Integer id;
		String name;
		String key;

		public static ServiceAccountToken create(Integer id, String name, String key) {
			ServiceAccountToken sa = new ServiceAccountToken();
			sa.id = id;
			sa.name = name;
			sa.key = key;
			return sa;
		}

		public static ServiceAccountToken createFromJson(String json) {
			JsonObject jsonObject = new JsonObject(json);
			ServiceAccountToken sat = new ServiceAccountToken();
			sat.id = jsonObject.getInteger("id");
			sat.name = jsonObject.getString("name");
			sat.key = jsonObject.getString("key");
			return sat;
		}
	}

	public Integer tokenId() {
		return this.accountToken.id;
	}

	public String tokenKey() {
		return this.accountToken.key;
	}

	public Integer accountId() {
		return this.account.id;
	}

	public String accountName() {
		return this.account.name;
	}

	public void setServiceAccount(String body) {
		this.account = ServiceAccount.createFromJson(body);
	}

	public void setServiceAccountToken(String body) {
		this.accountToken = ServiceAccountToken.createFromJson(body);
	}

	public boolean accountInit() {
		return this.account != null;
	}

	public boolean accountTokenInit() {
		return this.accountToken != null;
	}
}
