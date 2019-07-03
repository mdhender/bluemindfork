/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.auth;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class KrbAuthConfigChangeHandler extends HpsHelper implements ISystemConfigurationObserver {

	private enum Status {
		Install, Remove, None
	}

	private static final Logger logger = LoggerFactory.getLogger(KrbAuthConfigChangeHandler.class);

	private BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		switch (krbAuthHasChanged(previous, conf)) {
		case Install:
			logger.info("Authentication has changed, installing kerberos conf files");
			installKrb(conf);
			break;
		case Remove:
			logger.info("Authentication has changed, Removing kerberos conf files");
			removeKrb();
		default:
			break;
		}

	}

	private void removeKrb() throws ServerFault {
		List<ItemValue<Server>> servers = hpsNodes(context);
		for (ItemValue<Server> server : servers) {
			removeKerberosParameters(server.value);
		}
	}

	private void installKrb(SystemConf conf) throws ServerFault {

		List<ItemValue<Server>> servers = hpsNodes(context);
		String keytabBase64 = conf.values.get(SysConfKeys.krb_keytab.name());
		byte[] keytab = Base64.getDecoder().decode(keytabBase64);
		for (ItemValue<Server> server : servers) {
			updateKerberosParameters(server.value, conf.values.get(SysConfKeys.krb_ad_domain.name()),
					conf.values.get(SysConfKeys.krb_ad_ip.name()), conf.values.get(SysConfKeys.krb_domain.name()),
					keytab);
		}
	}

	private Status krbAuthHasChanged(SystemConf previous, SystemConf conf) {
		Map<String, String> previousValues = previous.values;
		Map<String, String> currentValues = conf.values;

		if (currentValues.get(SysConfKeys.auth_type.name()) != null
				&& currentValues.get(SysConfKeys.auth_type.name()).equals("KERBEROS")) {
			if (!"KERBEROS".equals(previousValues.get(SysConfKeys.auth_type.name()))) {
				return Status.Install;
			}

			if (isDifferent(previousValues, currentValues, SysConfKeys.krb_ad_domain)) {
				return Status.Install;
			}
			if (isDifferent(previousValues, currentValues, SysConfKeys.krb_ad_ip)) {
				return Status.Install;
			}
			if (isDifferent(previousValues, currentValues, SysConfKeys.krb_domain)) {
				return Status.Install;
			}

			if (isDifferent(previousValues, currentValues, SysConfKeys.krb_keytab)) {
				return Status.Install;
			}

		}

		if (currentValues.get(SysConfKeys.auth_type.name()) != null
				&& !currentValues.get(SysConfKeys.auth_type.name()).equals("KERBEROS")) {
			if (previousValues.get(SysConfKeys.auth_type.name()) != null
					&& previousValues.get(SysConfKeys.auth_type.name()).equals("KERBEROS")) {
				return Status.Remove;
			}
		}

		return Status.None;
	}

	private boolean isDifferent(Map<String, String> previousValues, Map<String, String> currentValues,
			SysConfKeys authType) {
		String prev = previousValues.get(authType.name());
		String current = currentValues.get(authType.name());
		if (prev == null && current == null) {
			return false;
		} else {
			if (current == null) {
				return true;
			} else {
				return !current.equals(prev);
			}
		}
	}

	private String buildKrb5File(String adDomain, String adIp) {
		StringBuilder sb = new StringBuilder();

		sb.append("[libdefaults]\n");
		sb.append("default_realm = " + adDomain.toUpperCase() + "\n");
		sb.append("default_keytab_name = FILE:/etc/bm-hps/hps.keytab\n");
		sb.append("default_tkt_enctypes = rc4-hmac,aes256-cts-hmac-sha1-96,aes128-cts-hmac-sha1-96\n");
		sb.append("default_tgs_enctypes = rc4-hmac,aes256-cts-hmac-sha1-96,aes128-cts-hmac-sha1-96\n");
		sb.append("forwardable=true\n\n");
		sb.append("[realms]\n");
		sb.append(adDomain.toUpperCase() + " = {\n");
		sb.append("  kdc = " + adIp + ":88\n");
		sb.append("}\n\n");
		sb.append("[domain_realm]\n");
		sb.append(adDomain.toLowerCase() + " = " + adDomain.toUpperCase() + "\n");
		sb.append("." + adDomain.toLowerCase() + " = " + adDomain.toUpperCase() + "\n");

		return sb.toString();
	}

	private String buildJaasFile(String bmExternalUrl, String adDomain) {
		StringBuilder sb = new StringBuilder();

		sb.append("ServicePrincipalLoginContext {\n");
		sb.append("    com.sun.security.auth.module.Krb5LoginModule required\n");
		sb.append("    doNotPrompt=true\n");
		sb.append("    principal=\"HTTP/" + bmExternalUrl.toLowerCase() + "@" + adDomain.toUpperCase() + "\"\n");
		sb.append("    useKeyTab=true\n");
		sb.append("    keyTab=\"/etc/bm-hps/hps.keytab\"\n");
		sb.append("    storeKey=true\n");
		sb.append("    useTicketCache=true\n");
		sb.append("    debug=false;\n");
		sb.append("};\n");

		return sb.toString();
	}

	private String buildMemConf() {
		return "KRB=\"-Djava.security.krb5.conf=/etc/bm-hps/krb5.ini\"\n"
				+ "KRB=\"$KRB -Djava.security.auth.login.config=/etc/bm-hps/jaas.conf\"\n\n"
				+ "JVM_EXT_OPTS=\"$KRB\"\n";
	}

	private String buildMappingsFile(String adDomain, String bmDomain) {
		return "[bm_mappings]\n" + adDomain.toUpperCase() + "=" + bmDomain + "\n";
	}

	private void updateKerberosParameters(Server server, String adDomain, String adIp, String bmDomain, byte[] keytab)
			throws ServerFault {
		// Read external-url for "jaas.conf" file
		String bmIni = nodeRead(server, "/etc/bm/bm.ini");
		String bmExternalUrl = "";
		for (String line : bmIni.split("\n")) {
			if (line.startsWith("external-url")) {
				bmExternalUrl = line.replaceAll("^.*?=", "").trim();
			}
		}

		nodeWrite(server, "/etc/bm-hps/hps.keytab", keytab);
		// Write "jaas.conf" file
		nodeWrite(server, "/etc/bm-hps/jaas.conf", buildJaasFile(bmExternalUrl, adDomain));

		// Read local/bm-hps.ini
		String localHps = nodeRead(server, "/etc/bm/local/bm-hps.ini");
		if (localHps == null) {
			// File doesn't exist
			// Copy template file and add "mem_conf.ini" to the end
			String defaultHps = nodeRead(server, "/etc/bm/default/bm-hps.ini");
			if (defaultHps != null) {
				nodeWrite(server, "/etc/bm/local/bm-hps.ini", defaultHps + "\n" + buildMemConf());
			}
		} else {
			// File exists : add "mem_conf.ini" to the end
			nodeWrite(server, "/etc/bm/local/bm-hps.ini", localHps + "\n" + buildMemConf());
		}

		// Build and write krb5.ini file
		nodeWrite(server, "/etc/bm-hps/krb5.ini", buildKrb5File(adDomain, adIp));

		// Add mapping.ini file if bmDomain != adDomain (case insensitive)
		if (!bmDomain.toLowerCase().equals(adDomain.toLowerCase())) {
			nodeWrite(server, "/etc/bm-hps/mappings.ini", buildMappingsFile(adDomain, bmDomain));
		}
		// restart hps
		restartHps(server);
	}

	private void removeKerberosParameters(Server server) throws ServerFault {

		// Remove Kerberos configuration files
		nodeClientFactory.create(server.address()).executeCommandNoOut(
				"rm /etc/bm-hps/hps.keytab /etc/bm-hps/jaas.conf " + " /etc/bm-hps/krb5.ini /etc/bm-hps/mappings.ini");

		// Remove "mem_conf.ini" from local/bm-hps.ini file
		String localHps = nodeRead(server, "/etc/bm/local/bm-hps.ini");
		if (localHps != null && !localHps.isEmpty()) {
			nodeWrite(server, "/etc/bm/local/bm-hps.ini", localHps.replace(buildMemConf(), ""));
		}
		restartHps(server);
	}

}
