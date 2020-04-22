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
import java.util.Collections;
import java.util.Optional;

import org.apache.directory.api.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.managers.GroupManager;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.RepportStatus;
import net.bluemind.system.importation.search.DirectorySearch;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.search.LdapGroupSearchFilter;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class GroupManagerTests {
	private ItemValue<Domain> domain;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws Exception {
		domain = getDomain();

		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	private ItemValue<Domain> getDomain() {
		Domain domain = new Domain();

		domain.name = "memberof.virt";

		domain.properties.put(LdapProperties.import_ldap_enabled.name(), "true");
		domain.properties.put(LdapProperties.import_ldap_hostname.name(),
				new BmConfIni().get(DockerContainer.LDAP.getName()));
		domain.properties.put(LdapProperties.import_ldap_protocol.name(), "plain");

		domain.properties.put(LdapProperties.import_ldap_group_filter.name(),
				LdapProperties.import_ldap_group_filter.getDefaultValue());
		domain.properties.put(LdapProperties.import_ldap_ext_id_attribute.name(),
				LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

		domain.properties.put(LdapProperties.import_ldap_base_dn.name(), LdapDockerTestHelper.LDAP_ROOT_DN);

		return ItemValue.create(Item.create(domain.name, 0), domain);
	}

	@Test
	public void entryToGroupNoName() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		testGroupEntry.removeAttributes(GroupManagerImpl.LDAP_NAME);

		ImportLogger importLogger = getImportLogger();
		try {
			GroupManager groupManager = GroupManagerImpl
					.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
							testGroupEntry)
					.get();
			groupManager.update(importLogger, null);
			fail("Test must thrown an excpetion");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
		assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());

		testGroupEntry.add(GroupManagerImpl.LDAP_NAME, "    ");

		importLogger = getImportLogger();
		try {
			GroupManager groupManager = GroupManagerImpl
					.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
							testGroupEntry)
					.get();
			groupManager.update(importLogger, null);
			fail("Test must thrown an excpetion");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
		assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());
	}

	@Test
	public void entryToGroupNoExtId() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));
		testGroupEntry.removeAttributes(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

		ImportLogger importLogger = getImportLogger();
		try {
			GroupManager groupManager = GroupManagerImpl
					.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
							testGroupEntry)
					.get();
			groupManager.update(importLogger, null);

			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
		assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());

		testGroupEntry.add(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(), " ");

		importLogger = getImportLogger();
		try {
			GroupManager groupManager = GroupManagerImpl
					.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
							testGroupEntry)
					.get();
			groupManager.update(importLogger, null);

			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
		assertEquals(JobExitStatus.FAILURE, importLogger.repportStatus.get().getJobStatus());
	}

	@Test
	public void entryToGroup() throws ServerFault, LdapInvalidDnException, LdapException, CursorException, IOException,
			LdapSearchException {
		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		ImportLogger importLogger = getImportLogger();
		GroupManager groupManager = GroupManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testGroupEntry)
				.get();
		assertNotNull(groupManager);

		groupManager.update(importLogger, null);
		assertNotNull(groupManager.group);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		UuidMapper uuidMapper = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				testGroupEntry);
		assertEquals(uuidMapper.getExtId(), groupManager.group.externalId);
		assertNotNull(groupManager.group.uid);

		assertEquals("grptest00", groupManager.group.value.name);
		assertEquals("Test group 00", groupManager.group.value.description);

		assertEquals(3, groupManager.group.value.emails.size());
		for (Email email : groupManager.group.value.emails) {
			switch (email.address) {
			case "grptest00@memberof.virt":
				continue;
			case "grptest00.alias00@memberof.virt":
				continue;
			case "grptest00.alias01@memberof.virt":
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}
	}

	@Test
	public void entryToGroupUpdateUpdate() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		ImportLogger importLogger = getImportLogger();
		GroupManager groupManager = GroupManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testGroupEntry)
				.get();
		assertNotNull(groupManager);

		ItemValue<Group> previousGroup = ItemValue.create(Item.create(null, null), new Group());
		previousGroup.uid = "old";
		previousGroup.externalId = "old";
		previousGroup.value.name = "old";
		previousGroup.value.description = "old";
		Email oldEmail = new Email();
		oldEmail.address = "old@old.old";
		previousGroup.value.emails = ImmutableSet.of(oldEmail);

		groupManager.update(importLogger, previousGroup);
		assertNotNull(groupManager.group);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(previousGroup.externalId, groupManager.group.externalId);
		assertEquals(previousGroup.uid, groupManager.group.uid);

		assertEquals("grptest00", groupManager.group.value.name);
		assertEquals("Test group 00", groupManager.group.value.description);

		assertEquals(3, groupManager.group.value.emails.size());
		for (Email email : groupManager.group.value.emails) {
			switch (email.address) {
			case "grptest00@memberof.virt":
				continue;
			case "grptest00.alias00@memberof.virt":
				continue;
			case "grptest00.alias01@memberof.virt":
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}
	}

	@Test
	public void entryToGroupNoDescription() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		ImportLogger importLogger = getImportLogger();
		GroupManager groupManager = GroupManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testGroupEntry)
				.get();
		assertNotNull(groupManager);

		groupManager.update(importLogger, null);
		assertNotNull(groupManager.group);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		UuidMapper uuidMapper = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				testGroupEntry);
		assertEquals(uuidMapper.getExtId(), groupManager.group.externalId);
		assertNotNull(groupManager.group.uid);

		assertEquals("grptest00", groupManager.group.value.name);
		assertNull(groupManager.group.value.description);

		assertEquals(3, groupManager.group.value.emails.size());
		for (Email email : groupManager.group.value.emails) {
			switch (email.address) {
			case "grptest00@memberof.virt":
				continue;
			case "grptest00.alias00@memberof.virt":
				continue;
			case "grptest00.alias01@memberof.virt":
				continue;
			default:
				fail("Unknown email address: " + email.address);
			}
		}
	}

	@Test
	public void entryToGroupNoEmail() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		ImportLogger importLogger = getImportLogger();
		GroupManager groupManager = GroupManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testGroupEntry)
				.get();
		assertNotNull(groupManager);

		groupManager.update(importLogger, null);
		assertNotNull(groupManager.group);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		UuidMapper uuidMapper = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				testGroupEntry);
		assertEquals(uuidMapper.getExtId(), groupManager.group.externalId);
		assertNotNull(groupManager.group.uid);

		assertEquals("grptest00", groupManager.group.value.name);
		assertEquals("Test group 00", groupManager.group.value.description);

		assertEquals(0, groupManager.group.value.emails.size());
	}

	private ImportLogger getImportLogger() {
		return new ImportLogger(Optional.empty(), Optional.empty(), Optional.of(new RepportStatus()));
	}

	private Entry getTestGroupEntry(String groupDn) throws ServerFault, LdapException, LdapInvalidDnException,
			CursorException, IOException, LdapSearchException {
		Entry entry = null;
		try (LdapConProxy ldapCon = LdapHelper
				.connectLdap(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()))) {
			PagedSearchResult entries = new DirectorySearch<>(
					LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
					new LdapGroupSearchFilter(), new LdapUserSearchFilter()).findByFilterAndBaseDnAndScopeAndAttributes(
							ldapCon,
							new LdapGroupSearchFilter().getSearchFilter(
									LdapParameters.build(domain.value, Collections.<String, String>emptyMap()),
									Optional.empty(), null, null),
							new Dn(groupDn), SearchScope.OBJECT, "*",
							LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

			while (entries.next()) {
				Response response = entries.get();

				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entry = ((SearchResultEntryDecorator) response).getEntry();
			}
		}

		return entry;
	}

	@Test
	public void entryToGroupEmailsDomainAlias() throws ServerFault, LdapInvalidDnException, LdapException,
			CursorException, IOException, LdapSearchException {
		domain.value.aliases = ImmutableSet.of("memberof-alias.virt");

		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		ImportLogger importLogger = getImportLogger();
		GroupManager groupManager = GroupManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testGroupEntry)
				.get();
		assertNotNull(groupManager);

		groupManager.update(importLogger, null);
		assertNotNull(groupManager.group);
		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());

		assertEquals(3, groupManager.group.value.emails.size());
		for (Email email : groupManager.group.value.emails) {
			if (email.address.equals("grptest00@memberof.virt")) {
				assertTrue(email.isDefault);
				assertTrue(email.allAliases);
				continue;
			}

			if (email.address.equals("grptest00.alias01@memberof.virt")) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
				continue;
			}

			if (email.address.equals("grptest00.alias01@memberof.virt")) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
				continue;
			}

			if (email.address.equals("allaliases@memberof.virt")) {
				assertFalse(email.isDefault);
				assertTrue(email.allAliases);
				continue;
			}

			fail("Unknown email address: " + email.address);
		}
	}

	@Test
	public void entryToGroupHook() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		Entry testGroupEntry = getTestGroupEntry(
				"cn=grptest00," + domain.value.properties.get(LdapProperties.import_ldap_base_dn.name()));

		ImportLogger importLogger = getImportLogger();
		GroupManager groupManager = GroupManagerImpl
				.build(LdapParameters.build(domain.value, Collections.<String, String>emptyMap()), domain,
						testGroupEntry)
				.get();
		assertNotNull(groupManager);

		groupManager.update(importLogger, null);

		assertEquals("hook value", groupManager.group.value.dataLocation);
	}
}