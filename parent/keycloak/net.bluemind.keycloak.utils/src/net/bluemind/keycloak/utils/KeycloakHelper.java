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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
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
import net.bluemind.keycloak.api.KerberosComponent.CachePolicy;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.SysConfKeys;

public class KeycloakHelper {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakHelper.class);
	private static final int keycloakWaitMaxRetries = 8; // 5sec per retry => 40sec max wait

	private KeycloakHelper() {

	}

	public static void initForDomain(ItemValue<Domain> domain) {
		if (domain.value.properties != null
				&& AuthTypes.OPENID.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			initExternalForDomain(domain);
		} else {
			Optional<ItemValue<Server>> kcServer = Topology.get().anyIfPresent(TagDescriptor.bm_keycloak.getTag());
			if (kcServer.isPresent()) {
				initKeycloakForDomain(domain);
			} else {
				logger.warn("No keycloak server in topology. Skipping init for domain {}", domain.uid);
			}
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
		keycloakFlowService.createByCopying(IKeycloakUids.keycloakFlowAlias, IKeycloakUids.bluemindFlowAlias);

		IKeycloakBluemindProviderAdmin keycloakBluemindProviderService = provider
				.instance(IKeycloakBluemindProviderAdmin.class, domain.uid);
		BluemindProviderComponent bpComponent = new BluemindProviderComponent();
		bpComponent.setParentId(realm);
		bpComponent.setName(realm + "-bmprovider");

		SharedMap<String, String> smap = MQ.sharedMap(Shared.MAP_SYSCONF);
		bpComponent.setBmUrl(
				smap.get(SysConfKeys.external_protocol.name()) + "://" + smap.get(SysConfKeys.external_url.name()));

		bpComponent.setBmCoreToken(Token.admin0());
		keycloakBluemindProviderService.create(bpComponent);

		String authType = AuthTypes.INTERNAL.name();
		String krbAdDomain = null;
		String krbAdIp = null;
		String krbKeytab = null;
		String casUrl = null;
		if (domain.value.properties != null
				&& domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()) != null) {
			authType = domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name());
			if (AuthTypes.KERBEROS.name().equals(authType)) {
				krbAdDomain = domain.value.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name());
				krbAdIp = domain.value.properties.get(AuthDomainProperties.KRB_AD_IP.name());
				krbKeytab = domain.value.properties.get(AuthDomainProperties.KRB_KEYTAB.name());
			}
			if (AuthTypes.CAS.name().equals(authType)) {
				casUrl = domain.value.properties.get(AuthDomainProperties.CAS_URL.name());
			}
		}

		String auth_type = authType;
		String krb_ad_domain = krbAdDomain != null ? krbAdDomain.toUpperCase() : null;
		String krb_ad_ip = krbAdIp;
		String krb_keytab = krbKeytab;
		String cas_url = casUrl;
		if (AuthTypes.KERBEROS.name().equals(auth_type)) {
			Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
					.get(domain.uid);
			String domainExternalUrl = domainSettings.get(DomainSettingsKeys.external_url.name());
			String globalExternalUrl = smap.get(SysConfKeys.external_url.name());
			String srvPrincHost = domainExternalUrl != null ? domainExternalUrl : globalExternalUrl;

			String serverPrincipal = "HTTP/" + srvPrincHost + "@" + krb_ad_domain;

			String keytabPath = "/etc/bm-keycloak/" + domain.uid + ".keytab";
			String kcServerAddr = Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address();
			INodeClient nodeClient = NodeActivator.get(kcServerAddr);
			nodeClient.writeFile(keytabPath, new ByteArrayInputStream(Base64.getDecoder().decode(krb_keytab)));

			KerberosComponent kerb = new KerberosComponent();
			kerb.setKerberosRealm(krb_ad_domain);
			kerb.setServerPrincipal(serverPrincipal);
			kerb.setKeyTab(keytabPath);
			kerb.setEnabled(true);
			kerb.setDebug(true);
			kerb.setCachePolicy(CachePolicy.DEFAULT);

			if (!"global.virt".equals(realm) && domainExternalUrl == null) {
				IKeycloakKerberosAdmin kerbProv = provider.instance(IKeycloakKerberosAdmin.class, "global.virt");
				try {
					kerbProv.deleteKerberosProvider("global.virt-kerberos");
				} catch (Throwable t) {
				}
				kerb.setName("global.virt-kerberos");
				kerb.setParentId("global.virt");
				kerbProv.create(kerb);
			} else {
				kerb.setName(realm + "-kerberos");
				kerb.setParentId(realm);
				provider.instance(IKeycloakKerberosAdmin.class, realm).create(kerb);
			}
		}
		KerberosConfigHelper.updateKrb5Conf();

		IKeycloakClientAdmin keycloakRealmAdminService = provider.instance(IKeycloakClientAdmin.class, domain.uid);
		keycloakRealmAdminService.create(clientId);
		String secret = keycloakRealmAdminService.getSecret(clientId);
		String opendIdHost = IKeycloakUids
				.defaultHost(Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address(), domain.uid);

		URI uri;
		try {
			uri = new URI(opendIdHost);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
			return;
		}

		HttpClient client = initHttpClient(uri);
		client.request(HttpMethod.GET, uri.getPath())
				.onSuccess(req -> req.send().onSuccess(res -> res.bodyHandler(body -> {
					JsonObject conf = new JsonObject(new String(body.getBytes()));

					Map<String, String> properties = domain.value.properties != null ? domain.value.properties
							: new HashMap<>();

					properties.put(AuthDomainProperties.OPENID_REALM.name(), realm);
					properties.put(AuthDomainProperties.OPENID_CLIENT_ID.name(), clientId);
					properties.put(AuthDomainProperties.OPENID_CLIENT_SECRET.name(), secret);
					properties.put(AuthDomainProperties.OPENID_HOST.name(), opendIdHost);
					properties.put(AuthDomainProperties.OPENID_AUTHORISATION_ENDPOINT.name(),
							conf.getString("authorization_endpoint"));
					properties.put(AuthDomainProperties.OPENID_TOKEN_ENDPOINT.name(), conf.getString("token_endpoint"));
					properties.put(AuthDomainProperties.OPENID_JWKS_URI.name(), conf.getString("jwks_uri"));
					String accessTokenIssuer = Optional.ofNullable(conf.getString("issuer"))
							.orElse(conf.getString("access_token_issuer"));
					properties.put(AuthDomainProperties.OPENID_ISSUER.name(), accessTokenIssuer);
					properties.put(AuthDomainProperties.OPENID_END_SESSION_ENDPOINT.name(),
							conf.getString("end_session_endpoint"));

					properties.put(AuthDomainProperties.AUTH_TYPE.name(), auth_type);
					if (AuthTypes.KERBEROS.name().equals(auth_type)) {
						properties.put(AuthDomainProperties.KRB_AD_DOMAIN.name(), krb_ad_domain);
						properties.put(AuthDomainProperties.KRB_AD_IP.name(), krb_ad_ip);
						properties.put(AuthDomainProperties.KRB_KEYTAB.name(), krb_keytab);
					}
					if (AuthTypes.CAS.name().equals(auth_type)) {
						properties.put(AuthDomainProperties.CAS_URL.name(), cas_url);
					}

					provider.instance(IInCoreDomains.class).setProperties(domain.uid, properties);

				}))).onFailure(t -> logger.error(t.getMessage(), t));

	}

	private static void initExternalForDomain(ItemValue<Domain> domain) {
		logger.info("Init external authentication config for domain {}", domain.uid);

		String opendIdHost = domain.value.properties.get(AuthDomainProperties.OPENID_HOST.name());

		URI uri;
		try {
			uri = new URI(opendIdHost);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
			return;
		}

		HttpClient client = initHttpClient(uri);
		client.request(HttpMethod.GET, uri.getPath())
				.onSuccess(req -> req.send().onSuccess(res -> res.bodyHandler(body -> {
					JsonObject conf = new JsonObject(new String(body.getBytes()));

					boolean somethingChanged = false;

					String key = AuthDomainProperties.OPENID_AUTHORISATION_ENDPOINT.name();
					String val = conf.getString("authorization_endpoint");
					if (val == null && domain.value.properties.get(key) != null) {
						domain.value.properties.remove(key);
						somethingChanged = true;
					} else if (val != null && !val.equals(domain.value.properties.get(key))) {
						domain.value.properties.put(key, val);
						somethingChanged = true;
					}

					key = AuthDomainProperties.OPENID_TOKEN_ENDPOINT.name();
					val = conf.getString("token_endpoint");
					if (val == null && domain.value.properties.get(key) != null) {
						domain.value.properties.remove(key);
						somethingChanged = true;
					} else if (val != null && !val.equals(domain.value.properties.get(key))) {
						domain.value.properties.put(key, val);
						somethingChanged = true;
					}

					key = AuthDomainProperties.OPENID_JWKS_URI.name();
					val = conf.getString("jwks_uri");
					if (val == null && domain.value.properties.get(key) != null) {
						domain.value.properties.remove(key);
						somethingChanged = true;
					} else if (val != null && !val.equals(domain.value.properties.get(key))) {
						domain.value.properties.put(key, val);
						somethingChanged = true;
					}

					key = AuthDomainProperties.OPENID_ISSUER.name();
					val = Optional.ofNullable(conf.getString("issuer")).orElse(conf.getString("access_token_issuer"));
					if (val == null && domain.value.properties.get(key) != null) {
						domain.value.properties.remove(key);
						somethingChanged = true;
					} else if (val != null && !val.equals(domain.value.properties.get(key))) {
						domain.value.properties.put(key, val);
						somethingChanged = true;
					}

					key = AuthDomainProperties.OPENID_END_SESSION_ENDPOINT.name();
					val = conf.getString("end_session_endpoint");
					if (val == null && domain.value.properties.get(key) != null) {
						domain.value.properties.remove(key);
						somethingChanged = true;
					} else if (val != null && !val.equals(domain.value.properties.get(key))) {
						domain.value.properties.put(key, val);
						somethingChanged = true;
					}

					if (somethingChanged) {
						ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreDomains.class)
								.setProperties(domain.uid, domain.value.properties);
					}
				}))).onFailure(t -> logger.error(t.getMessage(), t));

	}

	public static void updateForDomain(String domainUid) {
		ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(domainUid);
		Optional<ItemValue<Server>> kcServer = Topology.get().anyIfPresent(TagDescriptor.bm_keycloak.getTag());
		if (domain.value.properties != null
				&& AuthTypes.OPENID.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			initExternalForDomain(domain);
			if (kcServer.isPresent()) {
				try {
					ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IKeycloakAdmin.class)
							.deleteRealm(domainUid);
				} catch (Throwable t) {
				}
				KerberosConfigHelper.updateGlobalRealmKerb();
				KerberosConfigHelper.updateKrb5Conf();
			}
		} else if (kcServer.isPresent()) {
			updateKeycloakForDomain(domainUid);
		} else {
			logger.warn("No keycloak server in topology. Skipping update for domain {}", domain.uid);
		}
	}

	private static void updateKeycloakForDomain(String domainUid) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ItemValue<Domain> domain = provider.instance(IDomains.class).get(domainUid);

		String clientId = IKeycloakUids.clientId(domainUid);
		IKeycloakClientAdmin kcCientService = provider.instance(IKeycloakClientAdmin.class, domainUid);

		OidcClient oc = null;
		try {
			oc = kcCientService.getOidcClient(clientId);
			if (oc != null && (domain.value.properties == null || !oc.secret
					.equals(domain.value.properties.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name())))) {
				oc = null;
			}
		} catch (Throwable t) {
		}
		if (oc == null) {
			try {
				provider.instance(IKeycloakAdmin.class).deleteRealm(domainUid);
			} catch (Throwable t) {
			}
			initKeycloakForDomain(domain);
			oc = kcCientService.getOidcClient(clientId);
		}

		List<String> currentUrls = getDomainUrls(domainUid);
		if (!oc.redirectUris.containsAll(currentUrls) || !currentUrls.containsAll(oc.redirectUris)) {
			oc.redirectUris = currentUrls;
			kcCientService.updateClient(clientId, oc);
			logger.info("Domain {} update : Urls changed : updated oidc client", domainUid);
		} else {
			logger.info("Domain {} update : Urls did not change (no need to update oidc client)", domainUid);
		}

		KerberosConfigHelper.updateKeycloakKerberosConf(domainUid);
	}

	public static void upgradeDomain(ItemValue<Domain> domain) {
		if (domain.value.properties == null
				|| domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()) == null) {
			Map<String, String> properties = domain.value.properties != null ? domain.value.properties
					: new HashMap<>();
			SharedMap<String, String> smap = MQ.sharedMap(Shared.MAP_SYSCONF);

			String authType = smap.get("auth_type");
			if (AuthTypes.KERBEROS.name().equals(authType) && domain.uid.equals(smap.get("krb_domain"))) {
				properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.KERBEROS.name());
				properties.put(AuthDomainProperties.KRB_AD_DOMAIN.name(), smap.get("krb_ad_domain"));
				properties.put(AuthDomainProperties.KRB_AD_IP.name(), smap.get("krb_ad_ip"));
				properties.put(AuthDomainProperties.KRB_KEYTAB.name(), smap.get("krb_keytab"));
			} else if (AuthTypes.CAS.name().equals(authType) && domain.uid.equals(smap.get("cas_domain"))) {
				properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.CAS.name());
				properties.put(AuthDomainProperties.CAS_URL.name(), smap.get("cas_url"));
			} else if (AuthTypes.OPENID.name().equals(authType)) {
				properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.OPENID.name());
			} else {
				properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.INTERNAL.name());
			}
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreDomains.class)
					.setProperties(domain.uid, properties);
		}

		initForDomain(domain);
	}

	public static List<String> getDomainUrls(String domainId) {
		ArrayList<String> res = new ArrayList<String>();
		SharedMap<String, String> sysconf = MQ.sharedMap(Shared.MAP_SYSCONF);
		if ("global.virt".equals(domainId)) {
			if (sysconf.get(SysConfKeys.external_url.name()) != null) {
				res.add("https://" + sysconf.get(SysConfKeys.external_url.name()) + "/auth/openid");
			}

			String otherUrls = sysconf.get(SysConfKeys.other_urls.name());
			if (otherUrls != null) {
				StringTokenizer tokenizer = new StringTokenizer(otherUrls.trim(), " ");
				while (tokenizer.hasMoreElements()) {
					String url = "https://" + tokenizer.nextToken() + "/auth/openid";
					res.add(url);
				}
			}
			if (res.isEmpty()) {
				res.add("https://configure_external_url_in_bluemind/");
			}
			return res;
		}

		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainId);
		if (domainSettings != null) {
			if (domainSettings.get(DomainSettingsKeys.external_url.name()) != null) {
				res.add("https://" + domainSettings.get(DomainSettingsKeys.external_url.name()) + "/auth/openid");
			}

			String otherUrls = domainSettings.get(DomainSettingsKeys.other_urls.name());
			if (otherUrls != null) {
				StringTokenizer tokenizer = new StringTokenizer(otherUrls.trim(), " ");
				while (tokenizer.hasMoreElements()) {
					String url = "https://" + tokenizer.nextToken() + "/auth/openid";
					res.add(url);
				}
			}
			if (res.isEmpty()) {
				res.add("https://configure_external_url_in_bluemind/");
			}
			return res;
		}

		res.add("https://configure_external_url_in_bluemind/");
		return res;
	}

	private static HttpClient initHttpClient(URI uri) {
		HttpClientOptions opts = new HttpClientOptions();
		opts.setDefaultHost(uri.getHost());
		opts.setSsl(uri.getScheme().equalsIgnoreCase("https"));
		opts.setDefaultPort(
				uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80));
		if (opts.isSsl()) {
			opts.setTrustAll(true);
			opts.setVerifyHost(false);
		}
		return VertxPlatform.getVertx().createHttpClient(opts);
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
		while (nbRetries < keycloakWaitMaxRetries) {
			try {
				keycloakAdminService.allRealms();
				logger.warn("Done waiting for keycloak (got a response)");
				return;
			} catch (Throwable t) {
			}
			nbRetries++;
		}
		logger.warn("Wait for keycloak timed out (keycloak still not responding)");
	}

}
