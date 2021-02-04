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
package net.bluemind.pool.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.utils.IniFile;

public class BmConfIni extends IniFile {

	private static String getIniPath() {
		String ret = System.getProperty("net.bluemind.ini.path");
		if (ret == null) {
			ret = "/etc/bm/bm.ini";
		}
		return ret;
	}

	private static Map<String, String> overrideMap = new HashMap<>();

	public BmConfIni() {
		super(getIniPath());
		System.out.println("Docker based conf.");

		List<ItemValue<Server>> topo = new LinkedList<>();
		topo.add(tagged("127.0.0.1", "bm/core"));

		overrideMap.putAll(DockerEnv.getImagesMap());
		String esHost = DockerEnv.getIp(DockerContainer.ELASTICSEARCH.getName());
		if (esHost != null) {
			overrideMap.put(DockerContainer.ELASTICSEARCH.getHostProperty(), esHost);
			topo.add(tagged(esHost, "bm/es"));
		}

		String host = DockerEnv.getIp(DockerContainer.POSTGRES.getName());
		if (null == host) {
			host = DockerEnv.getIp(DockerContainer.POSTGRES_MIGRATION.getName());
		}
		if (host != null) {
			overrideMap.put(DockerContainer.POSTGRES.getHostProperty(), host);
			overrideMap.put("db", "test");
			overrideMap.put("user", "test");
			overrideMap.put("password", "test");// NOSONAR
			overrideMap.put("dbtype", "PGSQL");
			topo.add(tagged(host, TagDescriptor.bm_pgsql.getTag()));
			topo.add(tagged(host, TagDescriptor.bm_pgsql_data.getTag()));
		}

		String nodeHost = DockerEnv.getIp(DockerContainer.NODE.getName());
		if (nodeHost != null) {
			overrideMap.put(DockerContainer.NODE.getHostProperty(), nodeHost);
			topo.add(tagged(nodeHost, TagDescriptor.bm_filehosting.getTag()));
		}

		String smtpHost = DockerEnv.getIp(DockerContainer.SMTP_ROLE.getName());
		if (smtpHost != null) {
			overrideMap.put(DockerContainer.SMTP_ROLE.getHostProperty(), smtpHost);
			topo.add(tagged(smtpHost, TagDescriptor.mail_smtp.getTag()));
		}

		String smtpEdgeHost = DockerEnv.getIp(DockerContainer.SMTP_EDGE.getName());
		if (smtpEdgeHost != null) {
			overrideMap.put(DockerContainer.SMTP_EDGE.getHostProperty(), smtpEdgeHost);
			topo.add(tagged(smtpEdgeHost, TagDescriptor.mail_smtp_edge.getTag()));
		}

		String imapHost = DockerEnv.getIp(DockerContainer.IMAP.getName());
		if (imapHost != null) {
			overrideMap.put(DockerContainer.IMAP.getHostProperty(), imapHost);
			topo.add(tagged(imapHost, TagDescriptor.mail_imap.getTag()));
		}

		String ldapHost = DockerEnv.getIp(DockerContainer.LDAP.getName());
		if (ldapHost != null) {
			overrideMap.put(DockerContainer.LDAP.getHostProperty(), ldapHost);
		}

		String samba4Host = DockerEnv.getIp(DockerContainer.SAMBA4.getName());
		if (samba4Host != null) {
			overrideMap.put(DockerContainer.SAMBA4.getHostProperty(), samba4Host);
		}

		String mailboxRoleHost = DockerEnv.getIp(DockerContainer.MAILBOX_ROLE.getName());
		if (mailboxRoleHost != null) {
			overrideMap.put(DockerContainer.MAILBOX_ROLE.getHostProperty(), mailboxRoleHost);
			topo.add(tagged(mailboxRoleHost, TagDescriptor.mail_imap.getTag()));
			topo.add(tagged(mailboxRoleHost, TagDescriptor.bm_pgsql_data.getTag()));

		}

		String proxy = DockerEnv.getIp(DockerContainer.PROXY.getName());
		if (proxy != null) {
			overrideMap.put(DockerContainer.PROXY.getHostProperty(), proxy);
		}

		if (BmConfIniExtraSettings.settings != null && !BmConfIniExtraSettings.settings.isEmpty()) {
			overrideMap.putAll(BmConfIniExtraSettings.settings);
		}
		Optional<IServiceTopology> topology = Topology.getIfAvailable();
		if (!topology.isPresent()) {
			Topology.update(topo);
		}
	}

	private ItemValue<Server> tagged(String ip, String... tags) {
		return ItemValue.create(ip, Server.tagged(ip, tags));
	}

	@Override
	public String getCategory() {
		return "bm";
	}

	public String get(String string) {
		String overrideValue = overrideMap.get(string);
		if (overrideValue != null) {
			return overrideValue;
		}

		String value = getProperty(string);
		if (value != null) {
			return value.replace("\"", "");
		} else {
			return null;
		}
	}
}
