/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.externaluser.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.domainbook.verticle.DomainBookVerticle;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.externaluser.service.internal.ExternalUserContainerStoreService;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.api.IRoles;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ExternalUserServiceTests {

	private ExternalUserContainerStoreService externalUserContainerStore;
	private String domainUid;
	private ContainerStore containerStore;
	private ItemValue<Domain> domain;
	private Container domainContainer;
	private SecurityContext securityContext;
	private SecurityContext adminSecurityContext;

	@Before
	public void before() throws Exception {
		DomainBookVerticle.suspended = true;
		domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);

		PopulateHelper.initGlobalVirt();
		domain = PopulateHelper.createTestDomain(domainUid);

		domainContainer = containerStore.get(domainUid);
		assertNotNull(domainContainer);

		securityContext = BmTestContext.contextWithSession("external-user-manager", "test", domainUid,
				BasicRoles.ROLE_MANAGE_EXTERNAL_USER, BasicRoles.ROLE_MANAGE_GROUP).getSecurityContext();
		adminSecurityContext = BmTestContext.contextWithSession("external-user-manager", "test", domainUid,
				BasicRoles.ROLE_MANAGE_EXTERNAL_USER, BasicRoles.ROLE_MANAGE_GROUP, BasicRoles.ROLE_ADMIN)
				.getSecurityContext();

		Container container = containerStore.get(domainUid);
		assertNotNull(container);
		externalUserContainerStore = new ExternalUserContainerStoreService(new BmTestContext(SecurityContext.SYSTEM),
				domain, container);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private IExternalUser getExternalUserService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IExternalUser.class, domainUid);
	}

	private IExternalUser getExternalUserServiceWithoutRole() throws ServerFault {
		return ServerSideServiceProvider.getProvider(
				BmTestContext.contextWithSession("external-user-manager", "test", domainUid).getSecurityContext())
				.instance(IExternalUser.class, domainUid);
	}

	private IGroup getGroupService() {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IGroup.class, domainUid);
	}

	private ExternalUser createDefaultExternalUser() {
		return this.createDefaultExternalUser("user@external.com");
	}

	private ExternalUser createDefaultExternalUser(String address) {
		ExternalUser externalUser = new ExternalUser();
		String firstName = "MyFirstName";
		String name = "MyName";

		externalUser.hidden = true;
		externalUser.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		externalUser.contactInfos = new VCard();
		externalUser.contactInfos.identification.formatedName = VCard.Identification.FormatedName
				.create(firstName + " " + name);
		externalUser.contactInfos.identification.name.familyNames = name;
		externalUser.contactInfos.identification.name.additionalNames = firstName;
		externalUser.contactInfos.communications.emails = new ArrayList<>();
		externalUser.contactInfos.communications.emails.add(VCard.Communications.Email.create(address));
		externalUser.emails = new ArrayList<>();
		externalUser.emails.add(Email.create(address, true));

		return externalUser;
	}

	private void assertExternalUserValueEquals(ExternalUser eu1, ExternalUser eu2) {
		assertEquals(eu1.hidden, eu2.hidden);
		assertEquals(eu1.dataLocation, eu2.dataLocation);
		assertEquals(eu1.defaultEmailAddress(), eu2.defaultEmailAddress());
		assertEquals(eu1.contactInfos.communications.emails.iterator().next().value,
				eu2.contactInfos.communications.emails.iterator().next().value);
		assertEquals(eu1.contactInfos.identification.formatedName.value,
				eu2.contactInfos.identification.formatedName.value);
	}

	private void assertExternalUserItemEquals(ItemValue<ExternalUser> expected, ItemValue<ExternalUser> value) {
		assertEquals(expected.internalId, value.internalId);
		assertEquals(expected.uid, value.uid);
		assertEquals(expected.externalId, value.externalId);
		assertEquals(expected.created, value.created);
		assertEquals(expected.updated, value.updated);
		assertEquals(expected.version, value.version);
	}

	@Test
	public void testRoleManageExternalUserExists() {
		assertTrue(new BmTestContext(SecurityContext.SYSTEM).provider().instance(IRoles.class).getRoles().stream()
				.anyMatch(r -> r.id.equals(BasicRoles.ROLE_MANAGE_EXTERNAL_USER)));
	}

	@Test
	public void testCreateExternalUser() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		getExternalUserService().create(itemUid, eu);
		ItemValue<ExternalUser> created = externalUserContainerStore.get(itemUid);

		assertExternalUserValueEquals(eu, created.value);
		assertNotNull(created.value.contactInfos.identification.formatedName);
		assertNull(created.externalId);
	}

	@Test
	public void createWithExtId() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		getExternalUserService().createWithExtId(itemUid, "externalid", eu);
		ItemValue<ExternalUser> created = externalUserContainerStore.get(itemUid);

		assertExternalUserValueEquals(eu, created.value);
		assertNotNull(created.value.contactInfos.identification.formatedName);

		assertEquals("externalid", created.externalId);
	}

	@Test
	public void testCreateWithItem() throws ParseException {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		ItemValue<ExternalUser> externalUserItem = ItemValue.create(itemUid, eu);
		externalUserItem.internalId = 73;
		externalUserItem.externalId = "external-" + System.currentTimeMillis();
		externalUserItem.displayName = "test";
		externalUserItem.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		externalUserItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		externalUserItem.version = 17;
		getExternalUserService().createWithItem(itemUid, externalUserItem);
		ItemValue<ExternalUser> created = externalUserContainerStore.get(itemUid);

		assertEquals(externalUserItem.externalId, created.externalId);
		assertExternalUserValueEquals(eu, created.value);
		assertExternalUserItemEquals(externalUserItem, created);
	}

	@Test
	public void testCreateExternalUserEmptyEmail() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		eu.emails = new ArrayList<>();
		eu.contactInfos.communications.emails = new ArrayList<>();

		try {
			getExternalUserService().create(itemUid, eu);
			fail("can't create an external user without email.");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateExternalUserNullEmail() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		eu.emails = null;
		eu.contactInfos.communications.emails.clear();

		try {
			getExternalUserService().create(itemUid, eu);
			fail("can't create an external user without email.");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCantCreateExternalUserWhoseRightPartIsDomainAliasAlreadyUsedByAnUser() {
		// adding an alias
		String newAlias = "newalias.tld";
		ServerSideServiceProvider.getProvider(adminSecurityContext).instance(IDomains.class).setAliases(domainUid,
				Sets.newHashSet(newAlias));

		// creating an user with mailbox in all_alias
		String userItemUid = UUID.randomUUID().toString();
		String leftPart = "whatever";
		User user = PopulateHelper.getUser(leftPart, domainUid, Routing.none);
		user.emails.forEach(email -> email.allAliases = true);
		ServerSideServiceProvider.getProvider(adminSecurityContext).instance(IUser.class, domainUid).create(userItemUid,
				user);

		// try to create an external user which uses the same left part as the user and
		// the new alias as right part
		String extUserItemUid = UUID.randomUUID().toString();
		ExternalUser externalUser = createDefaultExternalUser(leftPart + "@" + newAlias);

		try {
			getExternalUserService().create(extUserItemUid, externalUser);
			fail("can't create an external user whose right part is a domain alias already used by an user.");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.EMAIL_ALREADY_USED, sf.getCode());
		}
	}

	@Test
	public void testCantCreateExternalUserWithSameEmailThanExistingExternalUser() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		getExternalUserService().create(itemUid, eu);

		String itemUid2 = UUID.randomUUID().toString();
		ExternalUser eu2 = createDefaultExternalUser();

		try {
			getExternalUserService().create(itemUid2, eu2);
			fail("can't create an external user with same email than an existing external user.");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.EMAIL_ALREADY_USED, sf.getCode());
		}
	}

	@Test
	public void testUpdateExternalUser() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(itemUid, eu);

		String updatedName = "updatedName";
		String updatedMail = "updated@mail.com";
		eu.contactInfos.identification.name = VCard.Identification.Name.create(updatedName, null, null, null, null,
				null);
		eu.contactInfos.identification.formatedName = VCard.Identification.FormatedName.create(updatedName);
		eu.contactInfos.communications.emails.clear();
		eu.contactInfos.communications.emails.add(VCard.Communications.Email.create(updatedMail));
		getExternalUserService().update(itemUid, eu);

		ItemValue<ExternalUser> updated = externalUserContainerStore.get(itemUid);

		assertTrue(updated.value.contactInfos.identification.formatedName.value.contains(updatedName));
		assertEquals(updatedMail, updated.value.defaultEmailAddress());
	}

	@Test
	public void testUpdateWithItem() throws ParseException {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		ItemValue<ExternalUser> externalUserItem = ItemValue.create(itemUid, eu);
		externalUserItem.internalId = 73;
		externalUserItem.externalId = "external-" + System.currentTimeMillis();
		externalUserItem.displayName = "test";
		externalUserItem.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		externalUserItem.version = 17;
		getExternalUserService().createWithItem(itemUid, externalUserItem);
		externalUserItem = externalUserContainerStore.get(itemUid);
		externalUserItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		externalUserItem.version = 23;
		getExternalUserService().updateWithItem(itemUid, externalUserItem);

		ItemValue<ExternalUser> updatedItem = externalUserContainerStore.get(itemUid);

		assertExternalUserValueEquals(eu, updatedItem.value);
		assertExternalUserItemEquals(externalUserItem, updatedItem);
	}

	@Test
	public void testDeleteExternalUser() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(itemUid, eu);
		getExternalUserService().delete(itemUid);

		ItemValue<ExternalUser> found = externalUserContainerStore.get(itemUid);
		assertEquals(null, found);
	}

	@Test
	public void testGetExternalUser() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(itemUid, eu);
		getExternalUserService().getComplete(itemUid);

		ItemValue<ExternalUser> found = externalUserContainerStore.get(itemUid);
		assertNotNull(found);
		assertExternalUserValueEquals(eu, found.value);
	}

	@Test
	public void testCreateExternalUserServiceWithoutRole() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();

		try {
			getExternalUserServiceWithoutRole().create(itemUid, eu);
			fail("can't create external user without the right role.");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testGetExternalUserServiceWithoutRole() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();

		getExternalUserService().create(itemUid, eu);

		try {
			getExternalUserServiceWithoutRole().getComplete(itemUid);
			fail("can't get an external user without the right role.");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDeleteExternalUserServiceWithoutRole() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();

		getExternalUserService().create(itemUid, eu);

		try {
			getExternalUserServiceWithoutRole().delete(itemUid);
			fail("can't delete an external user without the right role.");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testUpdateExternalUserServiceWithoutRole() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();

		getExternalUserService().create(itemUid, eu);

		eu.contactInfos.identification.formatedName = VCard.Identification.FormatedName.create("updatedName");

		try {
			getExternalUserServiceWithoutRole().update(itemUid, eu);
			fail("can't update an external user without the right role.");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testExternalUserVcardIsFilled() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = new ExternalUser();
		String myEmail = "mail@external.com";
		String myName = "myName";
		eu.contactInfos = new VCard();
		eu.contactInfos.identification.name.familyNames = myName;
		eu.contactInfos.communications.emails = new ArrayList<>();
		eu.contactInfos.communications.emails.add(VCard.Communications.Email.create(myEmail));
		eu.hidden = true;
		eu.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		eu.emails = new ArrayList<>();
		eu.emails.add(Email.create(myEmail, true));

		IExternalUser externalUserService = getExternalUserService();
		externalUserService.create(itemUid, eu);
		ExternalUser created = externalUserService.getComplete(itemUid).value;

		assertEquals(1, created.contactInfos.communications.emails.size());
		assertEquals(myEmail, created.contactInfos.defaultMail());
		assertEquals(myName, created.contactInfos.identification.name.familyNames);
		assertEquals(myName, created.contactInfos.identification.formatedName.value);
	}

	private Group createGroup(String name, String desc, String groupUid) {
		Group group = new Group();
		group.name = name;
		group.description = desc;
		getGroupService().create(groupUid, group);
		return group;
	}

	@Test
	public void testMemberOfOneGroup() {
		String euItemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(euItemUid, eu);

		String groupItemUid = UUID.randomUUID().toString();
		Group group = createGroup("OneName", "describe my group !", groupItemUid);

		Member m = Member.externalUser(euItemUid);
		getGroupService().add(groupItemUid, Arrays.asList(m));

		List<ItemValue<Group>> groups = getExternalUserService().memberOf(euItemUid);
		assertEquals(1, groups.size());
		assertEquals(group.name, groups.get(0).value.name);
		assertEquals(group.description, groups.get(0).value.description);
	}

	@Test
	public void testMemberOfMultipleGroups() {
		String euItemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(euItemUid, eu);

		String group1ItemUid = UUID.randomUUID().toString();
		createGroup("OneName", "describe my group !", group1ItemUid);

		String group2ItemUid = UUID.randomUUID().toString();
		createGroup("2ndName", "describe my group !", group2ItemUid);

		String group3ItemUid = UUID.randomUUID().toString();
		createGroup("3rdName", "describe my group !", group3ItemUid);

		Member m = Member.externalUser(euItemUid);
		getGroupService().add(group1ItemUid, Arrays.asList(m));
		getGroupService().add(group2ItemUid, Arrays.asList(m));
		getGroupService().add(group3ItemUid, Arrays.asList(m));

		List<ItemValue<Group>> groups = getExternalUserService().memberOf(euItemUid);
		assertEquals(3, groups.size());
	}

	@Test
	public void testStringMemberOfOneGroup() {
		String euItemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(euItemUid, eu);

		String groupItemUid = UUID.randomUUID().toString();
		createGroup("OneName", "describe my group !", groupItemUid);

		Member m = Member.externalUser(euItemUid);
		getGroupService().add(groupItemUid, Arrays.asList(m));

		List<String> groups = getExternalUserService().memberOfGroups(euItemUid);
		assertEquals(1, groups.size());
		assertEquals(groupItemUid, groups.get(0));
	}

	@Test
	public void testStringMemberOfMultipleGroups() {
		String euItemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(euItemUid, eu);

		String group1ItemUid = UUID.randomUUID().toString();
		createGroup("OneName", "describe my group !", group1ItemUid);

		String group2ItemUid = UUID.randomUUID().toString();
		createGroup("2ndName", "describe my group !", group2ItemUid);

		String group3ItemUid = UUID.randomUUID().toString();
		createGroup("3rdName", "describe my group !", group3ItemUid);

		Member m = Member.externalUser(euItemUid);
		getGroupService().add(group1ItemUid, Arrays.asList(m));
		getGroupService().add(group2ItemUid, Arrays.asList(m));
		getGroupService().add(group3ItemUid, Arrays.asList(m));

		List<String> groups = getExternalUserService().memberOfGroups(euItemUid);
		assertEquals(3, groups.size());
		assertTrue(groups.contains(group1ItemUid));
		assertTrue(groups.contains(group2ItemUid));
		assertTrue(groups.contains(group3ItemUid));
	}

	@Test
	public void testDeleteExternalUserDeleteHisGroupMemberships() throws SQLException {
		// create an external user
		String euItemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		externalUserContainerStore.create(euItemUid, eu);

		// create group
		String groupItemUid = UUID.randomUUID().toString();
		createGroup("OneName", "describe my group !", groupItemUid);

		// external user becomes member of the group
		Member m = Member.externalUser(euItemUid);
		getGroupService().add(groupItemUid, Arrays.asList(m));

		// when external user is deleted
		getExternalUserService().delete(euItemUid);

		// assert all his memberships are removed
		try {
			getExternalUserService().memberOfGroups(euItemUid);
			fail("can't get memberships of a deleted external user");
		} catch (ServerFault sf) {
			assertEquals("Invalid user UID: " + euItemUid, sf.getMessage());
		}
		assertFalse("deleted external user but membership still exists",
				getGroupService().getExpandedMembers(groupItemUid).stream().anyMatch(mem -> mem.uid.equals(euItemUid)));
	}

	@Test
	public void testCreateExternalUserWithEmptyEmailsButVCardEmailsWorks() {
		// create an external user
		String euItemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		eu.emails = null;

		getExternalUserService().create(euItemUid, eu);

		assertEquals(eu.contactInfos.defaultMail(),
				getExternalUserService().getComplete(euItemUid).value.defaultEmailAddress());
	}

	@Test
	public void byExtId() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		getExternalUserService().createWithExtId(itemUid, "externalid", eu);
		ItemValue<ExternalUser> created = getExternalUserService().byExtId("externalid");

		assertExternalUserValueEquals(eu, created.value);
		assertEquals("externalid", created.externalId);
	}

	@Test
	public void byExtId_unknown() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		getExternalUserService().createWithExtId(itemUid, "externalid", eu);
		assertNull(getExternalUserService().byExtId("unknown"));
	}

	@Test
	public void byExtId_nullOrEmptyExtId() {
		String itemUid = UUID.randomUUID().toString();
		ExternalUser eu = createDefaultExternalUser();
		getExternalUserService().createWithExtId(itemUid, "externalid", eu);

		try {
			getExternalUserService().byExtId(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getExternalUserService().byExtId("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}
}
