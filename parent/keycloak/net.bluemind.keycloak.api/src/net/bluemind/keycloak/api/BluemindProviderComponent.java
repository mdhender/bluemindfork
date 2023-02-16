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

import java.util.Arrays;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class BluemindProviderComponent extends Component {
	private String providerId;
	private String providerType;
	private String parentId;
	private String name;
	
	private String bmDomain;
	private String bmUrl;
	private String bmCoreToken;
	
	
	public BluemindProviderComponent() {
		providerId = "Bluemind";
		providerType = "org.keycloak.storage.UserStorageProvider";
	}
	
	
	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBmDomain() {
		return bmDomain;
	}

	public void setBmDomain(String bmDomain) {
		this.bmDomain = bmDomain;
	}

	public String getBmUrl() {
		return bmUrl;
	}

	public void setBmUrl(String bmUrl) {
		this.bmUrl = bmUrl;
	}

	public String getProviderId() {
		return providerId;
	}

	public String getProviderType() {
		return providerType;
	}
	
	public String getBmCoreToken() {
		return bmCoreToken;
	}

	public void setBmCoreToken(String bmCoreToken) {
		this.bmCoreToken = bmCoreToken;
	}

	@Override
	public JsonObject toJson() {
		JsonObject component = new JsonObject();
		component.put("providerId", providerId);
		component.put("providerType", providerType);
		component.put("parentId", parentId);
		component.put("name", name);

		JsonObject config = new JsonObject();
		if (bmDomain != null) config.put("bmDomain", Arrays.asList(bmDomain));
		config.put("bmUrl", Arrays.asList(bmUrl));
		config.put("bmCoreToken", Arrays.asList(bmCoreToken));

		component.put("config", config);

		return component;
	}
	
	@Override
	public String toString() {
		return "BluemindProviderComponent [parentId=" + parentId + ", name=" + name
				+ ", bmDomain=" + bmDomain + ", bmUrl=" + bmUrl 
				+ ", bmCoreToken=" + (bmCoreToken == null ? "--empty--" : "--secret--") + "]";
	}

}
