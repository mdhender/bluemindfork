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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.keycloak.api.KerberosComponent.CachePolicy;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.SysConfKeys;

public class KerberosConfigHelper {

	private static final Logger logger = LoggerFactory.getLogger(KerberosConfigHelper.class);
	private static final String LAST_CONF_LOCATION = "/etc/bm-keycloak/krbconf.json";
	private static final String KRB5_CONF_PATH = "/etc/krb5.conf";
	private static final String GLOBAL_VIRT = "global.virt";
	public static final String KRB_GLOBAL_VIRT_NAME = IKeycloakUids.kerberosComponentName(GLOBAL_VIRT);

	private KerberosConfigHelper() {

	}

	public static void updateKeycloakKerberosConf(ItemValue<Domain> domain) {
		String domainUid = domain.uid;
		if (GLOBAL_VIRT.equals(domainUid)) {
			return;
		}
		if (domain.value.properties == null
				|| domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()) == null) {
			logger.warn("skipping kerberos conf update for domain {} (no domain properties)", domainUid);
			return;
		}

		logger.info("Domain {} created/updated : updating kerberos conf", domainUid);

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IKeycloakKerberosAdmin.class, domainUid)
				.deleteKerberosProvider(IKeycloakUids.kerberosComponentName(domainUid));

		if (AuthTypes.KERBEROS.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			createKeycloakKerberosConf(domain);
		}
		KerberosConfigHelper.updateGlobalRealmKerb();
		KerberosConfigHelper.updateKrb5Conf();
	}

	public static void createKeycloakKerberosConf(ItemValue<Domain> domain) {
		String krdAdDomain = domain.value.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name());
		krdAdDomain = krdAdDomain != null ? krdAdDomain.toUpperCase() : null;
		String krbKeytab = domain.value.properties.get(AuthDomainProperties.KRB_KEYTAB.name());

		SharedMap<String, String> smap = MQ.sharedMap(Shared.MAP_SYSCONF);
		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domain.uid);
		String domainExternalUrl = domainSettings.get(DomainSettingsKeys.external_url.name());
		String globalExternalUrl = smap.get(SysConfKeys.external_url.name());
		String srvPrincHost = domainExternalUrl != null ? domainExternalUrl : globalExternalUrl;

		String serverPrincipal = "HTTP/" + srvPrincHost + "@" + krdAdDomain;

		String keytabPath = getKeytabFilename(domain.uid);
		String kcServerAddr = Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address();
		INodeClient nodeClient = NodeActivator.get(kcServerAddr);
		nodeClient.writeFile(keytabPath, new ByteArrayInputStream(Base64.getDecoder().decode(krbKeytab)));

		KerberosComponent kerb = new KerberosComponent();
		kerb.setKerberosRealm(krdAdDomain);
		kerb.setServerPrincipal(serverPrincipal);
		kerb.setKeyTab(keytabPath);
		kerb.setEnabled(true);
		kerb.setDebug(true);
		kerb.setCachePolicy(CachePolicy.DEFAULT);

		if (!GLOBAL_VIRT.equals(domain.uid) && domainExternalUrl == null) {
			IKeycloakKerberosAdmin kerbProv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IKeycloakKerberosAdmin.class, GLOBAL_VIRT);
			kerbProv.deleteKerberosProvider(KRB_GLOBAL_VIRT_NAME);
			kerb.setName(KRB_GLOBAL_VIRT_NAME);
			kerb.setParentId(GLOBAL_VIRT);
			kerbProv.create(kerb);
		} else {
			kerb.setName(IKeycloakUids.kerberosComponentName(domain.uid));
			kerb.setParentId(domain.uid);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IKeycloakKerberosAdmin.class, domain.uid).create(kerb);
		}
	}

	public static void updateGlobalRealmKerb() {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDomains domainService = provider.instance(IDomains.class);

		if (domainService.all().stream().noneMatch(domain -> isKerberosWithoutExternalUrl(domain))) {
			IKeycloakKerberosAdmin kerbProv = provider.instance(IKeycloakKerberosAdmin.class, GLOBAL_VIRT);
			kerbProv.deleteKerberosProvider(KRB_GLOBAL_VIRT_NAME);
		}
	}

	private static boolean isKerberosWithoutExternalUrl(ItemValue<Domain> domain) {
		return AuthTypes.KERBEROS.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))
				&& getExternalUrl(domain.uid) == null;
	}

	public static void updateKrb5Conf() {
		JsonObject currentConf = getConf();
		JsonObject previousConf = null;
		try {
			previousConf = new JsonObject(Files.readString(Paths.get(LAST_CONF_LOCATION)));
		} catch (IOException e) {
			previousConf = new JsonObject();
		}

		if (!currentConf.equals(previousConf)) {
			String kcServerAddr = Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address();
			INodeClient nodeClient = NodeActivator.get(kcServerAddr);

			nodeClient.writeFile(KRB5_CONF_PATH,
					new ByteArrayInputStream(krb5Conf(currentConf).getBytes(StandardCharsets.UTF_8)));
			nodeClient.writeFile(LAST_CONF_LOCATION,
					new ByteArrayInputStream(currentConf.encode().getBytes(StandardCharsets.UTF_8)));

			nodeClient.listFiles("/etc/bm-keycloak/", "keytab").forEach(file -> nodeClient.deleteFile(file.getPath()));
			currentConf.fieldNames().forEach(domainUid -> nodeClient.writeFile(getKeytabFilename(domainUid),
					new ByteArrayInputStream(Base64.getDecoder().decode(
							currentConf.getJsonObject(domainUid).getString(AuthDomainProperties.KRB_KEYTAB.name())))));

			// TODO check if we need to restart keycloak here
			logger.info("Keycloak restarting on server {}...", kcServerAddr);
			NCUtils.execNoOut(nodeClient, "systemctl restart bm-keycloak.service");
			KeycloakHelper.waitForKeycloak();
			logger.info("Keycloak restarted on server {}", kcServerAddr);
		} else {
			logger.debug("Kerberos config did not change. No need to update /etc/krb5.conf.");
		}
	}

	public static void removeKrb5Conf(String domainUid) {
		String kcServerAddr = Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address();
		INodeClient nodeClient = NodeActivator.get(kcServerAddr);
		NCUtils.execNoOut(nodeClient, "rm -f " + getKeytabFilename(domainUid));
		updateKrb5Conf();
	}

	private static String krb5Conf(JsonObject jsonConf) {
		StringBuilder buf = new StringBuilder();

		buf.append("# This file is generated by Bluemind (and may be overwritten anytime).\n");
		buf.append("# Consider setting up your configuration in Bluemind, instead of editing this.\n");
		buf.append("#\n");
		buf.append("[libdefaults]\n");
		buf.append("     allow_weak_crypto = true\n");
		buf.append("\n");
		buf.append("[realms]\n");

		jsonConf.fieldNames().forEach(domainUid -> {
			JsonObject domConf = jsonConf.getJsonObject(domainUid);
			buf.append("#    Bluemind domain: " + domainUid + "\n");
			buf.append("     " + domConf.getString(AuthDomainProperties.KRB_AD_DOMAIN.name()) + " = {\n");
			buf.append("          kdc = " + domConf.getString(AuthDomainProperties.KRB_AD_IP.name()) + "\n");
			buf.append("     }\n\n");
		});

		return buf.toString();
	}

	private static JsonObject getConf() {
		JsonObject conf = new JsonObject();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).all().forEach(domain -> {
			if (AuthTypes.KERBEROS.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
				conf.put(domain.uid,
						new JsonObject()
								.put(AuthDomainProperties.KRB_AD_DOMAIN.name(),
										domain.value.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name()))
								.put(AuthDomainProperties.KRB_AD_IP.name(),
										domain.value.properties.get(AuthDomainProperties.KRB_AD_IP.name()))
								.put(AuthDomainProperties.KRB_KEYTAB.name(),
										domain.value.properties.get(AuthDomainProperties.KRB_KEYTAB.name())));
			}
		});

		return conf;
	}

	private static String getExternalUrl(String domainUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid)
				.get().get(DomainSettingsKeys.external_url.name());
	}

	private static String getKeytabFilename(String domainUid) {
		return "/etc/bm-keycloak/" + domainUid + ".keytab";
	}

}
