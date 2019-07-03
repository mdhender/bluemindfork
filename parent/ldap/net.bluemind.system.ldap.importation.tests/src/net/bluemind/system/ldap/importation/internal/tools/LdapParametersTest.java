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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.junit.Test;

import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.ldap.importation.api.LdapProperties;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class LdapParametersTest {
	@Test
	public void nullDomainItemValue() {
		try {
			LdapParameters.build(null, null);
			fail("Test must thrown an exception");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void nullDomainValue() {
		try {
			LdapParameters.build(null, null);
			fail("Test must thrown an exception");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void importDisable() {
		assertFalse(LdapParameters.build(new Domain(), Collections.<String, String>emptyMap()).enabled);

		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "false");
		assertFalse(LdapParameters.build(new Domain(), Collections.<String, String>emptyMap()).enabled);

		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "");
		assertFalse(LdapParameters.build(new Domain(), Collections.<String, String>emptyMap()).enabled);
	}

	@Test
	public void validParameters() throws LdapInvalidDnException {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), Boolean.TRUE.toString());
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname");
		domain.properties.put(LdapProperties.import_ldap_login_dn.name(), "logindn");
		domain.properties.put(LdapProperties.import_ldap_password.name(), "password");
		domain.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");
		domain.properties.put(LdapProperties.import_ldap_relay_mailbox_group.name(), "relaygroup");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=basedn");
		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");
		domain.properties.put(LdapProperties.import_ldap_group_filter.name(), "groupfilter");
		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), "extid");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");

		LdapParameters ildapp = LdapParameters.build(domain, Collections.<String, String>emptyMap());
		assertTrue(ildapp.enabled);
		assertEquals(1, ildapp.ldapServer.getLdapHost().size());
		assertEquals("hostname", ildapp.ldapServer.getLdapHost().get(0).hostname);
		assertEquals("logindn", ildapp.ldapServer.login);
		assertEquals("password", ildapp.ldapServer.password);
		assertEquals("lastupdate", ildapp.lastUpdate.get());
		assertEquals("relaygroup", ildapp.splitDomain.relayMailboxGroup);
		assertEquals("dc=basedn", ildapp.ldapDirectory.baseDn.toString());
		assertEquals("userfilter", ildapp.ldapDirectory.userFilter);
		assertEquals("groupfilter", ildapp.ldapDirectory.groupFilter);
		assertEquals("extid", ildapp.ldapDirectory.extIdAttribute);
		assertFalse(ildapp.splitDomain.splitRelayEnabled);
		assertEquals(LdapProtocol.PLAIN, ildapp.ldapServer.protocol);
	}

	@Test
	public void splitRelayEnabled() throws LdapInvalidDnException {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), Boolean.TRUE.toString());
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "slave-relay.domain.tld");
		LdapParameters ildapp = LdapParameters.build(domain, settings);
		assertTrue(ildapp.splitDomain.splitRelayEnabled);
	}

	@Test
	public void protocol() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), Boolean.TRUE.toString());
		assertEquals(LdapProtocol.PLAIN,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "");
		assertEquals(LdapProtocol.PLAIN,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "invalid");
		assertEquals(LdapProtocol.PLAIN,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		assertEquals(LdapProtocol.PLAIN,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "PLAIN");
		assertEquals(LdapProtocol.PLAIN,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "ssl");
		assertEquals(LdapProtocol.SSL,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "SSL");
		assertEquals(LdapProtocol.SSL,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "tls");
		assertEquals(LdapProtocol.TLS,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "TLS");
		assertEquals(LdapProtocol.TLS,
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapServer.protocol);
	}
}
