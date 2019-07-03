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
package net.bluemind.lib.ldap.tests;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import junit.framework.TestCase;
import net.bluemind.lib.ldap.SidGuidHelper;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;

public class SidGuidHelperTests extends TestCase {
	private static final String VALID_AD_HOSTNAME = new BmConfIni().get(DockerContainer.SAMBA4.getHostProperty());
	private static final String VALID_AD_BASEDN = "dc=bmsamba,dc=virt";

	private static final String VALID_AD_DN = "administrator@bmsamba.virt";
	private static final String VALID_USER_PASSWORD = "bmsamba99_";

	private static final String OBJECT_GUID = "objectGuid";
	private static final String OBJECT_SID = "objectSid";

	public void testGetADObjectGuid() {
		LdapConnection con = getLdapCon();
		assertNotNull(con);

		try {
			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);
			bindRequest.setName(VALID_AD_DN);
			bindRequest.setCredentials(VALID_USER_PASSWORD);

			BindResponse bindResponse = con.bind(bindRequest);

			assertNotNull(bindResponse);
			assertEquals(ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode());
			if (!con.isAuthenticated()) {
				fail("Fail to bind to LDAP server");
			}

			System.out.println("Searching for: " + VALID_AD_BASEDN);
			EntryCursor result = con.search(new Dn(VALID_AD_BASEDN), "(objectClass=*)", SearchScope.OBJECT, OBJECT_GUID,
					"+");
			assertTrue(result.next());
			Entry entry = result.get();

			assertTrue(entry.containsAttribute(OBJECT_GUID));
			System.out.println("Dn: " + VALID_AD_BASEDN + " contain attribute: " + OBJECT_GUID);

			assertFalse(entry.get(OBJECT_GUID).isHumanReadable());

			String guid = SidGuidHelper.convertGuidToString(entry.get(OBJECT_GUID).get().getBytes());
			assertTrue(guid.startsWith("{"));
			assertTrue(guid.endsWith("}"));
			assertTrue(guid.matches("^\\{[a-f0-9-]+\\}$"));
		} catch (Exception e) {
			System.out.println(e);
			fail(e.getMessage());
		}
	}

	public void testGetADObjectSid() {
		LdapConnection con = getLdapCon();
		assertNotNull(con);

		try {
			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);
			bindRequest.setName(VALID_AD_DN);
			bindRequest.setCredentials(VALID_USER_PASSWORD);

			BindResponse bindResponse = con.bind(bindRequest);

			assertNotNull(bindResponse);
			assertEquals(ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode());
			if (!con.isAuthenticated()) {
				fail("Fail to bind to LDAP server");
			}

			System.out.println("Searching for: " + VALID_AD_BASEDN);
			EntryCursor result = con.search(new Dn(VALID_AD_BASEDN), "(objectClass=*)", SearchScope.OBJECT, OBJECT_SID,
					"+");
			assertTrue(result.next());
			Entry entry = result.get();

			assertTrue(entry.containsAttribute(OBJECT_SID));
			System.out.println("Dn: " + VALID_AD_BASEDN + " contain attribute: " + OBJECT_SID);

			assertFalse(entry.get(OBJECT_SID).isHumanReadable());

			assertTrue(SidGuidHelper.convertSidToString(entry.get(OBJECT_SID).get().getBytes()).matches("^S-[0-9-]+$"));
		} catch (Exception e) {
			System.out.println(e);
			fail(e.getMessage());
		}
	}

	private LdapNetworkConnection getLdapCon() {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(VALID_AD_HOSTNAME);
		config.setLdapPort(389);
		ConfigurableBinaryAttributeDetector detector = new DefaultConfigurableBinaryAttributeDetector();
		detector.addBinaryAttribute(OBJECT_GUID);
		detector.addBinaryAttribute(OBJECT_SID);
		config.setBinaryAttributeDetector(detector);

		return new LdapNetworkConnection(config);
	}

	public void testDontFollowSearchRef() {
		LdapNetworkConnection ldapCon = getLdapCon();

		try {
			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);
			bindRequest.setName(VALID_AD_DN);
			bindRequest.setCredentials(VALID_USER_PASSWORD);

			BindResponse bindResponse = ldapCon.bind(bindRequest);

			assertNotNull(bindResponse);
			assertEquals(ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode());
			if (!ldapCon.isAuthenticated()) {
				fail("Fail to bind to LDAP server");
			}

			SearchRequest searchRequest = new SearchRequestImpl();
			searchRequest.setBase(new Dn(VALID_AD_BASEDN));
			searchRequest.setFilter("(&(objectclass=group))");
			searchRequest.setScope(SearchScope.SUBTREE);
			searchRequest.addAttributes("*");
			searchRequest.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);

			SearchCursor cursor = ldapCon.search(searchRequest);

			while (cursor.next()) {
				Response response = cursor.get();

				if (response.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					Entry entry = ((SearchResultEntryDecorator) response).getEntry();
					System.out.println(entry.getDn());
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			fail("Test thrown an exception !");
		}
	}
}
