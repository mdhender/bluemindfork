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
	private String id;
	private String parentId;
	private String name;
	
	private String bmDomain;
	private String bmUrl;
	private String bmCoreToken;
	private Boolean enabled = Boolean.TRUE;
	private CachePolicy cachePolicy = CachePolicy.DEFAULT;

	@BMApi(version = "3")
	public enum CachePolicy {
		DEFAULT, EVICT_DAILY, EVICT_WEEKLY, MAX_LIFESPAN, NO_CACHE;
	}
	
	public BluemindProviderComponent() {
		providerId = "Bluemind";
		providerType = "org.keycloak.storage.UserStorageProvider";
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
	
	public String getBmCoreToken() {
		return bmCoreToken;
	}

	public void setBmCoreToken(String bmCoreToken) {
		this.bmCoreToken = bmCoreToken;
	}
	
	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public CachePolicy getCachePolicy() {
		return cachePolicy;
	}

	public void setCachePolicy(CachePolicy cachePolicy) {
		this.cachePolicy = cachePolicy;
	}

	@Override
	public JsonObject toJson() {
		JsonObject component = new JsonObject();
		component.put("id", id);
		component.put("providerId", providerId);
		component.put("providerType", providerType);
		component.put("parentId", parentId);
		component.put("name", name);

		JsonObject config = new JsonObject();
		if (bmDomain != null) config.put("bmDomain", Arrays.asList(bmDomain));
		if (bmUrl != null) config.put("bmUrl", Arrays.asList(bmUrl));
		if (bmCoreToken != null) config.put("bmCoreToken", Arrays.asList(bmCoreToken));
		config.put("enabled", Arrays.asList(enabled.toString()));
		if (cachePolicy != null) config.put("cachePolicy", Arrays.asList(cachePolicy.name()));

		component.put("config", config);

		return component;
	}
	
	@Override
	public String toString() {
		return "BluemindProviderComponent [id=" + id + ", parentId=" + parentId + ", name=" + getName()
				+ ", enabled=" + enabled + ", cachePolicy=" + cachePolicy
				+ ", bmDomain=" + bmDomain + ", bmUrl=" + bmUrl 
				+ ", bmCoreToken=" + (bmCoreToken == null ? "--empty--" : "--secret--") + "]";
	}

}
