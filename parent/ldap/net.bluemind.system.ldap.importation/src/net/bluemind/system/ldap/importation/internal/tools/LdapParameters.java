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
package net.bluemind.system.ldap.importation.internal.tools;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.Parameters.Server.Host;
import net.bluemind.system.importation.commons.exceptions.InvalidDnServerFault;
import net.bluemind.system.ldap.importation.api.LdapProperties;

public class LdapParameters extends Parameters {
	private static final Logger logger = LoggerFactory.getLogger(LdapParameters.class);

	public static class LdapServer extends Server {
		public LdapServer(Host host, String login, String password, LdapProtocol protocol,
				boolean acceptAllCertificates) {
			super(Optional.ofNullable(host), login, password, protocol, acceptAllCertificates);
		}

		@Override
		protected List<Host> getAlternativeHosts() {
			return Collections.emptyList();
		}

	}

	protected LdapParameters(boolean enabled, Server ldapServer, Directory ldapDirectory, SplitDomain splitDomain,
			Optional<String> lastUpdate) {
		super(enabled, ldapServer, ldapDirectory, splitDomain, lastUpdate);
	}

	public static LdapParameters build(Domain domain, Map<String, String> domainSettings) throws InvalidDnServerFault {
		if (domain == null) {
			throw new IllegalArgumentException();
		}

		if (!Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_enabled.name()))) {
			return new LdapParameters(false, null, null, null, Optional.empty());
		}

		String splitDomainRelayHostname = domainSettings.get(DomainSettingsKeys.mail_routing_relay.name());

		Host host = null;
		if (domain.properties.containsKey(LdapProperties.import_ldap_hostname.name())
				&& !Strings.isNullOrEmpty(domain.properties.get(LdapProperties.import_ldap_hostname.name()))) {
			host = Host.build(domain.properties.get(LdapProperties.import_ldap_hostname.name()), 389, 0, 0);
		}

		LdapProtocol protocol = LdapProtocol.PLAIN;
		try {
			protocol = LdapProtocol.getProtocol(domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		} catch (IllegalArgumentException | NullPointerException i) {
			logger.error("Invalid protocol '{}', use: {}",
					domain.properties.get(LdapProperties.import_ldap_protocol.name()), LdapProtocol.PLAIN.toString());
		}

		return new LdapParameters(true,
				new LdapServer(host, domain.properties.get(LdapProperties.import_ldap_login_dn.name()),
						domain.properties.get(LdapProperties.import_ldap_password.name()), protocol,
						Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_accept_certificate.name()))),
				Directory.build(domain.properties.get(LdapProperties.import_ldap_base_dn.name()),
						domain.properties.get(LdapProperties.import_ldap_user_filter.name()),
						domain.properties.get(LdapProperties.import_ldap_group_filter.name()),
						domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name())),
				new SplitDomain(splitDomainRelayHostname != null && !splitDomainRelayHostname.trim().isEmpty(),
						domain.properties.get(LdapProperties.import_ldap_relay_mailbox_group.name())),
				Optional.ofNullable(domain.properties.get(LdapProperties.import_ldap_lastupdate.name())));
	}

	public static LdapParameters build(String hostname, String protocol, String allCertificate, String baseDn,
			String loginDn, String password) throws InvalidDnServerFault {
		LdapServer server = new LdapServer(Host.build(hostname, 389, 0, 0), loginDn, password,
				LdapProtocol.getProtocol(protocol), Boolean.valueOf(allCertificate));
		Directory directory = Directory.build(baseDn, null, null, null);
		SplitDomain sd = new SplitDomain(false, null);

		return new LdapParameters(true, server, directory, sd, null);
	}

	public LdapParameters updateLastUpdate(Optional<String> lastUpdate) {
		return new LdapParameters(enabled, ldapServer, ldapDirectory, splitDomain, lastUpdate);
	}
}
