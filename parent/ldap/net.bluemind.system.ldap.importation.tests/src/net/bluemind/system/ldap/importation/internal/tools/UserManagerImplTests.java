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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.ImmutableSet;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.RepportStatus;
import net.bluemind.system.importation.search.DirectorySearch;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.search.LdapGroupSearchFilter;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper.DeleteTreeException;
import net.bluemind.user.api.User;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class UserManagerImplTests {
	private ItemValue<Domain> domain;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws LdapException, DeleteTreeException, IOException {
		domain = getDomain();

		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	private ItemValue<Domain> getDomain() {
		return getDomain("memberof.virt", Collections.emptySet());
	}

	private ItemValue<Domain> getDomain(String domainName, Set<String> domainAlias) {
		Domain domain = new Domain();

		domain.name = domainName;
		domain.aliases = domainAlias;

		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(),
				new BmConfIni().get(DockerContainer.LDAP.getName()));
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");

		domain.properties.put(LdapProperties.import_ldap_user_filter.name(),
				LdapProperties.import_ldap_user_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(),
				LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), LdapDockerTestHelper.LDAP_ROOT_DN);

		return ItemValue.create(Item.create(domain.name, 0), domain);
	}

	@Test
	public void userManagerBuild() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain, null).get();
		assertNotNull(userManager);
		assertNotNull(userManager.user);
		assertNotNull(userManager.getUpdatedMailFilter());
		assertTrue(userManager.getUpdatedMailFilter().isPresent());

		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		userManager.update(null, null);
		assertNotNull(userManager);
		assertNotNull(userManager.user);
		assertNotNull(userManager.user.uid);
		assertEquals(LdapUuidMapper
				.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(), testUserEntry).getExtId(),
				userManager.user.externalId);
		assertEquals("user00", userManager.user.value.login);
		assertNotNull(userManager.getUpdatedMailFilter());
		assertTrue(userManager.getUpdatedMailFilter().isPresent());

		ItemValue<User> user = ItemValue.create(Item.create("uid", "extid"), new User());
		user.value.contactInfos = new VCard();
		MailFilter mailFilter = new MailFilter();
		mailFilter.forwarding.emails = new HashSet<>(Arrays.asList("testEmail"));
		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		userManager.update(user, mailFilter);
		assertNotNull(userManager);
		assertNotNull(userManager.user);
		assertNotNull(userManager.getUpdatedMailFilter());
		assertTrue(userManager.getUpdatedMailFilter().isPresent());
		assertEquals(ImmutableSet.builder().add("testEmail").build(),
				userManager.getUpdatedMailFilter().get().forwarding.emails);
	}

	@Test
	public void entryToUserNoOrEmptyLogin() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		testUserEntry.removeAttributes(UserManagerImpl.LDAP_LOGIN);

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();
		try {
			userManager.update(importLogger, null, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().contains(testUserEntry.getDn().toString()));
			assertTrue(sf.getMessage().contains(UserManagerImpl.LDAP_LOGIN));
			assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());
		}

		testUserEntry.add(UserManagerImpl.LDAP_LOGIN, " ");

		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		importLogger = getImportLogger();
		try {
			userManager.update(importLogger, null, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().contains(testUserEntry.getDn().toString()));
			assertTrue(sf.getMessage().contains(UserManagerImpl.LDAP_LOGIN));
			assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());
		}
	}

	@Test
	public void entryToUserNoOrEmptyExtId() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		testUserEntry.removeAttributes(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();
		try {
			userManager.update(importLogger, null, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().contains(testUserEntry.getDn().toString()));
			assertTrue(sf.getMessage().contains(LdapParameters.build(domain.value,
					Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute));
			assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());
		}

		testUserEntry.add(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(), " ");

		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		importLogger = getImportLogger();
		try {
			userManager.update(importLogger, null, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().contains(testUserEntry.getDn().toString()));
			assertTrue(sf.getMessage().contains(LdapParameters.build(domain.value,
					Collections.<String, String>emptyMap()).ldapDirectory.extIdAttribute));
			assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());
		}
	}

	@Test
	public void entryToUser() throws ServerFault, LdapInvalidDnException, LdapException, CursorException, IOException,
			LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		UuidMapper uuidEntry = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				testUserEntry);
		assertEquals(uuidEntry.getExtId(), userManager.user.externalId);
		assertNotNull(userManager.user.uid);

		checkUserUpdated(userManager);
		assertNotNull(userManager.getUpdatedMailFilter());
		assertTrue(userManager.getUpdatedMailFilter().isPresent());
		assertNotNull(userManager.userPhoto);
		assertTrue(userManager.userPhoto.length > 0);

		testUserEntry.removeAttributes("displayName");
		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		userManager.update(importLogger, null, null);
		assertNotNull(userManager.user.value.contactInfos.identification.formatedName);
		assertNull(userManager.user.value.contactInfos.identification.formatedName.value);
	}

	@Test
	public void entryToUserUpdateUser() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ItemValue<User> previousUser = ItemValue.create(Item.create("old", "oldextid"), new User());
		previousUser.value.login = "oldlogin";
		previousUser.value.password = "oldpasswd";
		previousUser.value.archived = true;
		previousUser.value.contactInfos = new VCard();
		previousUser.value.contactInfos.identification.name.givenNames = "oldfirstname";
		previousUser.value.contactInfos.identification.name.familyNames = "oldname";
		previousUser.value.contactInfos.explanatory.note = "olddescription";
		previousUser.value.routing = Routing.none;
		previousUser.value.contactInfos.communications.tels = new LinkedList<>();
		previousUser.value.contactInfos.communications.tels
				.add(Tel.create("9999999", Arrays.asList(Parameter.create("TYPE", "work"),
						Parameter.create("TYPE", "voice"), Parameter.create("TYPE", "X-BM-Ref00"))));

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, previousUser, new MailFilter());
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(previousUser.uid, userManager.user.uid);
		assertEquals(previousUser.externalId, userManager.user.externalId);
		checkUserUpdated(userManager);
		assertNull(userManager.userPhoto);

		assertEquals("hook value", userManager.user.value.dataLocation);
		assertTrue(userManager.getUpdatedMailFilter().isPresent());
		assertNotNull(userManager.getUpdatedMailFilter().get().rules.get(0).discard().orElse(null));
	}

	private void checkUserUpdated(UserManager userManager) throws ServerFault, LdapInvalidAttributeValueException {
		assertNotNull(userManager.user);

		ItemValue<User> user = userManager.user;

		assertEquals("user00", user.value.login);
		assertNull(user.value.password);
		assertFalse(user.value.archived);
		assertEquals("Prenom00 Nom00 displayname", user.value.contactInfos.identification.formatedName.value);
		assertEquals("Prenom00", user.value.contactInfos.identification.name.givenNames);
		assertEquals("Nom00", user.value.contactInfos.identification.name.familyNames);
		assertEquals("Description Prenom00 Nom00", user.value.contactInfos.explanatory.note);
		assertEquals("title", user.value.contactInfos.organizational.title);
		assertEquals("organization", user.value.contactInfos.organizational.org.company);
		assertEquals("division", user.value.contactInfos.organizational.org.division);
		assertEquals("department", user.value.contactInfos.organizational.org.department);

		assertEquals(1, user.value.contactInfos.deliveryAddressing.size());

		DeliveryAddressing address = user.value.contactInfos.deliveryAddressing.get(0);
		assertEquals(1, address.address.parameters.size());
		assertEquals(address.address.parameters.get(0).label, "TYPE");
		assertEquals(address.address.parameters.get(0).value, "work");

		assertEquals("locality", address.address.locality);
		assertEquals("postalcode", address.address.postalCode);
		assertEquals("postofficebox", address.address.postOfficeBox);
		assertEquals("postaladdress1\npostaladdress2", address.address.streetAddress);
		assertEquals("countryname", address.address.countryName);

		assertEquals(Routing.internal, user.value.routing);

		assertEquals(3, user.value.emails.size());
		for (Email email : user.value.emails) {
			switch (email.address) {
			case "user00@memberof.virt":
				continue;
			case "user00.alias00@memberof.virt":
				continue;
			case "user00.alias01@memberof.virt":
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}

		assertEquals(10, user.value.contactInfos.communications.tels.size());
		checkPhones(Arrays.asList("1111", "11111"), ImmutableSet.of("work", "voice"),
				user.value.contactInfos.communications.tels);
		checkPhones(Arrays.asList("2222", "22222"), ImmutableSet.of("home", "voice"),
				user.value.contactInfos.communications.tels);
		checkPhones(Arrays.asList("3333", "33333"), ImmutableSet.of("cell", "voice"),
				user.value.contactInfos.communications.tels);
		checkPhones(Arrays.asList("4444", "44444"), ImmutableSet.of("fax"),
				user.value.contactInfos.communications.tels);
		checkPhones(Arrays.asList("5555", "55555"), ImmutableSet.of("pager"),
				user.value.contactInfos.communications.tels);
	}

	/**
	 * @param asList
	 * @param immutableSet
	 * @param userTels
	 */
	private void checkPhones(List<String> expectedTels, Set<String> parameters, List<Tel> userTels) {
		for (String expectedTel : expectedTels) {
			boolean telFound = false;
			for (Tel userTel : userTels) {
				if (!userTel.value.equals(expectedTel)) {
					continue;
				}
				telFound = true;

				for (String p : parameters) {
					boolean paramFound = false;
					for (Parameter userParameter : userTel.parameters) {
						if (userParameter.value.equals(p)) {
							paramFound = true;
							break;
						}
					}

					assertTrue("Unknown parameter: " + p, paramFound);
				}
			}

			assertTrue(telFound);
		}
	}

	@Test
	public void entryToUserNoFirstname() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertNotNull(userManager.user);
		assertNull(userManager.user.value.contactInfos.identification.name.givenNames);
	}

	@Test
	public void entryToUserNoLastname() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		testUserEntry.removeAttributes("sn");

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();

		userManager.update(importLogger, null, null);
		assertNotNull(userManager.user);
		assertNull(userManager.user.value.contactInfos.identification.name.familyNames);
	}

	@Test
	public void entryToUserNoDescription() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertNotNull(userManager.user);
		assertNull(userManager.user.value.contactInfos.explanatory.note);
	}

	@Test
	public void entryToUserExternalRouting() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "split.relay.tld");

		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl.build(LdapParameters.build(domain.value, settings), domain,
				testUserEntry,
				Optional.of(ImmutableSet.of(LdapUuidMapper
						.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(), testUserEntry))))
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertNotNull(userManager.user);
		assertEquals(Routing.external, userManager.user.value.routing);
	}

	@Test
	public void entryToUserNoEmail() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Routing.none, userManager.user.value.routing);
		assertEquals(0, userManager.user.value.emails.size());
	}

	private ImportLogger getImportLogger() {
		return new ImportLogger(Optional.empty(), Optional.empty(), Optional.of(new RepportStatus()));
	}

	private Entry getTestUserEntry(String userDn) throws ServerFault, LdapException, LdapInvalidDnException,
			CursorException, IOException, LdapSearchException {
		Entry entry = null;
		try (LdapConProxy ldapCon = LdapHelper
				.connectLdap(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()))) {

			PagedSearchResult entries = new DirectorySearch<>(
					LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
					new LdapGroupSearchFilter(), new LdapUserSearchFilter())
					.findByFilterAndBaseDnAndScopeAndAttributes(ldapCon,
							new LdapUserSearchFilter().getSearchFilter(
									LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
									Optional.empty(), null, null),
							new Dn(userDn), SearchScope.OBJECT, "*",
							LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(), "memberof");

			while (entries.next()) {
				Response response = entries.get();

				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entry = ((SearchResultEntry) response).getEntry();
			}
		}

		return entry;
	}

	@Test
	public void entryToUserEmailsDomainAlias() throws ServerFault, LdapInvalidDnException, LdapException,
			CursorException, IOException, LdapSearchException {
		domain.value.aliases = ImmutableSet.of("memberof-alias.virt");

		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Routing.internal, userManager.user.value.routing);
		assertEquals(4, userManager.user.value.emails.size());
		for (Email email : userManager.user.value.emails) {
			switch (email.address) {
			case "user00@memberof.virt":
				assertTrue(email.allAliases);
				continue;
			case "user00.alias00@memberof.virt":
				assertFalse(email.allAliases);
				continue;
			case "user00.alias01@memberof.virt":
				assertFalse(email.allAliases);
				continue;
			case "nodomainpart@memberof.virt":
				assertTrue(email.allAliases);
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}
	}

	@Test
	public void entryToUserMultipleMail() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		domain.value.aliases = ImmutableSet.of("memberof-alias.virt");

		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Routing.internal, userManager.user.value.routing);
		assertEquals(5, userManager.user.value.emails.size());
		for (Email email : userManager.user.value.emails) {
			switch (email.address) {
			case "user00@memberof.virt":
				assertFalse(email.allAliases);
				continue;
			case "user00.alias00@memberof.virt":
				assertFalse(email.allAliases);
				continue;
			case "user00.alias01@memberof-alias.virt":
				assertFalse(email.allAliases);
				continue;
			case "user00.alias02@memberof.virt":
				assertFalse(email.allAliases);
				continue;
			case "nodomainpart@memberof.virt":
				assertTrue(email.allAliases);
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}
	}

	@Test
	public void entryToUserEmailsExternalFirst() throws ServerFault, LdapInvalidDnException, LdapException,
			CursorException, IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Routing.internal, userManager.user.value.routing);
		assertEquals(1, userManager.user.value.emails.size());
		for (Email email : userManager.user.value.emails) {
			switch (email.address) {
			case "user00@memberof.virt":
				assertTrue(email.allAliases);
				assertTrue(email.isDefault);
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}
	}

	@Test
	public void entryToUserEmailsExternalOnly() throws ServerFault, LdapInvalidDnException, LdapException,
			CursorException, IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Routing.none, userManager.user.value.routing);
		assertEquals(1, userManager.user.value.emails.size());
		for (Email email : userManager.user.value.emails) {
			switch (email.address) {
			case "user00@external-domain.virt":
				assertFalse(email.allAliases);
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}
	}

	@Test
	public void entryToUserHook() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);

		assertEquals("hook value", userManager.user.value.dataLocation);
		assertTrue(userManager.getUpdatedMailFilter().isPresent());
		assertNotNull(userManager.getUpdatedMailFilter().get().rules.get(0).discard().orElse(null));
	}

	@Test
	public void entryMailboxQuota() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Integer.valueOf(2), userManager.user.value.quota);

		testUserEntry = getTestUserEntry(
				"uid=user01," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Integer.valueOf(4), userManager.user.value.quota);

		testUserEntry = getTestUserEntry(
				"uid=user02," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(Integer.valueOf(5242880), userManager.user.value.quota);

		testUserEntry = getTestUserEntry(
				"uid=user03," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		userManager = UserManagerImpl.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
				domain, testUserEntry).get();
		importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertNull(userManager.user.value.quota);
	}

	@Test
	public void entryToUserRemoveDescription() throws LdapInvalidDnException, ServerFault, LdapException,
			CursorException, IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ItemValue<User> previousUser = ItemValue.create(Item.create("old", "oldextid"), new User());
		previousUser.value.login = "oldlogin";
		previousUser.value.password = "oldpasswd";
		previousUser.value.contactInfos = new VCard();
		previousUser.value.contactInfos.identification.name.givenNames = "oldfirstname";
		previousUser.value.contactInfos.identification.name.familyNames = "oldname";
		previousUser.value.contactInfos.explanatory.note = "olddescription";

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, previousUser, new MailFilter());
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertNull(previousUser.value.contactInfos.explanatory.note);
	}

	@Test
	public void entryToUserRemoveAddress() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ItemValue<User> previousUser = ItemValue.create(Item.create("old", "oldextid"), new User());
		previousUser.value.login = "oldlogin";
		previousUser.value.password = "oldpasswd";
		previousUser.value.contactInfos = new VCard();
		previousUser.value.contactInfos.identification.name.givenNames = "oldfirstname";
		previousUser.value.contactInfos.identification.name.familyNames = "oldname";
		previousUser.value.contactInfos.explanatory.note = "olddescription";

		DeliveryAddressing oldAddress = new DeliveryAddressing();
		oldAddress.address.parameters = Arrays.asList(Parameter.create("TYPE", "work"));
		oldAddress.address.locality = "locality";
		oldAddress.address.postalCode = "postalcode";
		oldAddress.address.countryName = "countryname";
		oldAddress.address.streetAddress = "streetaddress";
		oldAddress.address.postOfficeBox = "postofficebox";
		previousUser.value.contactInfos.deliveryAddressing = Arrays.asList(oldAddress);

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, previousUser, new MailFilter());
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(0, previousUser.value.contactInfos.deliveryAddressing.size());
	}

	@Test
	public void entryToUserEmailsDomainUidAsAlias() throws LdapInvalidDnException, ServerFault, LdapException,
			CursorException, IOException, LdapSearchException {
		domain = getDomain("domain.internal",
				new HashSet<String>(Arrays.asList("domain.internal", "domain-alias.virt")));

		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, new MailFilter());
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals("user00", userManager.user.value.login);
		assertEquals(1, userManager.user.value.emails.size());
	}

	@Test
	public void entryToUserNoDisplayName() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();

		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, new MailFilter());
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertNotNull(userManager.user.value.contactInfos.identification.formatedName);
		assertNull(userManager.user.value.contactInfos.identification.formatedName.value);
		assertTrue(userManager.user.value.contactInfos.identification.formatedName.parameters.isEmpty());

		ItemValue<User> previousUser = ItemValue.create(Item.create("old", "oldextid"), new User());
		previousUser.value.login = "oldlogin";
		previousUser.value.password = "oldpasswd";
		previousUser.value.contactInfos = new VCard();
		previousUser.value.contactInfos.identification.formatedName = FormatedName.create("Previous formated name");

		importLogger = getImportLogger();
		userManager.update(importLogger, previousUser, new MailFilter());
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertNotNull(previousUser.value.contactInfos.identification.formatedName);
		assertNull(previousUser.value.contactInfos.identification.formatedName.value);
		assertTrue(previousUser.value.contactInfos.identification.formatedName.parameters.isEmpty());
	}

	@Test
	public void entryToUserCertificatePkcs7() throws LdapInvalidDnException, ServerFault, LdapException,
			CursorException, IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(1, userManager.user.value.contactInfos.security.key.parameters.size());
		assertEquals("TYPE", userManager.user.value.contactInfos.security.key.parameters.get(0).label);
		assertEquals("pkcs7", userManager.user.value.contactInfos.security.key.parameters.get(0).value);

		String pkcs7 = "MIIF8gYJKoZIhvcNAQcCoIIF4zCCBd8CAQExADALBgkqhkiG9w0BBwGgggXHMIIF" //
				+ "wzCCA6ugAwIBAgIUVTSFATfec/mVyk95Yu8jhQJjEhcwDQYJKoZIhvcNAQELBQAw" //
				+ "cTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91bG91" //
				+ "c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNVBAMM" //
				+ "EWxkYXBhZC5pbXBvcnQudGxkMB4XDTIzMDMwMzE1MjMxOFoXDTI0MDMwMjE1MjMx" //
				+ "OFowcTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91" //
				+ "bG91c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNV" //
				+ "BAMMEWxkYXBhZC5pbXBvcnQudGxkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC" //
				+ "CgKCAgEA3SqvSmLU+mnqo11RAYExZ2hT61pJ0vBjGSJ+gIOVgve2Vw8QHWgWs3C/" //
				+ "ff8kGiD6F3c/+qzkUpd65ZcOBMwcnPwDk2rGRbchVCrTwjePyGhWxoC7Mi/RlpRT" //
				+ "kc1Q84v0vZ3KthzsCXIMSgRDRnZ4cmwuj90EN+7tb0BS5HRBdeG921OeIK02DJaO" //
				+ "3uqRfC9mnR8Urd1hwqy0nLP7AMOOSE5264+slXPeyeQg5uTwQFkAV2vZCsjEKS7i" //
				+ "d82UCQc2BWp+6sMlCZAFXmU1ue2rzohKbAMmfqQZLX5/rTVY4p4UO+KA8RKaekUR" //
				+ "t0s7iqOJ/7ANILwdmKEYxNBWuXOLJ8rINl7AI61IOY2tX79jGHacZ/h8dkn14RC9" //
				+ "DKn2w1l8iFQc5tl76MDqaq4KFp6jz6BHCbCfcpziMZGFCK9dcvL+QEflck7iAOd1" //
				+ "Gcnj5Az19AxNa4lL+5VXMOblV6SHz2WyxxlxD9RDa9Opr44rpPUOPsfumS5JbTk4" //
				+ "YbwIszi2wFioN+s8EcO/lAh6ysOTcotdxMg3Bp1VBPkpf4UFJpY6rIdSyHhRt/ym" //
				+ "VDx7ohQhfJ1sfSqbNGWVCI+Mk5c4zBXMjPRl05J9jUuz+JOrMVfaAy71ZF6sZKiQ" //
				+ "Lmeo3w4WEnxX6hDtBhbURjTP2AEdqfN1Y8rlvffWmumFKJyLnGcCAwEAAaNTMFEw" //
				+ "HQYDVR0OBBYEFBicOubB3xEds8WI8DPLrSwxm4P+MB8GA1UdIwQYMBaAFBicOubB" //
				+ "3xEds8WI8DPLrSwxm4P+MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQAD" //
				+ "ggIBAAN6mJtKIW2vaRlh9Fwa6g2XIi81YjGO7jti2jotaXFuh0lkxs/IEMfQd+WR" //
				+ "jjoHRJmWV30t5abW8weMFaxUDHAzA9SL5zjlKl5D99F7wC4gy82yOLnhQ1jP5m7X" //
				+ "rqbFEQT/AukLnrbawG1kgwVsp+w7JqdzPnWDBmd36mmUF5ebIF6dtgvN2L7PFtYV" //
				+ "Kr/SEa55D4Gdo8i0Jle5/EmYX0IuxLyUmJiUhX03LexiuAix96TFWLl3lhFgA3Vd" //
				+ "tPVqebHibuGHojnLh59d851TM4CB/EuLBgw1/ZM2Gx3ipccuxSZQeHUHWq6FiGmC" //
				+ "ukw7k5S+XOGVZN5cddhV2b04IKDDIMR18uMuUAa0nLOKouDG+0ml/5dmI/tjtYPl" //
				+ "F5jTLQ8hG7bT3LIoXtnyXG1H7hca6YvhOtrlXxShJRp3/CKin/lzrorcp1u1nEwu" //
				+ "kSFbJJeTVbJ/pU4fZNkfJrFfdVuthCb4TgrpYMXkHmdivWMxdoE0HwQTYxXoDjqS" //
				+ "VYLuFxnjBNw1JTrQn7ak62d9AKkRLC7/kw2WCrFoUptC7/kT50htFOCEcXBVGar9" //
				+ "YeV1M8LWDLmOQMSjSBO2RYKmGKZHZ5XVvEcFQTyvWdOlQ32UB2v/lXHXgdayjcsz" //
				+ "lR/N8xJTZ6ylMgeLA5Jpz8dvGPdk+T0HJiN/zC5jBP8u0qByMQA=";
		assertEquals("-----BEGIN PKCS7-----" + pkcs7 + "-----END PKCS7-----",
				userManager.user.value.contactInfos.security.key.value.replace("\n", ""));
	}

	@Test
	public void entryToUserCertificatePem() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testUserEntry = getTestUserEntry(
				"uid=user00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		UserManager userManager = UserManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testUserEntry)
				.get();
		ImportLogger importLogger = getImportLogger();
		userManager.update(importLogger, null, null);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(1, userManager.user.value.contactInfos.security.key.parameters.size());
		assertEquals("TYPE", userManager.user.value.contactInfos.security.key.parameters.get(0).label);
		assertEquals("pem", userManager.user.value.contactInfos.security.key.parameters.get(0).value);

		String certificate = "MIIFwzCCA6ugAwIBAgIUVTSFATfec/mVyk95Yu8jhQJjEhcwDQYJKoZIhvcNAQELBQAwcTELMAkG" //
				+ "A1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91bG91c2UxETAPBgNVBAoMCEJs" //
				+ "dWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNVBAMMEWxkYXBhZC5pbXBvcnQudGxkMB4XDTIz" //
				+ "MDMwMzE1MjMxOFoXDTI0MDMwMjE1MjMxOFowcTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5j" //
				+ "ZTERMA8GA1UEBwwIVG91bG91c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMx" //
				+ "GjAYBgNVBAMMEWxkYXBhZC5pbXBvcnQudGxkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC" //
				+ "AgEA3SqvSmLU+mnqo11RAYExZ2hT61pJ0vBjGSJ+gIOVgve2Vw8QHWgWs3C/ff8kGiD6F3c/+qzk" //
				+ "Upd65ZcOBMwcnPwDk2rGRbchVCrTwjePyGhWxoC7Mi/RlpRTkc1Q84v0vZ3KthzsCXIMSgRDRnZ4" //
				+ "cmwuj90EN+7tb0BS5HRBdeG921OeIK02DJaO3uqRfC9mnR8Urd1hwqy0nLP7AMOOSE5264+slXPe" //
				+ "yeQg5uTwQFkAV2vZCsjEKS7id82UCQc2BWp+6sMlCZAFXmU1ue2rzohKbAMmfqQZLX5/rTVY4p4U" //
				+ "O+KA8RKaekURt0s7iqOJ/7ANILwdmKEYxNBWuXOLJ8rINl7AI61IOY2tX79jGHacZ/h8dkn14RC9" //
				+ "DKn2w1l8iFQc5tl76MDqaq4KFp6jz6BHCbCfcpziMZGFCK9dcvL+QEflck7iAOd1Gcnj5Az19AxN" //
				+ "a4lL+5VXMOblV6SHz2WyxxlxD9RDa9Opr44rpPUOPsfumS5JbTk4YbwIszi2wFioN+s8EcO/lAh6" //
				+ "ysOTcotdxMg3Bp1VBPkpf4UFJpY6rIdSyHhRt/ymVDx7ohQhfJ1sfSqbNGWVCI+Mk5c4zBXMjPRl" //
				+ "05J9jUuz+JOrMVfaAy71ZF6sZKiQLmeo3w4WEnxX6hDtBhbURjTP2AEdqfN1Y8rlvffWmumFKJyL" //
				+ "nGcCAwEAAaNTMFEwHQYDVR0OBBYEFBicOubB3xEds8WI8DPLrSwxm4P+MB8GA1UdIwQYMBaAFBic" //
				+ "OubB3xEds8WI8DPLrSwxm4P+MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggIBAAN6" //
				+ "mJtKIW2vaRlh9Fwa6g2XIi81YjGO7jti2jotaXFuh0lkxs/IEMfQd+WRjjoHRJmWV30t5abW8weM" //
				+ "FaxUDHAzA9SL5zjlKl5D99F7wC4gy82yOLnhQ1jP5m7XrqbFEQT/AukLnrbawG1kgwVsp+w7Jqdz" //
				+ "PnWDBmd36mmUF5ebIF6dtgvN2L7PFtYVKr/SEa55D4Gdo8i0Jle5/EmYX0IuxLyUmJiUhX03Lexi" //
				+ "uAix96TFWLl3lhFgA3VdtPVqebHibuGHojnLh59d851TM4CB/EuLBgw1/ZM2Gx3ipccuxSZQeHUH" //
				+ "Wq6FiGmCukw7k5S+XOGVZN5cddhV2b04IKDDIMR18uMuUAa0nLOKouDG+0ml/5dmI/tjtYPlF5jT" //
				+ "LQ8hG7bT3LIoXtnyXG1H7hca6YvhOtrlXxShJRp3/CKin/lzrorcp1u1nEwukSFbJJeTVbJ/pU4f" //
				+ "ZNkfJrFfdVuthCb4TgrpYMXkHmdivWMxdoE0HwQTYxXoDjqSVYLuFxnjBNw1JTrQn7ak62d9AKkR" //
				+ "LC7/kw2WCrFoUptC7/kT50htFOCEcXBVGar9YeV1M8LWDLmOQMSjSBO2RYKmGKZHZ5XVvEcFQTyv" //
				+ "WdOlQ32UB2v/lXHXgdayjcszlR/N8xJTZ6ylMgeLA5Jpz8dvGPdk+T0HJiN/zC5jBP8u0qBy";
		assertEquals("-----BEGIN CERTIFICATE-----" + certificate + "-----END CERTIFICATE-----",
				userManager.user.value.contactInfos.security.key.value.replace("\n", ""));
	}
}