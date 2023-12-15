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
package net.bluemind.system.ldap.importation.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Collections;
import java.util.Optional;

import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;

public class LdapUserSearchFilterTest {
	private static final String TESTUUID = "97d55733-4a94-4e4f-a847-47b1f6162e1b";

	private Domain getDomain() {
		Domain domain = new Domain();

		domain.name = "memberof.virt";

		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(), "fake.host.name");

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(),
				LdapProperties.import_ldap_user_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(),
				LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=memberof");

		return domain;
	}

	@Test
	public void emptySearchFilter() {
		Domain domain = getDomain();

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), null);

		assertEquals("", new LdapUserSearchFilter()
				.getSearchFilter(LdapParameters.build(domain, Collections.<String, String>emptyMap())));
	}

	@Test
	public void userFilterOnly() throws ParseException {
		Domain domain = getDomain();

		String filter = new LdapUserSearchFilter()
				.getSearchFilter(LdapParameters.build(domain, Collections.<String, String>emptyMap()));

		assertEquals(LdapProperties.import_ldap_user_filter.getDefaultValue(), filter);
		FilterParser.parse(filter);
	}

	private String getExpectedLastUpdateFilterPart() {
		return "(" + LdapConstants.MODIFYTIMESTAMP_ATTR + ">=lastupdate)";
	}

	private String getExpectedUuidFilterPart(String uuidAttr) {
		return "(" + uuidAttr + "=" + TESTUUID + ")";
	}

	private String getExpectedLoginFilterPart() {
		return "(" + UserManagerImpl.LDAP_LOGIN + "=login)";
	}

	@Test
	public void lastUpdateFilter() throws ParseException {
		Domain domain = getDomain();

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");

		// Userfilter + lastupdate
		String filter = new LdapUserSearchFilter().getSearchFilterByLastModification(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.of("lastupdate"));

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedLastUpdateFilterPart()));
		assertEquals("(&)", filter.replace("userfilter", "").replace(getExpectedLastUpdateFilterPart(), ""));
	}

	@Test
	public void uuidFilter() throws ParseException {
		Domain domain = getDomain();

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");

		// Userfilter + UUID
		String filter = new LdapUserSearchFilter()
				.getSearchFilterByUuid(LdapParameters.build(domain, Collections.<String, String>emptyMap()), TESTUUID);

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute)));
		assertEquals("(&)", filter.replace("userfilter", "").replace(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute),
				""));

		filter = new LdapUserSearchFilter()
				.getSearchFilterByUuid(LdapParameters.build(domain, Collections.<String, String>emptyMap()), "invalid");
		assertEquals("userfilter", filter);
	}

	@Test
	public void loginFilter() throws ParseException {
		Domain domain = getDomain();

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");

		// Userfilter + UUID
		String filter = new LdapUserSearchFilter()
				.getSearchFilterByName(LdapParameters.build(domain, Collections.<String, String>emptyMap()), "login");

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedLoginFilterPart()));
		assertEquals("(&)", filter.replace("userfilter", "").replace(getExpectedLoginFilterPart(), ""));

		// Invalid user name
		try {
			new LdapUserSearchFilter()
					.getSearchFilterByName(LdapParameters.build(domain, Collections.<String, String>emptyMap()), "");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}

		try {
			new LdapUserSearchFilter().getSearchFilterByName(
					LdapParameters.build(domain, Collections.<String, String>emptyMap()), " invalid");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
		}
	}
}
