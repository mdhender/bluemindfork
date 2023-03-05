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
package net.bluemind.keycloak.internal;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.keycloak.api.KerberosComponent.CachePolicy;
import net.bluemind.role.api.BasicRoles;

public class KeycloakKerberosAdminService extends ComponentService implements IKeycloakKerberosAdmin {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakKerberosAdminService.class);

	public KeycloakKerberosAdminService(BmContext context, String domainId) {
		super(context, domainId);
	}

	@Override
	public void create(KerberosComponent component) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		createComponent(component);

	}

	@Override
	public List<KerberosComponent> allKerberosProviders() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get all Kerberos providers", domainId);
		
		List<KerberosComponent> ret = new ArrayList<>();
		allComponents(ComponentProvider.KERBEROS).forEach(cmp -> ret.add(jsonToKerberosComponent(cmp)));
		return ret;
	}

	@Override
	public KerberosComponent getKerberosProvider(String componentName) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get Kerberos provider {}", domainId, componentName);
		
		return jsonToKerberosComponent(getComponent(ComponentProvider.KERBEROS, componentName));
	}

	@Override
	public void deleteKerberosProvider(String componentName) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Delete kerberos provider {}", domainId, componentName);
		
		deleteComponent(ComponentProvider.KERBEROS, componentName);
	}

	KerberosComponent jsonToKerberosComponent(JsonObject ret) {
		if (ret == null) {
			return null;
		}
		
		KerberosComponent kc = new KerberosComponent();
		kc.setId(ret.getString("id"));
		kc.setParentId(ret.getString("parentId"));
		kc.setName(ret.getString("name"));
		
		if (ret.getJsonObject("config").getJsonArray("kerberosRealm") != null) {
			kc.setKerberosRealm(ret.getJsonObject("config").getJsonArray("kerberosRealm").getString(0));
		}
		if (ret.getJsonObject("config").getJsonArray("serverPrincipal") != null) {
			kc.setServerPrincipal(ret.getJsonObject("config").getJsonArray("serverPrincipal").getString(0));
		}
		if (ret.getJsonObject("config").getJsonArray("keyTab") != null) {
			kc.setKeyTab(ret.getJsonObject("config").getJsonArray("keyTab").getString(0));
		}


		if (ret.getJsonObject("config").getJsonArray("enabled") != null) {
			kc.setEnabled(Boolean.valueOf(ret.getJsonObject("config").getJsonArray("enabled").getString(0)));
		}
		if (ret.getJsonObject("config").getJsonArray("debug") != null) {
			kc.setDebug(Boolean.valueOf(ret.getJsonObject("config").getJsonArray("debug").getString(0)));
		}
		if (ret.getJsonObject("config").getJsonArray("allowPasswordAuthentication") != null) {
			kc.setAllowPasswordAuthentication(Boolean.valueOf(ret.getJsonObject("config").getJsonArray("allowPasswordAuthentication").getString(0)));
		}
		if (ret.getJsonObject("config").getJsonArray("updateProfileFirstLogin") != null) {
			kc.setUpdateProfileFirstLogin(Boolean.valueOf(ret.getJsonObject("config").getJsonArray("updateProfileFirstLogin").getString(0)));
		}
		
		if (ret.getJsonObject("config").getJsonArray("cachePolicy") != null) {
			kc.setCachePolicy(CachePolicy.valueOf(ret.getJsonObject("config").getJsonArray("cachePolicy").getString(0)));
		}
		
		return kc;
	}
}
