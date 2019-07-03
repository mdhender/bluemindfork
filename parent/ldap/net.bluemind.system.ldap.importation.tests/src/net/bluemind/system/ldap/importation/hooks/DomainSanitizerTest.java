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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.importation.api.LdapProperties;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class DomainSanitizerTest {
	@Test
	public void testSupported() {
		Class<Domain> supported = new DomainSanitizerFactory().support();
		assertEquals(Domain.class, supported);
	}

	@Test
	public void nullDomains() {
		try {
			getDomainGlobalSanitizer().update(null, new Domain());
			fail("Test must thrown an exception");
		} catch (NullPointerException e) {
		}

		try {
			getDomainGlobalSanitizer().update(new Domain(), null);
			fail("Test must thrown an exception");
		} catch (NullPointerException e) {
		}

		try {
			getDomainGlobalSanitizer().create(null);
			fail("Test must thrown an exception");
		} catch (NullPointerException e) {
		}
	}

	@Test
	public void setDefaultsCreate() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");

		getDomainGlobalSanitizer().create(domain);
		assertTrue(Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_enabled.name())));
		assertEquals("plain", domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		assertFalse(Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_accept_certificate.name())));
		assertEquals(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name()));
		assertEquals(LdapProperties.import_ldap_user_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_user_filter.name()));
		assertEquals(LdapProperties.import_ldap_group_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_group_filter.name()));
		assertEquals(null, domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void setDefaultsUpdate() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");

		getDomainGlobalSanitizer().update(new Domain(), domain);
		assertTrue(Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_enabled.name())));
		assertEquals("plain", domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		assertFalse(Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_accept_certificate.name())));
		assertEquals(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name()));
		assertEquals(LdapProperties.import_ldap_user_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_user_filter.name()));
		assertEquals(LdapProperties.import_ldap_group_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_group_filter.name()));
		assertEquals(null, domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void setPrevious() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");

		Domain previous = new Domain();
		previous.properties.put(LdapProperties.import_ldap_protocol.name(), "ssl");
		previous.properties.put(LdapProperties.import_ldap_accept_certificate.name(), "true");
		previous.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), "extid");
		previous.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");
		previous.properties.put(LdapProperties.import_ldap_group_filter.name(), "groupfilter");
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");

		getDomainGlobalSanitizer().update(previous, domain);
		assertEquals("extid", domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name()));
		assertEquals("ssl", domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		assertTrue(Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_accept_certificate.name())));
		assertEquals("userfilter", domain.properties.get(LdapProperties.import_ldap_user_filter.name()));
		assertEquals("groupfilter", domain.properties.get(LdapProperties.import_ldap_group_filter.name()));
		assertEquals(null, domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void previousNullOrEmtpySetDefaults() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");

		Domain previous = new Domain();
		previous.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		previous.properties.put(LdapProperties.import_ldap_protocol.name(), null);
		previous.properties.put(LdapProperties.import_ldap_accept_certificate.name(), null);
		previous.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), null);
		previous.properties.put(LdapProperties.import_ldap_user_filter.name(), null);
		previous.properties.put(LdapProperties.import_ldap_group_filter.name(), null);
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), null);

		getDomainGlobalSanitizer().update(previous, domain);
		assertTrue(Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_enabled.name())));
		assertEquals(LdapProperties.import_ldap_protocol.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		assertEquals(LdapProperties.import_ldap_accept_certificate.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_accept_certificate.name()));
		assertEquals(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name()));
		assertEquals(LdapProperties.import_ldap_user_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_user_filter.name()));
		assertEquals(LdapProperties.import_ldap_group_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_group_filter.name()));
		assertEquals(null, domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));

		domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		previous.properties.put(LdapProperties.import_ldap_protocol.name(), "   ");
		previous.properties.put(LdapProperties.import_ldap_accept_certificate.name(), "   ");
		previous.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), "   ");
		previous.properties.put(LdapProperties.import_ldap_user_filter.name(), "   ");
		previous.properties.put(LdapProperties.import_ldap_group_filter.name(), "   ");
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), "   ");

		getDomainGlobalSanitizer().update(previous, domain);
		assertTrue(Boolean.valueOf(domain.properties.get(LdapProperties.import_ldap_enabled.name())));
		assertEquals(LdapProperties.import_ldap_protocol.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		assertEquals(LdapProperties.import_ldap_accept_certificate.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_accept_certificate.name()));
		assertEquals(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name()));
		assertEquals(LdapProperties.import_ldap_user_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_user_filter.name()));
		assertEquals(LdapProperties.import_ldap_group_filter.getDefaultValue(),
				domain.properties.get(LdapProperties.import_ldap_group_filter.name()));
		assertEquals(null, domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void sanitizeUpdated() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "ssl");
		domain.properties.put(LdapProperties.import_ldap_accept_certificate.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), "extid");
		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");
		domain.properties.put(LdapProperties.import_ldap_group_filter.name(), "groupfilter");
		domain.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");

		Domain previous = new Domain();
		previous.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");
		previous.properties.put(LdapProperties.import_ldap_accept_certificate.name(), "false");
		previous.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(), "oldextid");
		previous.properties.put(LdapProperties.import_ldap_user_filter.name(), "olduserfilter");
		previous.properties.put(LdapProperties.import_ldap_group_filter.name(), "oldgroupfilter");
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), "oldlastupdate");

		getDomainGlobalSanitizer().update(previous, domain);
		assertEquals("ssl", domain.properties.get(LdapProperties.import_ldap_protocol.name()));
		assertEquals("true", domain.properties.get(LdapProperties.import_ldap_accept_certificate.name()));
		assertEquals("extid", domain.properties.get(LdapProperties.import_ldap_ext_id_attribute.name()));
		assertEquals("userfilter", domain.properties.get(LdapProperties.import_ldap_user_filter.name()));
		assertEquals("groupfilter", domain.properties.get(LdapProperties.import_ldap_group_filter.name()));
		assertEquals("lastupdate", domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void sanitizeNullOrEmptyNewLastUpdate() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_lastupdate.name(), null);

		Domain previous = new Domain();
		previous.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), "oldlastupdate");

		getDomainGlobalSanitizer().update(previous, domain);
		assertEquals(null, domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));

		domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_lastupdate.name(), " ");

		getDomainGlobalSanitizer().update(previous, domain);
		assertEquals(" ", domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	@Test
	public void sanitizeNullOrEmptyPreviousLastUpdate() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");

		Domain previous = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), null);

		getDomainGlobalSanitizer().update(previous, domain);
		assertEquals("lastupdate", domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));

		previous = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), "  ");

		getDomainGlobalSanitizer().update(previous, domain);
		assertEquals("lastupdate", domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}

	private DomainSanitizer getDomainGlobalSanitizer() {
		return new DomainSanitizer(getBmContext(SecurityContext.ROLE_SYSTEM));
	}

	private DomainSanitizer getDomainAdminSanitizer() {
		return new DomainSanitizer(getBmContext(SecurityContext.ROLE_ADMIN));
	}

	private BmContext getBmContext(final String securityContextRole) {
		return new BmTestContext(new SecurityContext("test", "test", Arrays.<String> asList(),
				Arrays.asList(securityContextRole), "test"), null);
	}

	@Test
	public void sanitizeAsDomainAdminLastUpdate() {
		Domain domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");

		getDomainAdminSanitizer().create(domain);

		assertTrue(domain.properties.containsKey(LdapProperties.import_ldap_lastupdate.name()));
		assertNull(domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));

		domain = new Domain();
		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");

		Domain previous = new Domain();
		previous.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		previous.properties.put(LdapProperties.import_ldap_lastupdate.name(), "previous");

		getDomainAdminSanitizer().update(previous, domain);

		assertTrue(domain.properties.containsKey(LdapProperties.import_ldap_lastupdate.name()));
		assertEquals("previous", domain.properties.get(LdapProperties.import_ldap_lastupdate.name()));
	}
}
