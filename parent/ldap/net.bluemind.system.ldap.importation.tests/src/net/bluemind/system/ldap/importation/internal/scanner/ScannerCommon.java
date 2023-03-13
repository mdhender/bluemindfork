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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.ldap.importation.internal.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.junit.Test;

import com.google.common.collect.Iterables;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Security;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.RepportStatus;
import net.bluemind.system.importation.search.DirectorySearch;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapUuidMapper;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;
import net.bluemind.system.ldap.importation.search.LdapGroupSearchFilter;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;
import net.bluemind.system.ldap.importation.tests.enhancer.ScannerEnhancerHook;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.user.api.User;

public abstract class ScannerCommon {
	protected abstract Domain getDomain();

	protected abstract void scanLdap(ImportLogger importLogger, CoreServicesTest coreService,
			LdapParameters ldapParameters);

	protected abstract void scanLdap(ImportLogger importLogger, CoreServicesTest coreService,
			LdapParameters ldapParameters, Optional<String> beforeDate);

	protected boolean isMemberUidAttribute() {
		return false;
	}

	protected ImportLogger getImportLogger() {
		return new ImportLogger(Optional.empty(), Optional.empty(), Optional.of(new RepportStatus()));
	}

	@Test
	public void deletedGroups() throws ServerFault {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<Group> g = ItemValue.create(Item.create("1", LdapConstants.EXTID_PREFIX + "doesntexist"),
				new Group());
		coreService.existingGroupsExtIds.add(g.externalId);
		coreService.groups.put(g.uid, g);

		g = ItemValue.create(Item.create("2", "notimportedfromldap"), new Group());
		coreService.existingGroupsExtIds.add(g.externalId);
		coreService.groups.put(g.uid, g);

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.deletedGroupUids.size());
		assertEquals("1", coreService.deletedGroupUids.iterator().next());
	}

	@Test
	public void deletedUser() throws ServerFault {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<User> u = ItemValue.create(Item.create("1", LdapConstants.EXTID_PREFIX + "doesntexist"), new User());
		coreService.existingUsersExtIds.active.add(u.externalId);
		coreService.users.put(u.uid, u);

		u = ItemValue.create(Item.create("2", "notimportedfromldap"), new User());
		coreService.existingUsersExtIds.active.add(u.externalId);
		coreService.users.put(u.uid, u);

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.suspendedUserUids.size());
		assertEquals("1", coreService.suspendedUserUids.iterator().next());
	}

	@Test
	public void createAndUpdateGroups() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();
		ItemValue<Group> existingGroup = getExistingGroup("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(existingGroup.uid, existingGroup);

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(2, coreService.createdGroups.size());
		for (ItemValue<Group> group : coreService.createdGroups.values()) {
			UuidMapper uuid = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
					getExistingGroupEntry("cn=" + group.value.name + "," + LdapDockerTestHelper.LDAP_ROOT_DN));
			assertEquals(uuid.getExtId(), group.externalId);
		}

		assertEquals(1, coreService.updatedGroups.size());
		assertTrue(coreService.updatedGroups.keySet().contains(existingGroup.uid));
	}

	@Test
	public void createAndUpdateGroupsInvalidMember() throws ServerFault, LdapInvalidDnException, LdapException,
			CursorException, IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();
		ItemValue<Group> existingGroup = getExistingGroup("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(existingGroup.uid, existingGroup);

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.createdGroups.size());
		for (ItemValue<Group> group : coreService.createdGroups.values()) {
			UuidMapper uuid = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
					getExistingGroupEntry("cn=" + group.value.name + "," + LdapDockerTestHelper.LDAP_ROOT_DN));
			assertEquals(uuid.getExtId(), group.externalId);
			assertEquals(1, coreService.groupMembersToAdd.get(group.uid).size());
			assertNull(coreService.groupMembersToRemove.get(group.uid));
		}

		assertEquals(1, coreService.updatedGroups.size());
		assertTrue(coreService.updatedGroups.keySet().contains(existingGroup.uid));
		coreService.updatedGroups.values()
				.forEach(group -> assertEquals(1, coreService.groupMembersToAdd.get(group.uid).size()));
		coreService.updatedGroups.values()
				.forEach(group -> assertNull(coreService.groupMembersToRemove.get(group.uid)));
	}

	/**
	 * @return
	 * @throws ServerFault
	 * @throws LdapException
	 * @throws LdapInvalidDnException
	 * @throws CursorException
	 * @throws IOException
	 */
	protected ItemValue<Group> getExistingGroup(String dn) throws ServerFault, LdapInvalidDnException, LdapException,
			CursorException, IOException, LdapSearchException {
		Entry entry = getExistingGroupEntry(dn);

		Group group = new Group();
		group.name = entry.get("cn").getString();
		String extId = entry.get(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue()).getString();
		return ItemValue.create(Item.create(UIDGenerator.uid(), LdapConstants.EXTID_PREFIX + extId), group);
	}

	private Entry getExistingGroupEntry(String dn) throws LdapInvalidDnException, LdapException, IOException,
			ServerFault, CursorException, LdapSearchException {
		Entry entry = null;

		try (LdapConProxy ldapCon = LdapHelper
				.connectLdap(LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()))) {
			PagedSearchResult entries = new DirectorySearch<>(
					LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
					new LdapGroupSearchFilter(), new LdapUserSearchFilter()).findByFilterAndBaseDnAndScopeAndAttributes(
							ldapCon, "(objectclass=*)", new Dn(dn), SearchScope.OBJECT, "*", "+",
							LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

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
	public void createAndUpdateUsers() throws ServerFault, LdapInvalidDnException, LdapException, CursorException,
			IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();
		ItemValue<User> existingUser = getExistingUser("uid=user02," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.users.put(existingUser.uid, existingUser);
		coreService.usersMailfilters.put(existingUser.uid, new MailFilter());

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(2, coreService.createdUsers.size());
		for (ItemValue<User> createdUser : coreService.createdUsers.values()) {
			UuidMapper uuid = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
					getExistingUserEntry("uid=" + createdUser.value.login + "," + LdapDockerTestHelper.LDAP_ROOT_DN));
			assertNotNull(createdUser.uid);
			assertEquals(uuid.getExtId(), createdUser.externalId);
		}

		assertEquals(1, coreService.updatedUsers.size());
		assertTrue(coreService.updatedUsers.keySet().contains(existingUser.uid));

		assertEquals(3, coreService.mailfiltersSet.size());
		for (String uid : Iterables.concat(coreService.createdUsers.keySet(), coreService.updatedUsers.keySet())) {
			assertTrue(coreService.mailfiltersSet.keySet().contains(uid));
		}

		assertEquals(1, coreService.userSetPhoto);
		assertEquals(2, coreService.userDeletePhoto);
	}

	/**
	 * @param string
	 * @return
	 * @throws LdapException
	 * @throws LdapInvalidDnException
	 * @throws ServerFault
	 * @throws CursorException
	 * @throws IOException
	 */
	private ItemValue<User> getExistingUser(String dn) throws LdapInvalidDnException, LdapException, ServerFault,
			CursorException, IOException, LdapSearchException {
		Entry entry = getExistingUserEntry(dn);
		return getExistingUser(entry);
	}

	private ItemValue<User> getExistingUser(Entry entry) throws LdapInvalidAttributeValueException {
		User user = new User();
		user.contactInfos = new VCard();
		user.login = entry.get(UserManagerImpl.LDAP_LOGIN).getString();

		String extId = entry.get(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue()).getString();
		return ItemValue.create(Item.create(UIDGenerator.uid(), LdapConstants.EXTID_PREFIX + extId), user);
	}

	private Entry getExistingUserEntry(String dn) throws IOException, ServerFault, LdapInvalidDnException,
			LdapException, CursorException, LdapSearchException {
		Entry entry = null;

		try (LdapConProxy ldapCon = LdapHelper
				.connectLdap(LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()))) {
			PagedSearchResult entries = new DirectorySearch<>(
					LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
					new LdapGroupSearchFilter(), new LdapUserSearchFilter()).findByFilterAndBaseDnAndScopeAndAttributes(
							ldapCon, "(objectclass=*)", new Dn(dn), SearchScope.OBJECT, "*", "+",
							LdapProperties.import_ldap_ext_id_attribute.getDefaultValue());

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
	public void groupMemberAdd() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();
		ItemValue<User> user00 = getExistingUser("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user00);

		ItemValue<User> user01 = getExistingUser("uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user01);

		ItemValue<Group> group00 = getExistingGroup("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group00.uid, group00);
		ItemValue<Group> group01 = getExistingGroup("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group01.uid, group01);
		ItemValue<Group> group02 = getExistingGroup("cn=grptest02," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group02.uid, group02);

		coreService.addUserToGroups(user00.uid, getGroupMembers(Arrays.asList(group00, group02)));

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(2, coreService.groupMembersToAdd.size());
		for (String groupUid : coreService.groupMembersToAdd.keySet()) {
			if (groupUid.equals(group00.uid)) {
				assertEquals(1, coreService.groupMembersToAdd.get(groupUid).size());

				Member member = coreService.groupMembersToAdd.get(groupUid).get(0);
				assertEquals(Member.Type.user, member.type);
				assertEquals(user01.uid, member.uid);
				continue;
			}

			if (groupUid.equals(group01.uid)) {
				assertEquals(1, coreService.groupMembersToAdd.get(groupUid).size());

				Member member = coreService.groupMembersToAdd.get(groupUid).get(0);
				assertEquals(Member.Type.user, member.type);
				assertEquals(user00.uid, member.uid);
				continue;
			}

			fail("Unknow group uid: " + groupUid);
		}
	}

	@Test
	public void groupMemberRemove() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();
		ItemValue<User> user00 = getExistingUser("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user00);

		ItemValue<User> user01 = getExistingUser("uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user01);

		ItemValue<Group> group00 = getExistingGroup("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group00.uid, group00);
		ItemValue<Group> group01 = getExistingGroup("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group01.uid, group01);
		ItemValue<Group> group02 = getExistingGroup("cn=grptest02," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group02.uid, group02);

		coreService.addUserToGroups(user00.uid, getGroupMembers(Arrays.asList(group00, group01, group02)));
		coreService.addUserToGroups(user01.uid, getGroupMembers(Arrays.asList(group00, group01)));

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(2, coreService.groupMembersToRemove.size());
		for (String groupUid : coreService.groupMembersToRemove.keySet()) {
			if (groupUid.equals(group00.uid) || groupUid.equals(group01.uid)) {
				assertEquals(1, coreService.groupMembersToRemove.get(groupUid).size());

				Member member = coreService.groupMembersToRemove.get(groupUid).get(0);
				assertEquals(Member.Type.user, member.type);
				assertEquals(user00.uid, member.uid);
				continue;
			}

			fail("Unknow group uid: " + groupUid);
		}
	}

	public List<ItemValue<Group>> getGroupMembers(List<ItemValue<Group>> list)
			throws LdapInvalidDnException, ServerFault, LdapException, CursorException, IOException {
		ArrayList<ItemValue<Group>> members = new ArrayList<>();

		for (ItemValue<Group> group : list) {
			members.add(group);
		}

		Group g = new Group();
		g.name = "localgroup";
		members.add(ItemValue.create(Item.create("localgroup", "localgroup"), g));
		g = new Group();
		g.name = "nullextidgroup";
		members.add(ItemValue.create(Item.create("nullextidgroup", null), g));
		g = new Group();
		g.name = "emptyextidgroup";
		members.add(ItemValue.create(Item.create("emptyextidgroup", ""), g));

		return members;
	}

	private void updateEntry(Entry entry) throws IOException, ServerFault, LdapException {
		ModifyRequestImpl modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(entry.getDn());
		modifyRequest.replace("description", "Incremental scan " + new Date().toString());
		try (LdapConProxy ldapCon = LdapHelper
				.connectLdap(LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()))) {
			ModifyResponse mr = ldapCon.modify(modifyRequest);
			assertEquals(ResultCodeEnum.SUCCESS, mr.getLdapResult().getResultCode());
		}
	}

	private String getDate() {
		SimpleDateFormat sdf = new SimpleDateFormat(LdapConstants.GENERALIZED_TIME_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date());
	}

	@Test
	public void incrementalCreate() throws LdapInvalidDnException, ServerFault, LdapException, CursorException,
			LdapSearchException, IOException, InterruptedException {
		CoreServicesTest coreService = new CoreServicesTest();
		Entry user00Entry = getExistingUserEntry("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		ItemValue<User> user00 = getExistingUser(user00Entry);
		UuidMapper user00Uuid = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				user00Entry);
		coreService.addExistingUser(user00);

		Entry user01Entry = getExistingUserEntry("uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		UuidMapper user01Uuid = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				user01Entry);

		Entry group00Entry = getExistingGroupEntry("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		UuidMapper group00Uuid = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				group00Entry);

		Entry group01Entry = getExistingGroupEntry("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		UuidMapper group01Uuid = LdapUuidMapper.fromEntry(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
				group01Entry);

		Thread.sleep(1500);
		String beforeDate = getDate();
		Thread.sleep(1500);

		updateEntry(user00Entry);
		updateEntry(group01Entry);

		ImportLogger importLogger = getImportLogger();

		System.out.println("Scan from: " + beforeDate);
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
				Optional.of(beforeDate));

		assertEquals(1, coreService.createdUsers.size());
		assertEquals(user01Uuid.getExtId(),
				coreService.createdUsers.values().stream().map(u -> u.externalId).findFirst().orElse(null));

		assertEquals(0, coreService.suspendedUserUids.size());

		assertEquals(1, coreService.updatedUsers.size());
		assertTrue(coreService.updatedUsers.keySet().contains(user00.uid));
		assertEquals(user00Uuid.getExtId(), coreService.updatedUsers.get(user00.uid).externalId);

		assertEquals(0, coreService.updatedGroups.size());
		assertEquals(0, coreService.deletedGroupUids.size());

		assertEquals(2, coreService.createdGroups.size());
		assertFalse(coreService.createdGroups.keySet().stream().map(coreService.createdGroups::get)
				.filter(g -> !g.externalId.equals(group00Uuid.getExtId()))
				.filter(g -> !g.externalId.equals(group01Uuid.getExtId())).findFirst().isPresent());
	}

	@Test
	public void incrementalDelete() throws ServerFault, InterruptedException {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<User> u = ItemValue.create(Item.create("1", LdapConstants.EXTID_PREFIX + "userdoesntexist"),
				new User());
		coreService.existingUsersExtIds.active.add(u.externalId);
		coreService.users.put(u.uid, u);

		u = ItemValue.create(Item.create("2", "notimportedfromldap"), new User());
		coreService.existingUsersExtIds.active.add(u.externalId);
		coreService.users.put(u.uid, u);

		ItemValue<Group> g = ItemValue.create(Item.create("3", LdapConstants.EXTID_PREFIX + "groupdoesntexist"),
				new Group());
		coreService.existingGroupsExtIds.add(g.externalId);
		coreService.groups.put(g.uid, g);

		g = ItemValue.create(Item.create("4", "notimportedfromldap"), new Group());
		coreService.existingGroupsExtIds.add(g.externalId);
		coreService.groups.put(g.uid, g);

		Thread.sleep(1500);
		String beforeDate = getDate();
		Thread.sleep(1500);

		ImportLogger importLogger = getImportLogger();

		System.out.println("Scan from: " + beforeDate);
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
				Optional.of(beforeDate));

		assertEquals(1, coreService.suspendedUserUids.size());
		assertEquals("1", coreService.suspendedUserUids.iterator().next());
		assertEquals(1, coreService.deletedGroupUids.size());
		assertEquals("3", coreService.deletedGroupUids.iterator().next());

		assertEquals(0, coreService.createdUsers.size());
		assertEquals(0, coreService.updatedUsers.size());

		assertEquals(0, coreService.createdGroups.size());
		assertEquals(0, coreService.updatedGroups.size());
	}

	@Test
	public void incrementalAddMember() throws LdapInvalidDnException, ServerFault, IOException, LdapException,
			CursorException, LdapSearchException, InterruptedException {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<User> user00Entry = getExistingUser("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user00Entry);

		ItemValue<User> user01Entry = getExistingUser("uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user01Entry);

		Entry group00Entry = getExistingGroupEntry("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		ItemValue<Group> group00 = getExistingGroup(group00Entry.getDn().getName());
		coreService.addExistingGroup(group00);

		Entry group01Entry = getExistingGroupEntry("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		ItemValue<Group> group01 = getExistingGroup(group01Entry.getDn().getName());
		coreService.addExistingGroup(group01);

		Thread.sleep(1500);
		String beforeDate = getDate();
		Thread.sleep(1500);
		updateEntry(group01Entry);

		ImportLogger importLogger = getImportLogger();

		System.out.println("Scan from: " + beforeDate);
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
				Optional.of(beforeDate));

		assertEquals(0, coreService.suspendedUserUids.size());
		assertEquals(0, coreService.updatedUsers.size());

		assertEquals(0, coreService.deletedGroupUids.size());

		assertEquals(0, coreService.createdUsers.size());
		assertEquals(0, coreService.createdGroups.size());
		assertEquals(1, coreService.updatedGroups.size());

		assertEquals(1, coreService.groupMembersToAdd.size());

		assertTrue(coreService.groupMembersToAdd.containsKey(group01.uid));
		assertEquals(2, coreService.groupMembersToAdd.get(group01.uid).size());

		assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());
	}

	@Test
	public void incremental_userInDirectory_notInBm() throws InterruptedException, LdapInvalidDnException, ServerFault,
			LdapException, CursorException, IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<User> user00Entry = getExistingUser("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN);

		ItemValue<User> user01Entry = getExistingUser("uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user01Entry);

		Thread.sleep(1500);
		String beforeDate = getDate();

		ImportLogger importLogger = getImportLogger();

		System.out.println("Scan from: " + beforeDate);
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
				Optional.of(beforeDate));

		assertEquals(0, coreService.suspendedUserUids.size());
		assertEquals(0, coreService.unsuspendedUserUids.size());
		assertEquals(0, coreService.updatedUsers.size());
		assertEquals(1, coreService.createdUsers.size());
		assertTrue(coreService.createdUsers.values().stream().filter(u -> u.externalId.equals(user00Entry.externalId))
				.findFirst().isPresent());
	}

	@Test
	public void incremental_userInDirectory_suspendedInBm() throws InterruptedException, LdapInvalidDnException,
			ServerFault, LdapException, CursorException, IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<User> user00Entry = getExistingUser("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		user00Entry.value.archived = true;
		coreService.addExistingUser(user00Entry);

		ItemValue<User> user01Entry = getExistingUser("uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingUser(user01Entry);

		Thread.sleep(1500);
		String beforeDate = getDate();

		ImportLogger importLogger = getImportLogger();

		System.out.println("Scan from: " + beforeDate);
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
				Optional.of(beforeDate));

		assertEquals(0, coreService.suspendedUserUids.size());
		assertEquals(0, coreService.updatedUsers.size());
		assertEquals(0, coreService.createdUsers.size());
		assertEquals(1, coreService.unsuspendedUserUids.size());
		assertTrue(coreService.unsuspendedUserUids.contains(user00Entry.uid));
	}

	@Test
	public void incremental_groupInDirectory_notInBm() throws InterruptedException, LdapInvalidDnException, ServerFault,
			LdapException, CursorException, IOException, LdapSearchException {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<Group> grp00 = getExistingGroup("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);

		ItemValue<Group> grp01 = getExistingGroup("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.addExistingGroup(grp01);

		Thread.sleep(1500);
		String beforeDate = getDate();

		ImportLogger importLogger = getImportLogger();

		System.out.println("Scan from: " + beforeDate);
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
				Optional.of(beforeDate));

		assertEquals(0, coreService.updatedGroups.size());
		assertEquals(1, coreService.createdGroups.size());
		assertTrue(coreService.createdGroups.values().stream().filter(g -> g.externalId.equals(grp00.externalId))
				.findFirst().isPresent());
	}

	@Test
	public void emailsWithoutDomainPart()
			throws ServerFault, LdapInvalidDnException, LdapException, CursorException, IOException {
		CoreServicesTest coreService = new CoreServicesTest();

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.createdUsers.size());
		for (ItemValue<User> item : coreService.createdUsers.values()) {
			for (Email email : item.value.emails) {
				assertTrue(email.address, email.address.contains("@"));

				if (email.address.startsWith("nodomainpart@")) {
					assertTrue("allAliases must be true", email.allAliases);
					assertTrue(email.address.endsWith("@" + getDomain().name));
				}
			}
		}
	}

	@Test
	public void beforeAndAfterHooks() {
		ScannerEnhancerHook.initFlags();

		CoreServicesTest coreService = new CoreServicesTest();

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()),
				Optional.of("before"));

		assertTrue(ScannerEnhancerHook.before);
		assertTrue(ScannerEnhancerHook.after);
	}

	@Test
	public void splitDomainGroup() {
		CoreServicesTest coreService = new CoreServicesTest();

		Domain domain = getDomain();
		domain.properties.put(LdapProperties.import_ldap_relay_mailbox_group.name(), "splitgroup");

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "test.split.tld");
		domainSettings.put(LdapProperties.import_ldap_group_filter.name(), "(objectclass=bmGroup)");

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(domain, domainSettings));

		assertEquals(2, coreService.createdUsers.size());

		for (ItemValue<User> user : coreService.createdUsers.values()) {
			switch (user.value.login) {
			case "user00":
				assertEquals(Routing.external, user.value.routing);
				break;
			case "user01":
				assertEquals(Routing.internal, user.value.routing);
				break;
			default:
				fail("Unkonw user");
				break;
			}
		}
	}

	@Test
	public void splitDomainGroupMemberUpdate() throws LdapException, InterruptedException {
		// Wait 1s to ensure incremental take care of update only
		Thread.sleep(1000);
		CoreServicesTest coreService = new CoreServicesTest();

		Domain domain = getDomain();
		domain.properties.put(LdapProperties.import_ldap_relay_mailbox_group.name(), "splitgroup");

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "test.split.tld");
		domainSettings.put(LdapProperties.import_ldap_group_filter.name(), "(objectclass=bmGroup)");

		LdapParameters ldapParameter = LdapParameters.build(domain, domainSettings);

		Date lastRun = new Date();
		scanLdap(getImportLogger(), coreService, ldapParameter);
		ldapParameter = ldapParameter.updateLastUpdate(Optional.of(getDateInGeneralizedTimeFormat(lastRun)));

		assertEquals(2, coreService.createdUsers.size());
		assertTrue(coreService.createdUsers.values().stream().allMatch(user -> user.value.routing == Routing.internal));

		ItemValue<User> user00 = coreService.createdUsers.values().stream()
				.filter(user -> user.value.login.equals("user00")).findFirst()
				.orElseThrow(() -> new ServerFault("user00 not found!"));

		assertEquals(1, coreService.createdGroups.size());

		ItemValue<Group> splitgroup = coreService.createdGroups.values().iterator().next();

		LdapDockerTestHelper.getLdapCon()
				.modify(isMemberUidAttribute()
						? new ModifyRequestImpl().setName(new Dn("cn=splitgroup,dc=local")).add("memberUid", "user00")
						: new ModifyRequestImpl().setName(new Dn("cn=splitgroup,dc=local")).add("member",
								"uid=user00,dc=local"));

		lastRun = new Date();
		scanLdap(getImportLogger(), coreService, ldapParameter);
		ldapParameter = ldapParameter.updateLastUpdate(Optional.of(getDateInGeneralizedTimeFormat(lastRun)));

		assertEquals(0, coreService.groupMembersToRemove.size());
		assertEquals(1, coreService.groupMembersToAdd.size());
		assertEquals(splitgroup.uid, coreService.groupMembersToAdd.keySet().iterator().next());
		assertEquals(1, coreService.memberUpdateToExternal.size());
		assertEquals(user00.uid, coreService.memberUpdateToExternal.iterator().next());
		assertEquals(0, coreService.memberUpdateToInternal.size());

		coreService.groupMembersToAdd.clear();
		coreService.memberUpdateToExternal.clear();

		LdapDockerTestHelper.getLdapCon().modify(new ModifyRequestImpl().setName(new Dn("cn=splitgroup,dc=local"))
				.remove(isMemberUidAttribute() ? "memberUid" : "member"));

		lastRun = new Date();
		scanLdap(getImportLogger(), coreService, ldapParameter);
		ldapParameter = ldapParameter.updateLastUpdate(Optional.of(getDateInGeneralizedTimeFormat(lastRun)));

		assertEquals(0, coreService.groupMembersToAdd.size());
		assertEquals(1, coreService.groupMembersToRemove.size());
		assertEquals(splitgroup.uid, coreService.groupMembersToRemove.keySet().iterator().next());
		assertEquals(1, coreService.memberUpdateToInternal.size());
		assertEquals(user00.uid, coreService.memberUpdateToInternal.iterator().next());
		assertEquals(0, coreService.memberUpdateToExternal.size());
	}

	@Test
	public void userCertificatePem() {
		CoreServicesTest coreService = new CoreServicesTest();

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.createdUsers.size());
		Security userSecurity = coreService.createdUsers.entrySet().iterator().next()
				.getValue().value.contactInfos.security;
		assertEquals(1, userSecurity.key.parameters.size());
		assertEquals("MEDIATYPE", userSecurity.key.parameters.get(0).label);
		assertEquals("application/x-pem-file", userSecurity.key.parameters.get(0).value);

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
				userSecurity.key.value.replace("\n", ""));
	}

	@Test
	public void userCertificatePkcs7() {
		CoreServicesTest coreService = new CoreServicesTest();

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.createdUsers.size());
		Security userSecurity = coreService.createdUsers.entrySet().iterator().next()
				.getValue().value.contactInfos.security;
		assertEquals(1, userSecurity.key.parameters.size());
		assertEquals("MEDIATYPE", userSecurity.key.parameters.get(0).label);
		assertEquals("application/pkcs7-mime", userSecurity.key.parameters.get(0).value);

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
		assertEquals("-----BEGIN PKCS7-----" + pkcs7 + "-----END PKCS7-----", userSecurity.key.value.replace("\n", ""));
	}

	private static String getDateInGeneralizedTimeFormat(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'.0Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return sdf.format(date);
	}
}
