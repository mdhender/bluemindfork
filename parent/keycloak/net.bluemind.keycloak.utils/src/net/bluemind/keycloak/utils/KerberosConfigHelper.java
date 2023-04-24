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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
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
	private static final String lastConfLocation = "/etc/bm-keycloak/krbconf.json";
	private static final String krb5ConfPath = "/etc/krb5.conf";
	private static final int keycloakWaitMaxRetries = 6; // 5sec per retry => 30sec max wait

	public static void updateKeycloakKerberosConf(String domainUid) {
		logger.info("Domain {} created/updated : updating kerberos conf (if needed)", domainUid);

		IDomains domainsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class);
		ItemValue<Domain> domain = domainsService.get(domainUid);
		if (domain.value.properties == null
				|| domain.value.properties.get(DomainAuthProperties.auth_type.name()) == null) {
			logger.warn("skipping kerberos conf update for domain " + domainUid + " (no domain properties)");
			return;
		}

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IKeycloakKerberosAdmin kerberosService = provider.instance(IKeycloakKerberosAdmin.class, domainUid);
		try {
			kerberosService.deleteKerberosProvider(domainUid + "-kerberos");
		} catch (Throwable t) {
		}

		if (AuthTypes.KERBEROS.name().equals(domain.value.properties.get(DomainAuthProperties.auth_type.name()))) {
			String krb_ad_domain = domain.value.properties.get(DomainAuthProperties.krb_ad_domain.name());
			String krb_keytab = domain.value.properties.get(DomainAuthProperties.krb_keytab.name());

			SharedMap<String, String> smap = MQ.sharedMap(Shared.MAP_SYSCONF);
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

			if (!"global.virt".equals(domainUid) && domainExternalUrl == null) {
				IKeycloakKerberosAdmin kerbProv = provider.instance(IKeycloakKerberosAdmin.class, "global.virt");
				try {
					kerbProv.deleteKerberosProvider("global.virt-kerberos");
				} catch (Throwable t) {
				}
				kerb.setName("global.virt-kerberos");
				kerb.setParentId("global.virt");
				kerbProv.create(kerb);
			} else {
				kerb.setName(domainUid + "-kerberos");
				kerb.setParentId(domainUid);
				kerberosService.create(kerb);
			}
		}
		KerberosConfigHelper.updateGlobalRealmKerb();
		KerberosConfigHelper.updateKrb5Conf();
	}

	public static void updateGlobalRealmKerb() {
		boolean found = false;

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDomains domainService = provider.instance(IDomains.class);
		Iterator<ItemValue<Domain>> it = domainService.all().iterator();
		while (it.hasNext() && !found) {
			ItemValue<Domain> domain = it.next();
			found = AuthTypes.KERBEROS.name().equals(domain.value.properties.get(DomainAuthProperties.auth_type.name()))
					&& getExternalUrl(domain.uid) == null;
		}

		if (!found) {
			IKeycloakKerberosAdmin kerbProv = provider.instance(IKeycloakKerberosAdmin.class, "global.virt");
			try {
				kerbProv.deleteKerberosProvider("global.virt-kerberos");
			} catch (Throwable t) {
			}
		}
	}

	public static void updateKrb5Conf() {
		JsonObject currentConf = getConf();
		JsonObject previousConf = null;
		try {
			previousConf = new JsonObject(Files.readString(Paths.get(lastConfLocation)));
		} catch (IOException e) {
			previousConf = new JsonObject();
		}

		if (!currentConf.equals(previousConf)) {
			String kcServerAddr = Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address();
			INodeClient nodeClient = NodeActivator.get(kcServerAddr);

			nodeClient.writeFile(krb5ConfPath,
					new ByteArrayInputStream(krb5Conf(currentConf).getBytes(Charset.forName("UTF-8"))));
			nodeClient.writeFile(lastConfLocation,
					new ByteArrayInputStream(currentConf.encode().getBytes(Charset.forName("UTF-8"))));

			nodeClient.listFiles("/etc/bm-keycloak/", "keytab").forEach(file -> nodeClient.deleteFile(file.getPath()));
			currentConf.fieldNames().forEach(domainUid -> {
				nodeClient.writeFile("/etc/bm-keycloak/" + domainUid + ".keytab",
						new ByteArrayInputStream(currentConf.getJsonObject(domainUid)
								.getString(DomainAuthProperties.krb_keytab.name()).getBytes(Charset.forName("UTF-8"))));
			});

			logger.info("Keycloak restarting on server {}...", kcServerAddr);
			NCUtils.execNoOut(nodeClient, "systemctl restart bm-keycloak.service");
			waitForKeycloak();
			logger.info("Keycloak restarted on server {}", kcServerAddr);
		} else {
			logger.info("Kerberos config did not change. No need to update /etc/krb5.conf.");
		}
	}

	public static void removeKrb5Conf(String domainUid) {
		String kcServerAddr = Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address();
		INodeClient nodeClient = NodeActivator.get(kcServerAddr);
		NCUtils.execNoOut(nodeClient, "rm -f /etc/bm-keycloak/" + domainUid + ".keytab");

		updateKrb5Conf();
	}

	public static void checkKerberosConf(BmContext context, String domainUid, Map<String, String> settings) {
		IDomains domainService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class);
		checkKerberosConf(context, domainService.get(domainUid).value, settings);
	}

	public static void checkKerberosConf(BmContext context, Domain domain) {
		IDomains domainService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class);
		String domainUid = null;
		Iterator<ItemValue<Domain>> it = domainService.all().iterator();
		while (it.hasNext() && domainUid == null) {
			ItemValue<Domain> d = it.next();
			if (domain.name.equals(d.value.name)) {
				domainUid = d.uid;
			}
		}

		Map<String, String> settings = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomainSettings.class, domainUid).get();
		checkKerberosConf(context, domain, settings);
	}

	private static void checkKerberosConf(BmContext context, Domain domain, Map<String, String> settings) {
		IDomains domainService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class);

		// If no kerb let go
		String authType = domain.properties.get(DomainAuthProperties.auth_type.name());
		if (!AuthTypes.KERBEROS.name().equals(authType)) {
			return;
		}

		// external url mandatory if another kerb domain without external url exists
		String extUrl = settings.get(DomainSettingsKeys.external_url.name());
		if (extUrl == null) {
			domainService.all().forEach(d -> {
				Domain currDomain = d.value;
				if (!currDomain.name.equals(domain.name)
						&& AuthTypes.KERBEROS.name()
								.equals(currDomain.properties.get(DomainAuthProperties.auth_type.name()))
						&& getExternalUrl(context, currDomain.name) == null) {
					throw new ServerFault(
							"External Url is mandatory to enable Kerberos. Only one domain can have kerberos enabled without an external url, which is the case for "
									+ currDomain.defaultAlias + ".",
							ErrorCode.INVALID_PARAMETER);
				}
			});
		}

		// kerb params mandatory
		if (domain.properties.get(DomainAuthProperties.krb_ad_domain.name()) == null) {
			throw new ServerFault("AD Domain is mandatory for kerberos configuration", ErrorCode.INVALID_PARAMETER);
		}
		if (domain.properties.get(DomainAuthProperties.krb_ad_ip.name()) == null) {
			throw new ServerFault("AD IP adress is mandatory for kerberos configuration", ErrorCode.INVALID_PARAMETER);
		}
		if (domain.properties.get(DomainAuthProperties.krb_keytab.name()) == null) {
			throw new ServerFault("Keytab file is mandatory for kerberos configuration", ErrorCode.INVALID_PARAMETER);
		}
	}

	private static String krb5Conf(JsonObject jsonConf) {
		StringBuffer buf = new StringBuffer();

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
			buf.append("     " + domConf.getString(DomainAuthProperties.krb_ad_domain.name()) + " = {\n");
			buf.append("          kdc = " + domConf.getString(DomainAuthProperties.krb_ad_ip.name()) + "\n");
			buf.append("     }\n\n");
		});

		return buf.toString();
	}

	private static JsonObject getConf() {
		JsonObject conf = new JsonObject();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).all().forEach(domain -> {
			if (AuthTypes.KERBEROS.name().equals(domain.value.properties.get(DomainAuthProperties.auth_type.name()))) {
				conf.put(domain.uid,
						new JsonObject()
								.put(DomainAuthProperties.krb_ad_domain.name(),
										domain.value.properties.get(DomainAuthProperties.krb_ad_domain.name()))
								.put(DomainAuthProperties.krb_ad_ip.name(),
										domain.value.properties.get(DomainAuthProperties.krb_ad_ip.name()))
								.put(DomainAuthProperties.krb_keytab.name(),
										domain.value.properties.get(DomainAuthProperties.krb_keytab.name())));
			}
		});

		return conf;
	}

	private static String getExternalUrl(String domainUid) {
		return getExternalUrl(null, domainUid);
	}

	private static String getExternalUrl(BmContext context, String domainUid) {
		return ServerSideServiceProvider
				.getProvider(context != null ? context.getSecurityContext() : SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get().get(DomainSettingsKeys.external_url.name());
	}

	public static void waitForKeycloak() {
		IKeycloakAdmin keycloakAdminService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IKeycloakAdmin.class);

		int nbRetries = 0;
		while (nbRetries < keycloakWaitMaxRetries) {
			try {
				keycloakAdminService.allRealms();
				return;
			} catch (Throwable t) {
			}
			nbRetries++;
		}
	}
}
