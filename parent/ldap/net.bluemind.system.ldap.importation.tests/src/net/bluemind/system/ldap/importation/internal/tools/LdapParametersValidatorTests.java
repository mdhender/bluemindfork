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
package net.bluemind.system.ldap.importation.internal.tools;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.ldap.importation.api.LdapProperties;

public class LdapParametersValidatorTests {
	@Test
	public void validParameters() {
		LdapParametersValidator.validate(getValidProperties(), Locale.ENGLISH);
	}

	@Test
	public void checkLdapAllCertificate() {
		LdapParametersValidator.checkLdapAllCertificate(null);
		LdapParametersValidator.checkLdapAllCertificate("true");
		LdapParametersValidator.checkLdapAllCertificate("TRUE");
		LdapParametersValidator.checkLdapAllCertificate("false");
		LdapParametersValidator.checkLdapAllCertificate("FALSE");

		try {
			LdapParametersValidator.checkLdapAllCertificate("invalid");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void checkLdapProtocol() {
		LdapParametersValidator.checkLdapProtocol("plain", Locale.ENGLISH);

		try {
			LdapParametersValidator.checkLdapProtocol(null, Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}

		try {
			LdapParametersValidator.checkLdapProtocol("invalid", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void checkLdapHostname() {
		LdapParametersValidator.checkLdapHostname("hostname", Locale.ENGLISH);
		LdapParametersValidator.checkLdapHostname("hostname:389", Locale.ENGLISH);

		try {
			LdapParametersValidator.checkLdapHostname(null, Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}

		try {
			LdapParametersValidator.checkLdapHostname("host:name:invalid", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}

		try {
			LdapParametersValidator.checkLdapHostname("hostname:-1", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}

		try {
			LdapParametersValidator.checkLdapHostname("hostname:65536", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void checkLdapBaseDn() {
		LdapParametersValidator.checkLdapBaseDn(null, Locale.ENGLISH);
		LdapParametersValidator.checkLdapBaseDn("dc=base", Locale.ENGLISH);

		try {
			LdapParametersValidator.checkLdapBaseDn("invalid", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void checkLdapLoginDn() {
		LdapParametersValidator.checkLdapLoginDn(null, Locale.ENGLISH);
		LdapParametersValidator.checkLdapLoginDn("uid=login", Locale.ENGLISH);

		try {
			LdapParametersValidator.checkLdapLoginDn("invalid", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void checkLdapUserFilter() {
		LdapParametersValidator.checkLdapUserFilter("(objectclass=user)", Locale.ENGLISH);

		try {
			LdapParametersValidator.checkLdapUserFilter(null, Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}

		try {
			LdapParametersValidator.checkLdapUserFilter("invalid", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void checkLdapGroupFilter() {
		LdapParametersValidator.checkLdapGroupFilter("(objectclass=group)", Locale.ENGLISH);

		try {
			LdapParametersValidator.checkLdapGroupFilter(null, Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}

		try {
			LdapParametersValidator.checkLdapGroupFilter("invalid", Locale.ENGLISH);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void noChanges() {
		LdapParametersValidator.noChanges(getValidProperties(), getValidProperties());
	}

	@Test
	public void noChanges_propertyUpdated() {
		for (LdapProperties ldapProperty : LdapProperties.values()) {
			Map<String, String> updatedProperties = getValidProperties();
			updatedProperties.put(ldapProperty.name(), "updated");
			try {
				LdapParametersValidator.noChanges(getValidProperties(), updatedProperties);
				fail("Test must thrown an exception for property updated: " + ldapProperty.name());
			} catch (ServerFault sf) {
			}
		}
	}

	private Map<String, String> getValidProperties() {
		Map<String, String> properties = new HashMap<>();
		properties.put(LdapProperties.import_ldap_enabled.name(), Boolean.TRUE.toString());
		properties.put(LdapProperties.import_ldap_hostname.name(), "hostname");
		properties.put(LdapProperties.import_ldap_login_dn.name(), "uid=login");
		properties.put(LdapProperties.import_ldap_password.name(), "password");
		properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");
		properties.put(LdapProperties.import_ldap_relay_mailbox_group.name(), "relaygroup");
		properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=basedn");
		properties.put(LdapProperties.import_ldap_user_filter.name(), "(objectclass=inetorgperson)");
		properties.put(LdapProperties.import_ldap_group_filter.name(), "(objectclass=groupfilter)");
		properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), "extid");
		properties.put(LdapProperties.import_ldap_protocol.name(), "plain");

		return properties;
	}
}
