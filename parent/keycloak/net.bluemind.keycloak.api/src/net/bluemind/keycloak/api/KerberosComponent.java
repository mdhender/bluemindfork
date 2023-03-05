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
public class KerberosComponent extends Component {
	private String providerId;
	private String providerType;
	private String id;
	private String parentId;
	private String name;

	private String kerberosRealm;
	private String serverPrincipal;
	private String keyTab;
	private Boolean enabled = Boolean.FALSE;
	private Boolean debug = Boolean.FALSE;
	private Boolean allowPasswordAuthentication = Boolean.FALSE;
	private Boolean updateProfileFirstLogin = Boolean.FALSE;
	private CachePolicy cachePolicy = CachePolicy.DEFAULT;

	@BMApi(version = "3")
	public enum CachePolicy {
		DEFAULT, EVICT_DAILY, EVICT_WEEKLY, MAX_LIFESPAN, NO_CACHE;
	}

	public KerberosComponent() {
		providerId = "kerberos";
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

	public String getKerberosRealm() {
		return kerberosRealm;
	}

	public void setKerberosRealm(String kerberosRealm) {
		this.kerberosRealm = kerberosRealm;
	}

	public String getServerPrincipal() {
		return serverPrincipal;
	}

	public void setServerPrincipal(String serverPrincipal) {
		this.serverPrincipal = serverPrincipal;
	}

	public String getKeyTab() {
		return keyTab;
	}

	public void setKeyTab(String keyTab) {
		this.keyTab = keyTab;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean isDebug() {
		return debug;
	}

	public void setDebug(Boolean debug) {
		this.debug = debug;
	}

	public Boolean isAllowPasswordAuthentication() {
		return allowPasswordAuthentication;
	}

	public void setAllowPasswordAuthentication(Boolean allowPasswordAuthentication) {
		this.allowPasswordAuthentication = allowPasswordAuthentication;
	}

	public Boolean isUpdateProfileFirstLogin() {
		return updateProfileFirstLogin;
	}

	public void setUpdateProfileFirstLogin(Boolean updateProfileFirstLogin) {
		this.updateProfileFirstLogin = updateProfileFirstLogin;
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
		config.put("kerberosRealm", Arrays.asList(kerberosRealm));
		config.put("serverPrincipal", Arrays.asList(serverPrincipal));
		config.put("keyTab", Arrays.asList(keyTab));
		config.put("enabled", Arrays.asList(enabled.toString()));
		config.put("debug", Arrays.asList(debug.toString()));
		config.put("allowPasswordAuthentication", Arrays.asList(allowPasswordAuthentication.toString()));
		config.put("updateProfileFirstLogin", Arrays.asList(updateProfileFirstLogin.toString()));
		config.put("cachePolicy", Arrays.asList(cachePolicy.name()));

		component.put("config", config);

		return component;
	}

	@Override
	public String toString() {
		return "KerberosComponent [kerberosRealm=" + kerberosRealm + ", serverPrincipal=" + serverPrincipal
				+ ", keyTab=" + keyTab + ", enabled=" + enabled + ", debug=" + debug + ", allowPasswordAuthentication="
				+ allowPasswordAuthentication + ", updateProfileFirstLogin=" + updateProfileFirstLogin
				+ ", cachePolicy=" + cachePolicy + ", providerId=" + providerId + ", providerType=" + providerType
				+ ", parentId=" + parentId + ", name=" + name + "]";
	}

}
