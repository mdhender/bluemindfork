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
package net.bluemind.keycloak.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.api.IInCoreDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakFlowAdmin;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.utils.SyncHttpClient;

public class KeycloakHelper {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakHelper.class);
	private static final int KEYCLOAK_WAIT_MAX_RETRIES = 8; // 5sec per retry => 40sec max wait
	private static final String GLOBAL_VIRT = "global.virt";
	private static final String NO_REDIRECT_URI = "https://configure_external_url_in_bluemind";

	private KeycloakHelper() {

	}

	public static void initForDomain(ItemValue<Domain> domain) {
		waitForKeycloak();
		if (domain.value.properties != null
				&& AuthTypes.OPENID.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			initExternalForDomain(domain);
		} else {
			initKeycloakForDomain(domain);
		}
	}

	private static void initKeycloakForDomain(ItemValue<Domain> domain) {
		logger.info("Init Keycloak realm for domain {}", domain.uid);

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IKeycloakAdmin keycloakAdminService = provider.instance(IKeycloakAdmin.class);

		String realm = domain.uid;
		String clientId = IKeycloakUids.clientId(domain.uid);

		keycloakAdminService.createRealm(realm);

		IKeycloakFlowAdmin keycloakFlowService = provider.instance(IKeycloakFlowAdmin.class, realm);
		keycloakFlowService.createByCopying(IKeycloakUids.KEYCLOAK_FLOW_ALIAS, IKeycloakUids.BLUEMIND_FLOW_ALIAS);

		IKeycloakBluemindProviderAdmin keycloakBluemindProviderService = provider
				.instance(IKeycloakBluemindProviderAdmin.class, domain.uid);
		BluemindProviderComponent bpComponent = new BluemindProviderComponent();
		bpComponent.setParentId(realm);
		bpComponent.setName(IKeycloakUids.bmProviderId(realm));

		SharedMap<String, String> smap = MQ.sharedMap(Shared.MAP_SYSCONF);
		bpComponent.setBmUrl(
				smap.get(SysConfKeys.external_protocol.name()) + "://" + smap.get(SysConfKeys.external_url.name()));

		bpComponent.setBmCoreToken(Token.admin0());
		keycloakBluemindProviderService.create(bpComponent);

		String authType = domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name());
		if (Strings.isNullOrEmpty(authType)) {
			authType = AuthTypes.INTERNAL.name();
			domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), authType);
		}

		IKeycloakClientAdmin keycloakRealmAdminService = provider.instance(IKeycloakClientAdmin.class, domain.uid);
		keycloakRealmAdminService.create(clientId);
		String secret = keycloakRealmAdminService.getSecret(clientId);
		String opendIdHost = IKeycloakUids
				.defaultHost(Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address(), domain.uid);

		JsonObject conf;
		try {
			conf = getOpenIdConfiguration(opendIdHost);
		} catch (Exception e) {
			throw new ServerFault("Failed to fetch OpenId configuration " + e.getMessage());
		}
		Map<String, String> properties = domain.value.properties != null ? domain.value.properties : new HashMap<>();

		properties.put(AuthDomainProperties.OPENID_REALM.name(), realm);
		properties.put(AuthDomainProperties.OPENID_CLIENT_ID.name(), clientId);
		properties.put(AuthDomainProperties.OPENID_CLIENT_SECRET.name(), secret);
		properties.put(AuthDomainProperties.OPENID_HOST.name(), opendIdHost);
		properties.put(AuthDomainProperties.OPENID_AUTHORISATION_ENDPOINT.name(),
				conf.getString("authorization_endpoint"));
		properties.put(AuthDomainProperties.OPENID_TOKEN_ENDPOINT.name(), conf.getString("token_endpoint"));
		properties.put(AuthDomainProperties.OPENID_JWKS_URI.name(), conf.getString("jwks_uri"));
		String accessTokenIssuer = Optional.ofNullable(conf.getString("access_token_issuer"))
				.orElse(conf.getString("issuer"));
		properties.put(AuthDomainProperties.OPENID_ISSUER.name(), accessTokenIssuer);
		properties.put(AuthDomainProperties.OPENID_END_SESSION_ENDPOINT.name(), conf.getString("end_session_endpoint"));

		provider.instance(IInCoreDomains.class).setProperties(domain.uid, properties);

		if (AuthTypes.KERBEROS.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			KerberosConfigHelper.createKeycloakKerberosConf(domain);
			KerberosConfigHelper.updateKrb5Conf();
		}

	}

	private static void initExternalForDomain(ItemValue<Domain> domain) {
		logger.info("Init external authentication config for domain {}", domain.uid);

		String opendIdHost = domain.value.properties.get(AuthDomainProperties.OPENID_HOST.name());

		JsonObject conf;
		try {
			conf = getOpenIdConfiguration(opendIdHost);
		} catch (Exception e) {
			throw new ServerFault("Failed to fetch OpenId configuration " + e.getMessage());
		}

		boolean somethingChanged = false;

		String key = AuthDomainProperties.OPENID_AUTHORISATION_ENDPOINT.name();
		String val = conf.getString("authorization_endpoint");
		somethingChanged = hasValueChanged(domain, somethingChanged, key, val);

		key = AuthDomainProperties.OPENID_TOKEN_ENDPOINT.name();
		val = conf.getString("token_endpoint");
		somethingChanged = hasValueChanged(domain, somethingChanged, key, val);

		key = AuthDomainProperties.OPENID_JWKS_URI.name();
		val = conf.getString("jwks_uri");
		somethingChanged = hasValueChanged(domain, somethingChanged, key, val);

		key = AuthDomainProperties.OPENID_ISSUER.name();
		val = Optional.ofNullable(conf.getString("access_token_issuer")).orElse(conf.getString("issuer"));
		somethingChanged = hasValueChanged(domain, somethingChanged, key, val);

		key = AuthDomainProperties.OPENID_END_SESSION_ENDPOINT.name();
		val = conf.getString("end_session_endpoint");
		somethingChanged = hasValueChanged(domain, somethingChanged, key, val);

		if (somethingChanged) {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreDomains.class)
					.setProperties(domain.uid, domain.value.properties);
		}

	}

	private static JsonObject getOpenIdConfiguration(String openIdHost) throws Exception {
		String configuration = SyncHttpClient.get(openIdHost);
		return new JsonObject(configuration);
	}

	private static boolean hasValueChanged(ItemValue<Domain> domain, boolean somethingChanged, String key, String val) {
		if (val == null && domain.value.properties.get(key) != null) {
			domain.value.properties.remove(key);
			somethingChanged = true;
		} else if (val != null && !val.equals(domain.value.properties.get(key))) {
			domain.value.properties.put(key, val);
			somethingChanged = true;
		}
		return somethingChanged;
	}

	public static void onDomainUpdate(String domainUid) {
		ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(domainUid);
		if (domain.value.properties != null
				&& AuthTypes.OPENID.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			initExternalForDomain(domain);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IKeycloakAdmin.class)
					.deleteRealm(domainUid);
			KerberosConfigHelper.updateGlobalRealmKerb();
			KerberosConfigHelper.updateKrb5Conf();
		} else {
			updateKeycloakForDomain(domain);
		}
	}

	private static void updateKeycloakForDomain(ItemValue<Domain> domain) {

		String domainUid = domain.uid;

		logger.info("Update keycloak config for domain {}", domainUid);
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		String clientId = IKeycloakUids.clientId(domainUid);
		IKeycloakClientAdmin kcCientService = provider.instance(IKeycloakClientAdmin.class, domainUid);

		OidcClient oc = kcCientService.getOidcClient(clientId);
		if (oc != null && (domain.value.properties == null
				|| !oc.secret.equals(domain.value.properties.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name())))) {
			oc = null;
		}

		if (oc == null) {
			IKeycloakKerberosAdmin krbProv = provider.instance(IKeycloakKerberosAdmin.class, GLOBAL_VIRT);
			KerberosComponent krbComp = null;
			if (GLOBAL_VIRT.equals(domainUid)) {
				krbComp = krbProv.getKerberosProvider(KerberosConfigHelper.KRB_GLOBAL_VIRT_NAME);
			}

			provider.instance(IKeycloakAdmin.class).deleteRealm(domain.uid);
			initKeycloakForDomain(domain);
			oc = kcCientService.getOidcClient(clientId);
			if (krbComp != null) {
				krbProv.create(krbComp);
			}
		}

		List<String> currentUrls = getDomainUrls(domainUid);
		if (!oc.redirectUris.containsAll(currentUrls) || !currentUrls.containsAll(oc.redirectUris)) {
			oc.redirectUris = currentUrls;
			kcCientService.updateClient(clientId, oc);
			logger.info("Domain {} update : Urls changed : updated oidc client", domainUid);
		} else {
			logger.debug("Domain {} update : Urls did not change (no need to update oidc client)", domainUid);
		}

		KerberosConfigHelper.updateKeycloakKerberosConf(domain);
	}

	public static List<String> getDomainUrls(String domainId) {
		List<String> res = new ArrayList<>();

		SystemConf sysconf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class).getValues();
		if (GLOBAL_VIRT.equals(domainId)) {
			if (sysconf.stringValue(SysConfKeys.external_url.name()) != null) {
				res.add(getOpenIdUrl(sysconf.stringValue(SysConfKeys.external_url.name())));
			}

			String otherUrls = sysconf.stringValue(SysConfKeys.other_urls.name());
			addOtherUrls(res, otherUrls);
			if (res.isEmpty()) {
				res.add(NO_REDIRECT_URI);
			}
			return res;
		}

		Map<String, String> domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainId).get();
		if (domainSettings != null) {
			if (domainSettings.get(DomainSettingsKeys.external_url.name()) != null) {
				res.add(getOpenIdUrl(domainSettings.get(DomainSettingsKeys.external_url.name())));
			}

			String otherUrls = domainSettings.get(DomainSettingsKeys.other_urls.name());
			addOtherUrls(res, otherUrls);
			if (res.isEmpty()) {
				res.add(NO_REDIRECT_URI);
			}
			return res;
		}

		res.add(NO_REDIRECT_URI);
		return res;
	}

	private static void addOtherUrls(List<String> res, String otherUrls) {
		if (otherUrls != null) {
			StringTokenizer tokenizer = new StringTokenizer(otherUrls.trim(), " ");
			while (tokenizer.hasMoreElements()) {
				res.add(getOpenIdUrl(tokenizer.nextToken()));
			}
		}
	}

	private static String getOpenIdUrl(String url) {
		return "https://" + url + "/auth/openid";

	}

	public static void initForDomain(String domainId) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		ItemValue<Domain> domain = provider.instance(IDomains.class).get(domainId);
		if (domain == null || domain.value == null) {
			throw ServerFault.notFound("Domain " + domainId + " not found");
		}

		initForDomain(domain);
	}

	public static void waitForKeycloak() {
		IKeycloakAdmin keycloakAdminService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IKeycloakAdmin.class);

		int nbRetries = 0;
		while (nbRetries < KEYCLOAK_WAIT_MAX_RETRIES) {
			try {
				keycloakAdminService.allRealms();
				return;
			} catch (Exception e) {
				// keycloak is not available yet
			}

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			nbRetries++;
		}
		throw new ServerFault("Wait for keycloak timed out (keycloak still not responding)");
	}

}
