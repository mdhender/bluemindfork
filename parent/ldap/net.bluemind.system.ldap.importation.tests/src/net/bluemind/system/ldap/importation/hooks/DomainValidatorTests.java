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
package net.bluemind.system.ldap.importation.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.importation.api.LdapProperties;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class DomainValidatorTests {
	@Test
	public void testSupported() {
		Class<Domain> supported = new DomainValidatorFactory().support();
		assertEquals(Domain.class, supported);
	}

	private DomainValidator getDomainGlobalValidator() {
		return new DomainValidator(getBmContext(SecurityContext.ROLE_SYSTEM));
	}

	private DomainValidator getDomainAdminValidator() {
		return new DomainValidator(getBmContext(SecurityContext.ROLE_ADMIN));
	}

	private BmContext getBmContext(final String securityContextRole) {
		return new BmTestContext(new SecurityContext("test", "test", Arrays.<String> asList(),
				Arrays.asList(securityContextRole), "test"), null);
	}

	@Test
	public void invalidEnabled() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "");

		try {
			getDomainGlobalValidator().create(domain);
		} catch (ServerFault sf) {
			assertEquals("Enabled value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
		} catch (ServerFault sf) {
			assertEquals("Enabled value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "invalid");
		try {
			getDomainGlobalValidator().create(domain);
		} catch (ServerFault sf) {
			assertEquals("Enabled value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
		} catch (ServerFault sf) {
			assertEquals("Enabled value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void disableLdap() throws ServerFault {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "false");

		getDomainGlobalValidator().create(domain);
		getDomainGlobalValidator().update(new Domain(), domain);

		getDomainAdminValidator().create(domain);
		try {
			getDomainAdminValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't disable LDAP", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void ldapHostname() throws ServerFault {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid hostname", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid hostname", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "  ");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid hostname", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid hostname", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
	}

	@Test
	public void ldapProtocol() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("LDAP protocol must not be null", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("LDAP protocol must not be null", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "invalid");
		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP protocol: invalid", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP protocol: invalid", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void ldapAllCertificate() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_accept_certificate.name(), "");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("All certificate value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("All certificate value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		domain.properties.put(LdapProperties.import_ldap_accept_certificate.name(), "invalid");
		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("All certificate value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("All certificate value must be null, true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void ldapBaseDn() throws ServerFault {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "invalid base dn");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP base DN", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP base DN", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void ldapLoginDn() throws ServerFault {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=local");
		domain.properties.put(LdapProperties.import_ldap_login_dn.name(), "invalid login dn");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP login", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP login", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void ldapUserFilter() throws ServerFault {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=local");
		domain.properties.put(LdapProperties.import_ldap_login_dn.name(), "uid=login,dc=local");
		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "invalid user filter");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP user filter", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP user filter", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void ldapGroupFilter() throws ServerFault {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=local");
		domain.properties.put(LdapProperties.import_ldap_login_dn.name(), "uid=login,dc=local");
		domain.properties.put(LdapProperties.import_ldap_user_filter.name(),
				LdapProperties.import_ldap_user_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_group_filter.name(), "invalid group filter");

		try {
			getDomainGlobalValidator().create(domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP group filter", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getDomainGlobalValidator().update(new Domain(), domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP group filter", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void ldapValidParameters() throws ServerFault {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "ssl");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=local");
		domain.properties.put(LdapProperties.import_ldap_login_dn.name(), "uid=login,dc=local");
		domain.properties.put(LdapProperties.import_ldap_user_filter.name(),
				LdapProperties.import_ldap_user_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_group_filter.name(),
				LdapProperties.import_ldap_group_filter.getDefaultValue());

		getDomainGlobalValidator().create(domain);
		getDomainGlobalValidator().update(new Domain(), domain);
	}

	@Test
	public void ldapDomainAdminUpdate() throws ServerFault {
		Domain previous = new Domain();

		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "hostname.domainname.tld");
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		domain.properties.put(LdapProperties.import_ldap_accept_certificate.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=local");
		domain.properties.put(LdapProperties.import_ldap_login_dn.name(), "uid=login,dc=local");
		domain.properties.put(LdapProperties.import_ldap_password.name(), "passwd");
		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(),
				LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_relay_mailbox_group.name(), "relaygroup");
		domain.properties.put(LdapProperties.import_ldap_user_filter.name(),
				LdapProperties.import_ldap_user_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_group_filter.name(),
				LdapProperties.import_ldap_group_filter.getDefaultValue());

		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't disable LDAP", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_enabled.name(),
				domain.properties.get(LdapProperties.import_ldap_enabled.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP server hostname", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_hostname.name(),
				domain.properties.get(LdapProperties.import_ldap_hostname.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP protocol", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_protocol.name(),
				domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP all certificate", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_accept_certificate.name(),
				domain.properties.get(LdapProperties.import_ldap_accept_certificate.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP base DN", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_base_dn.name(),
				domain.properties.get(LdapProperties.import_ldap_base_dn.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP user login", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_login_dn.name(),
				domain.properties.get(LdapProperties.import_ldap_login_dn.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP user password", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_password.name(),
				domain.properties.get(LdapProperties.import_ldap_password.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP external ID", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(),
				domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP split domain group", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_relay_mailbox_group.name(),
				domain.properties.get(LdapProperties.import_ldap_relay_mailbox_group.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP users filter", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_user_filter.name(),
				domain.properties.get(LdapProperties.import_ldap_user_filter.name()));
		try {
			getDomainAdminValidator().update(previous, domain);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Domain admin can't update LDAP groups filter", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		previous.properties.put(LdapProperties.import_ldap_group_filter.name(),
				domain.properties.get(LdapProperties.import_ldap_group_filter.name()));
		getDomainAdminValidator().update(previous, domain);
	}
}
