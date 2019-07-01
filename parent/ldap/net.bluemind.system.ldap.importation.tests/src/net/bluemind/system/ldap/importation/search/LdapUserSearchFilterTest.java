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

import java.text.ParseException;
import java.util.Collections;
import java.util.Optional;

import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.junit.Test;

import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;

public class LdapUserSearchFilterTest {
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

		assertEquals("", new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.empty(), null, null));
	}

	@Test
	public void userFilterOnly() throws ParseException {
		Domain domain = getDomain();

		String filter = new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.empty(), null, null);

		assertEquals(LdapProperties.import_ldap_user_filter.getDefaultValue(), filter);
		FilterParser.parse(filter);
	}

	private String getExpectedLastUpdateFilterPart() {
		return "(" + LdapConstants.MODIFYTIMESTAMP_ATTR + ">=lastupdate)";
	}

	private String getExpectedUuidFilterPart(String uuidAttr) {
		return "(" + uuidAttr + "=uuid)";
	}

	private String getExpectedLoginFilterPart() {
		return "(" + UserManagerImpl.LDAP_LOGIN + "=login)";
	}

	@Test
	public void lastUpdateFilter() throws ParseException {
		Domain domain = getDomain();

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");

		// Userfilter + lastupdate
		String filter = new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.of("lastupdate"), null,
				null);

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedLastUpdateFilterPart()));
		assertEquals("(&)", filter.replace("userfilter", "").replace(getExpectedLastUpdateFilterPart(), ""));

		// Userfilter + lastupdate + UUID
		filter = new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.of("lastupdate"), null,
				"uuid");

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedLastUpdateFilterPart()));
		assertTrue(filter.contains(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute)));
		assertEquals("(&)",
				filter.replace("userfilter", "").replace(getExpectedLastUpdateFilterPart(), "")
						.replace(getExpectedUuidFilterPart(LdapParameters.build(domain,
								Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute), ""));

		// Userfilter + lastupdate + UUID + name
		filter = new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.of("lastupdate"),
				"login", "uuid");

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedLastUpdateFilterPart()));
		assertTrue(filter.contains(getExpectedLoginFilterPart()));
		assertTrue(filter.contains(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute)));
		assertEquals("(&)",
				filter.replace("userfilter", "").replace(getExpectedLastUpdateFilterPart(), "")
						.replace(getExpectedUuidFilterPart(LdapParameters.build(domain,
								Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute), "")
						.replace(getExpectedLoginFilterPart(), ""));
	}

	@Test
	public void uuidFilter() throws ParseException {
		Domain domain = getDomain();

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");

		// Userfilter + UUID
		String filter = new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.empty(), null, "uuid");

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute)));
		assertEquals("(&)", filter.replace("userfilter", "").replace(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute),
				""));

		// Userfilter + UUID + name
		filter = new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.empty(), "login",
				"uuid");

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedLoginFilterPart()));
		assertTrue(filter.contains(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute)));
		assertEquals("(&)", filter.replace("userfilter", "").replace(getExpectedUuidFilterPart(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute), "")
				.replace(getExpectedLoginFilterPart(), ""));
	}

	@Test
	public void loginFilter() throws ParseException {
		Domain domain = getDomain();

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(), "userfilter");

		// Userfilter + UUID
		String filter = new LdapUserSearchFilter().getSearchFilter(
				LdapParameters.build(domain, Collections.<String, String>emptyMap()), Optional.empty(), "login", null);

		FilterParser.parse(filter);
		assertTrue(filter.contains("userfilter"));
		assertTrue(filter.contains(getExpectedLoginFilterPart()));
		assertEquals("(&)", filter.replace("userfilter", "").replace(getExpectedLoginFilterPart(), ""));
	}
}
