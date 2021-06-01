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
package net.bluemind.group.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.addressbook.domainbook.verticle.DomainBookVerticle;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.persistence.GroupStore;
import net.bluemind.group.service.internal.ContainerGroupStoreService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserHelper;
import net.bluemind.user.persistence.UserStore;

public class GroupServiceTests {
	private static final String FAKE_IP = "fake";
	private static final String NOTASSIGNED_IP = "notassigned";

	private ItemValue<User> adminItem;
	private User admin;
	private SecurityContext adminSecurityContext;

	private ItemValue<User> user1Item;
	private User user1;
	private SecurityContext user1SecurityContext;

	private Container userContainer;
	protected Container domainContainer;
	private ContainerStore containerHome;
	protected String domainUid;

	private BmContext testContext;
	private ItemValue<Domain> domain;

	@Before
	public void before() throws Exception {

		DomainBookVerticle.suspended = true;
		domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);

		Server fakeServer = new Server();
		fakeServer.ip = FAKE_IP;
		fakeServer.tags = Lists.newArrayList("fake/tag");

		Server notAssignedServer = new Server();
		notAssignedServer.ip = NOTASSIGNED_IP;
		notAssignedServer.tags = Lists.newArrayList("fake/tag");
		PopulateHelper.initGlobalVirt(fakeServer, notAssignedServer);

		domain = initDomain(containerHome, domainUid, fakeServer);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		PopulateHelper.domainAdmin(domainUid, adminSecurityContext.getSubject());
		domainContainer = containerHome.get(domainUid);
		assertNotNull(domainContainer);

		Container usersBook = containerHome.get("addressbook_" + domainUid);
		assertNotNull(usersBook);

		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(domainContainer,
				Arrays.asList(AccessControlEntry.create(adminSecurityContext.getSubject(), Verb.Write),
						AccessControlEntry.create(user1SecurityContext.getSubject(), Verb.Read)));

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

	}

	private ItemValue<Domain> initDomain(ContainerStore containerHome, String domainUid, Server... servers)
			throws Exception {

		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid, servers);

		userContainer = containerHome.get(UserHelper.getContainerUid(domainUid));
		assertNotNull(userContainer);

		UserStore userStore = new UserStore(JdbcTestHelper.getInstance().getDataSource(), userContainer);
		ContainerStoreService<User> userStoreService = new ContainerStoreService<>(
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM, userContainer, userStore);

		String nt = "" + System.nanoTime();
		String adm = "adm" + nt;
		adminItem = defaultUser(adm, adm);
		admin = adminItem.value;
		userStoreService.create(adminItem.uid, adm, admin);
		adminSecurityContext = BmTestContext.contextWithSession(adm, adm, domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		String u1 = "u1." + nt;
		user1Item = defaultUser(u1, u1);
		user1 = user1Item.value;
		userStoreService.create(user1Item.uid, u1, user1);
		user1SecurityContext = BmTestContext.contextWithSession(u1, u1, domainUid).getSecurityContext();
		return domain;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IGroup getGroupService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IGroup.class, domainUid);
	}

	@Test
	public void testCreateGroupAsAdmin() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		getGroupService(adminSecurityContext).create(uid, group);

		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);
		assertIGroupValueEquals(uid, null, group, createdGroup);

		createdGroup = getGroupService(adminSecurityContext).getComplete(uid);
		assertIGroupValueEquals(uid, null, group, createdGroup);
	}

	@Test
	public void testCreateGroupCheckVCard() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);

		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);
		assertNotNull(dir.findByEntryUid(uid));
		assertNotNull(dir.getVCard(uid));
		uid = UUID.randomUUID().toString();
		group = defaultGroup();
		group.hidden = true;
		getGroupService(adminSecurityContext).create(uid, group);

		assertNotNull(dir.getVCard(uid));
	}

	@Test
	public void testCreateGroupNullName() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.name = null;

		try {
			getGroupService(adminSecurityContext).create(uid, group);
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateGroupEmptyName() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.name = "   ";

		try {
			getGroupService(adminSecurityContext).create(uid, group);
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testUpdateGroupNullName() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		getGroupService(adminSecurityContext).create(uid, group);

		group.name = null;
		try {
			getGroupService(adminSecurityContext).update(uid, group);
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testUpdateGroupEmptyName() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		getGroupService(adminSecurityContext).create(uid, group);

		group.name = "   ";
		try {
			getGroupService(adminSecurityContext).update(uid, group);
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateNullUid() throws ServerFault, InterruptedException, SQLException {
		Group group = defaultGroup();
		try {
			getGroupService(adminSecurityContext).create(null, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateEmptyUid() throws ServerFault, InterruptedException, SQLException {
		Group group = defaultGroup();
		try {
			getGroupService(adminSecurityContext).create("", group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateEmptyExtId() throws ServerFault, InterruptedException, SQLException {
		Group group = defaultGroup();
		try {
			getGroupService(adminSecurityContext).createWithExtId(UUID.randomUUID().toString(), "", group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateNullGroup() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).create(UUID.randomUUID().toString(), null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testUpdateNullUid() throws ServerFault, InterruptedException, SQLException {
		Group group = defaultGroup();
		try {
			getGroupService(adminSecurityContext).update(null, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testUpdateEmptyUid() throws ServerFault, InterruptedException, SQLException {
		Group group = defaultGroup();
		try {
			getGroupService(adminSecurityContext).update("", group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testUpdateNullGroup() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).update(UUID.randomUUID().toString(), null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testGetCompleteNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getComplete(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetCompleteEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getComplete("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testDeleteNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).delete(null);
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testDeleteEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).delete("");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetByExtIdNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getByExtId(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetByExtIdEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getByExtId("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testAddMemberNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).add(null, new ArrayList<Member>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testAddMemberEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).add("", new ArrayList<Member>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testUpdateInexistantGroup() throws ServerFault {
		try {
			getGroupService(adminSecurityContext).update(UUID.randomUUID().toString(), defaultGroup());
			fail("Testmust thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testAddMemberInexistantGroup() throws ServerFault {
		try {
			getGroupService(adminSecurityContext).add(UUID.randomUUID().toString(), new ArrayList<Member>());
			fail("Testmust thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testGetMembersNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getMembers(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetMembersEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getMembers("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetMembersInexistantGroup() throws ServerFault {
		try {
			getGroupService(adminSecurityContext).getExpandedMembers(UUID.randomUUID().toString());
			fail("Testmust thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testGetExpandedUsersMembersNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getExpandedMembers(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetExpandedUsersMembersEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getExpandedMembers("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetExpandedUsersMembersInexistantGroup() throws ServerFault {
		try {
			getGroupService(adminSecurityContext).getMembers(UUID.randomUUID().toString());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testRemoveMembersNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).remove(null, new ArrayList<Member>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testRemoveMembersEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).remove("", new ArrayList<Member>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testRemoveMembersInexistantGroup() throws ServerFault {
		try {
			getGroupService(adminSecurityContext).remove(UUID.randomUUID().toString(), new ArrayList<Member>());
			fail("Testmust thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	private void assertIGroupValueEquals(String uid, String externalId, Group group, ItemValue<Group> createdGroup) {
		assertNotNull(createdGroup);
		assertEquals(uid, createdGroup.uid);
		assertEquals(externalId, createdGroup.externalId);
		assertNotNull(createdGroup.value);
		assertEquals(group.name, createdGroup.value.name);
		assertEquals(group.description, createdGroup.value.description);
		assertEquals(group.hidden, createdGroup.value.hidden);
		assertEquals(group.hiddenMembers, createdGroup.value.hiddenMembers);
		assertEquals(uid, createdGroup.uid);
		assertEquals(group.emails.size(), createdGroup.value.emails.size());
	}

	@Test
	public void testCreateGroupWithExtId() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		String externalId = "external-" + group.name;
		getGroupService(adminSecurityContext).createWithExtId(uid, externalId, group);

		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);
		assertIGroupValueEquals(uid, externalId, group, createdGroup);

		createdGroup = getGroupService(adminSecurityContext).getComplete(uid);
		assertIGroupValueEquals(uid, externalId, group, createdGroup);

		createdGroup = getGroupService(adminSecurityContext).getByExtId(externalId);
		assertIGroupValueEquals(uid, externalId, group, createdGroup);

	}

	@Test
	public void testCreateGroupAsUser() throws ServerFault, InterruptedException, SQLException {
		Group group = defaultGroup();
		String uid = UUID.randomUUID().toString();

		try {
			getGroupService(user1SecurityContext).create(uid, group);
			fail("Test must thrown an exception !");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testUpdateGroupAsAdmin() throws ServerFault, InterruptedException, SQLException {
		Group group = defaultGroup("testUpdateGroupAsAdmin");
		String uid = UUID.randomUUID().toString();
		getGroupService(adminSecurityContext).create(uid, group);

		group.hidden = !group.hidden;
		group.hiddenMembers = !group.hiddenMembers;
		Email e = new Email();
		e.address = group.name + "2@test.foo";
		e.allAliases = true;
		e.isDefault = false;
		group.emails = Arrays.asList(group.emails.iterator().next(), e);
		getGroupService(adminSecurityContext).update(uid, group);

		ItemValue<Group> updatedGroup = getGroupService(adminSecurityContext).getComplete(uid);
		assertIGroupValueEquals(uid, null, group, updatedGroup);
	}

	@Test
	public void testUpdateGroupCheckVCard() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.hiddenMembers = true;
		getGroupService(adminSecurityContext).create(uid, group);

		List<Member> membersToAdd = getMembers(3);
		getGroupService(adminSecurityContext).add(uid, membersToAdd);

		group.name = "checkthat" + System.nanoTime();
		getGroupService(adminSecurityContext).update(uid, group);

		ItemValue<VCard> dirVCard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(dirVCard);
		assertEquals(group.name, dirVCard.value.identification.formatedName.value);

		assertEquals(group.name, dirVCard.value.identification.formatedName.value);
		assertEquals(0, dirVCard.value.organizational.member.size());

		group.hiddenMembers = false;
		getGroupService(adminSecurityContext).update(uid, group);

		dirVCard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(dirVCard);
		assertEquals(3, dirVCard.value.organizational.member.size());
	}

	@Test
	public void testUpdateGroupAsUser() throws ServerFault, InterruptedException, SQLException {
		ItemValue<Group> group = createGroup();

		group.value = defaultGroup();
		group.value.hidden = !group.value.hidden;
		group.value.hiddenMembers = !group.value.hiddenMembers;
		Email e = new Email();
		e.address = group.value.name + "2@test.foo";
		e.allAliases = true;
		e.isDefault = true;
		group.value.emails = Arrays.asList(group.value.emails.iterator().next(), e);

		try {
			getGroupService(user1SecurityContext).update(group.uid, group.value);
			fail("Test must thrown an exception !");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}
	}

	@Test
	public void testDeleteGroupAsAdmin() throws ServerFault, InterruptedException, SQLException {
		ItemValue<Group> group = createGroup();

		TaskRef tr = getGroupService(adminSecurityContext).delete(group.uid);
		waitTaskEnd(tr);

		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(group.uid);
		assertNull(createdGroup);
	}

	@Test
	public void testDeleteGroupAsUser() throws ServerFault, InterruptedException, SQLException {
		try {
			ItemValue<Group> group = createGroup();
			getGroupService(user1SecurityContext).delete(group.uid);
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}
	}

	@Test
	public void testAddMembersAsAdmin() throws ServerFault, SQLException {

		DomainBookVerticle.suspended = true;
		ItemValue<Group> group = createGroup();

		List<Member> membersToAdd = getMembers(3);

		getGroupService(adminSecurityContext).add(group.uid, membersToAdd);

		List<Member> members = getGroupService(adminSecurityContext).getMembers(group.uid);

		assertEquals(ImmutableSet.copyOf(membersToAdd), ImmutableSet.copyOf(members));

		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);

		ItemValue<VCard> vcard = dir.getVCard(group.uid);
		assertNotNull(vcard);
		assertEquals(3, vcard.value.organizational.member.size());
	}

	@Test
	public void testAddMembersAllUsersInGroup() throws ServerFault, SQLException {
		DomainBookVerticle.suspended = true;
		ItemValue<Group> group = createGroup();
		List<Member> membersToAdd = getMembers(3);
		IGroup service = getGroupService(adminSecurityContext);
		service.add(group.uid, membersToAdd);
		List<Member> members = service.getMembers(group.uid);
		assertEquals(ImmutableSet.copyOf(membersToAdd), ImmutableSet.copyOf(members));

		List<Member> membersToReAdd = membersToAdd.subList(0, 1);
		membersToReAdd.addAll(getMembers(1));
		service.add(group.uid, membersToReAdd);

		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);

		ItemValue<VCard> vcard = dir.getVCard(group.uid);
		assertNotNull(vcard);
		// We added one member
		assertEquals(4, vcard.value.organizational.member.size());
	}

	@Test
	public void testAddMembersAlreadyInGroup() throws ServerFault, SQLException {
		DomainBookVerticle.suspended = true;
		ItemValue<Group> group = createGroup();
		List<Member> membersToAdd = getMembers(3);
		getGroupService(adminSecurityContext).add(group.uid, membersToAdd);
		List<Member> members = getGroupService(adminSecurityContext).getMembers(group.uid);
		assertEquals(ImmutableSet.copyOf(membersToAdd), ImmutableSet.copyOf(members));

		try {
			getGroupService(adminSecurityContext).add(group.uid, membersToAdd);
			fail("Should have failed: all users are already in group.");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testAddDeleteMembersShouldKeepVCardEmails() throws ServerFault, SQLException {

		DomainBookVerticle.suspended = true;
		ItemValue<Group> group = createGroup();

		List<Member> membersToAdd = getMembers(3);

		getGroupService(adminSecurityContext).add(group.uid, membersToAdd);

		List<Member> members = getGroupService(adminSecurityContext).getMembers(group.uid);
		int found = 0;
		for (Member memberToAdd : membersToAdd) {
			for (Member member : members) {
				if (memberToAdd.type == member.type && memberToAdd.uid.equals(member.uid)) {
					found++;
				}
			}
		}

		assertEquals(membersToAdd.size(), found);
		IDirectory dirAb = testContext.provider().instance(IDirectory.class, domainUid);
		ItemValue<VCard> vcard = dirAb.getVCard(group.uid);
		assertNotNull(vcard);
		assertEquals(3, vcard.value.organizational.member.size());
		assertEquals(2, vcard.value.communications.emails.size());

		getGroupService(adminSecurityContext).remove(group.uid, members);

		vcard = dirAb.getVCard(group.uid);
		assertNotNull(vcard);
		assertEquals(0, vcard.value.organizational.member.size());
		assertEquals(2, vcard.value.communications.emails.size());
	}

	@Test
	public void testAddMembersDuplicate() throws ServerFault, SQLException {

		DomainBookVerticle.suspended = true;
		ItemValue<Group> group = createGroup();

		List<Member> membersToAdd = getMembers(3);

		getGroupService(adminSecurityContext).add(group.uid, membersToAdd);

		try {
			getGroupService(adminSecurityContext).add(group.uid, membersToAdd.subList(1, 2));
			fail("should fail when duplicate member");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

	}

	@Test
	public void testAddMembersAsUser() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		List<Member> membersToAdd = getMembers(3);
		try {
			getGroupService(user1SecurityContext).add(group.uid, membersToAdd);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

	}

	private Group defaultGroup() {
		return defaultGroup(null);
	}

	private Group defaultGroup(String prefix) {
		Group group = new Group();

		if (prefix == null || prefix.isEmpty()) {
			prefix = "group";
		}
		group.name = prefix + "-" + System.nanoTime();
		group.description = "Test group";

		Email e = new Email();
		e.address = group.name + "@" + domainUid;
		e.allAliases = true;
		e.isDefault = true;
		group.emails = new ArrayList<Email>(1);
		group.emails.add(e);
		group.mailArchived = false;
		group.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		return group;
	}

	private ItemValue<User> defaultUser(String uid, String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.none;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		return ItemValue.create(uid, user);
	}

	private ItemValue<ExternalUser> defaultExternalUser(String uid, String displayName, String email) {
		ExternalUser externalUser = new ExternalUser();
		externalUser.emails = new ArrayList<>();
		externalUser.emails.add(Email.create(email, true));
		VCard card = new VCard();
		card.communications.emails = new ArrayList<>();
		card.communications.emails.add(VCard.Communications.Email.create(externalUser.defaultEmailAddress()));
		card.identification.name = Name.create(displayName, null, null, null, null, null);
		externalUser.contactInfos = card;
		return ItemValue.create(uid, externalUser);
	}

	private List<Member> getMembers(int nb) throws ServerFault, SQLException {
		ArrayList<Member> members = new ArrayList<Member>(nb);

		if (nb != 0) {
			int users = nb / 3;
			int externalUsers = nb / 3;
			int groups = nb - users - externalUsers;

			members.addAll(getUsersMembers(users));
			members.addAll(getGroupsMembers(groups));
			members.addAll(getExternalUsersMembers(externalUsers));
		}

		return members;
	}

	private List<Member> getGroupsMembers(int nb) throws ServerFault, SQLException {
		ArrayList<Member> members = new ArrayList<Member>(nb);
		for (int i = 0; i < nb; i++) {
			ItemValue<Group> group = createGroup();

			Member member = new Member();
			member.type = Member.Type.group;
			member.uid = group.uid;
			members.add(member);
		}

		return members;
	}

	private List<Member> getUsersMembers(int nb) throws ServerFault {

		ArrayList<Member> members = new ArrayList<Member>(nb);
		for (int i = 0; i < nb; i++) {
			ItemValue<User> user = defaultUser();
			if (i == 1) {
				user.value.archived = true;
			}
			testContext.provider().instance(IUser.class, domainUid).create(user.uid, user.value);

			Member member = new Member();
			member.type = Member.Type.user;
			member.uid = user.uid;
			members.add(member);
		}

		return members;
	}

	private List<Member> getExternalUsersMembers(int nb) throws ServerFault {

		ArrayList<Member> members = new ArrayList<Member>(nb);
		for (int i = 0; i < nb; i++) {
			ItemValue<ExternalUser> externalUser = defaultExternalUser();
			testContext.provider().instance(IExternalUser.class, domainUid).create(externalUser.uid,
					externalUser.value);

			Member member = new Member();
			member.type = Member.Type.external_user;
			member.uid = externalUser.uid;
			members.add(member);
		}

		return members;
	}

	private ItemValue<User> defaultUser() {
		long sufix = System.nanoTime();
		return defaultUser(UUID.randomUUID().toString(), "" + sufix);
	}

	private ItemValue<ExternalUser> defaultExternalUser() {
		long sufix = System.nanoTime();
		return defaultExternalUser(UUID.randomUUID().toString(), "" + sufix, "" + sufix + "@mail.com");
	}

	@Test
	public void testRemoveMemberAsAdmin() throws Exception {

		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);

		ItemValue<Group> group = createGroup();

		List<Member> membersToAdd = getMembers(3);
		List<Member> membersToRemove = getMembers(1);

		getGroupService(adminSecurityContext).add(group.uid, membersToAdd);

		getGroupService(adminSecurityContext).add(group.uid, membersToRemove);

		ItemValue<VCard> vcard = dir.getVCard(group.uid);
		assertNotNull(vcard);
		assertEquals(4, vcard.value.organizational.member.size());
		getGroupService(adminSecurityContext).remove(group.uid, membersToRemove);

		List<Member> members = getGroupService(adminSecurityContext).getMembers(group.uid);
		int count = 0;
		for (Member member : members) {
			assertFalse(membersToRemove.get(0).type == member.type && membersToRemove.get(0).uid.equals(member.uid));

			for (Member memberToAdd : membersToAdd) {
				if (memberToAdd.type == member.type && memberToAdd.uid.equals(member.uid)) {
					count++;
				}
			}
		}

		assertEquals(membersToAdd.size(), count);

		vcard = dir.getVCard(group.uid);
		assertNotNull(vcard);
		assertEquals(3, vcard.value.organizational.member.size());

	}

	@Test
	public void testRemoveMemberAsUser() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		List<Member> membersToAdd = getMembers(3);
		List<Member> membersToRemove = getMembers(1);

		getGroupService(adminSecurityContext).add(group.uid, membersToAdd);
		getGroupService(adminSecurityContext).add(group.uid, membersToRemove);

		try {
			getGroupService(user1SecurityContext).remove(group.uid, membersToRemove);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

	}

	@Test
	public void testGetExpandedUsersMembers() throws ServerFault, SQLException {
		ItemValue<Group> group1 = createGroup();

		ItemValue<Group> group2 = createGroup();
		Member g2AsMember = new Member();
		g2AsMember.type = Member.Type.group;
		g2AsMember.uid = group2.uid;

		ItemValue<Group> group3 = createGroup();
		Member g3AsMember = new Member();
		g3AsMember.type = Member.Type.group;
		g3AsMember.uid = group3.uid;

		ItemValue<Group> group4 = createGroup();
		Member g4AsMember = new Member();
		g4AsMember.type = Member.Type.group;
		g4AsMember.uid = group4.uid;

		getGroupService(adminSecurityContext).add(group1.uid, Arrays.asList(g2AsMember, g3AsMember));
		getGroupService(adminSecurityContext).add(group2.uid, Arrays.asList(g4AsMember));
		getGroupService(adminSecurityContext).add(group3.uid, Arrays.asList(g4AsMember));

		List<Member> userMembers = getUsersMembers(3);
		getGroupService(adminSecurityContext).add(group2.uid, Arrays.asList(userMembers.get(0)));
		getGroupService(adminSecurityContext).add(group4.uid, Arrays.asList(userMembers.get(1)));
		getGroupService(adminSecurityContext).add(group3.uid, Arrays.asList(userMembers.get(2)));

		List<Member> users = getGroupService(adminSecurityContext).getExpandedMembers(group1.uid);
		compareMember(userMembers, users);

		users = getGroupService(adminSecurityContext).getExpandedMembers(group1.uid);
		compareMember(userMembers, users);

		getGroupService(adminSecurityContext).remove(group2.uid, Arrays.asList(userMembers.get(0)));
		userMembers.remove(0);

		users = getGroupService(adminSecurityContext).getExpandedMembers(group1.uid);
		compareMember(userMembers, users);
	}

	private ItemValue<Group> createGroup() throws ServerFault, SQLException {
		return createGroup(null);
	}

	private ItemValue<Group> createGroup(String prefix) throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup(prefix);

		try {
			testContext.provider().instance(IGroup.class, domainUid).create(uid, group);
			return testContext.provider().instance(IGroup.class, domainUid).getComplete(uid);
		} catch (Exception e) {
			fail();
			throw new ServerFault(e);
		}

	}

	private void compareMember(List<Member> expected, List<Member> members) {
		int count = 0;

		for (Member exp : expected) {
			for (Member member : members) {
				if (member.type == exp.type && member.uid.equals(exp.uid)) {
					count++;
				}
			}
		}

		assertEquals(expected.size(), count);
	}

	@Test
	public void testAddMembersGroupLoop() throws ServerFault, SQLException {
		ItemValue<Group> group1 = createGroup();
		Member g1AsMember = new Member();
		g1AsMember.type = Member.Type.group;
		g1AsMember.uid = group1.uid;

		ItemValue<Group> group2 = createGroup();
		Member g2AsMember = new Member();
		g2AsMember.type = Member.Type.group;
		g2AsMember.uid = group2.uid;

		getGroupService(adminSecurityContext).add(group1.uid, Arrays.asList(g2AsMember));
		try {
			getGroupService(adminSecurityContext).add(group2.uid, Arrays.asList(g1AsMember));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().toLowerCase().contains("group loop detected"));
		}
	}

	@Test
	public void testAddMeToMyself() throws ServerFault, SQLException {
		ItemValue<Group> group1 = createGroup();
		Member g1AsMember = new Member();
		g1AsMember.type = Member.Type.group;
		g1AsMember.uid = group1.uid;

		try {
			getGroupService(adminSecurityContext).add(group1.uid, Arrays.asList(g1AsMember));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			sf.printStackTrace();
			assertTrue(sf.getMessage().toLowerCase().contains("group loop detected"));
		}
	}

	@Test
	public void testAddMembersToProfileGroup() throws Exception {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup(null);
		group.properties = ImmutableMap.of("is_profile", "true");
		try {
			testContext.provider().instance(IGroup.class, domainUid).create(uid, group);
		} catch (Exception e) {
			fail();
			throw new ServerFault(e);
		}

		String ouAdminUser = PopulateHelper.addUser("testadminou", domainUid, Routing.internal);
		OrgUnit ou = new OrgUnit();
		ou.name = "testOU";
		testContext.provider().instance(IOrgUnits.class, domainUid).create("testOU", ou);
		String testUserUid = PopulateHelper.addUser("testu", domainUid, Routing.internal);
		ItemValue<User> testUserItem = testContext.provider().instance(IUser.class, domainUid).getComplete(testUserUid);
		testUserItem.value.orgUnitUid = "testOU";
		testContext.provider().instance(IUser.class, domainUid).update(testUserUid, testUserItem.value);

		// begin test...

		BmContext testAdmOUContext = BmTestContext.contextWithSession("testAdmOU", ouAdminUser, domainUid);

		try {
			testAdmOUContext.provider().instance(IGroup.class, domainUid).add(uid,
					Arrays.asList(Member.user(testUserUid)));
			fail();
		} catch (ServerFault e) {

		}

		try {

			// with admin role on testOU
			testAdmOUContext = BmTestContext.contextWithSession("testAdmO3U", ouAdminUser, domainUid)
					.withRolesOnOrgUnit("testOU", BasicRoles.ROLE_MANAGE_USER);

			testAdmOUContext.provider().instance(IGroup.class, domainUid).add(uid,
					Arrays.asList(Member.user(testUserUid)));
			fail();
		} catch (ServerFault e) {

		}

		// with admin role on testOU
		testAdmOUContext = BmTestContext.contextWithSession("testAdmO2U", ouAdminUser, domainUid)
				.withRolesOnOrgUnit("testOU", BasicRoles.ROLE_MANAGE_USER).withGroup(uid);

		testAdmOUContext.provider().instance(IGroup.class, domainUid).add(uid, Arrays.asList(Member.user(testUserUid)));
	}

	@Test
	public void testCreateWithSameName() throws ServerFault, SQLException {
		ItemValue<Group> group1 = createGroup();

		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.name = group1.value.name;

		try {
			getGroupService(adminSecurityContext).create(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.ALREADY_EXISTS, sf.getCode());
			assertTrue(sf.getMessage().toLowerCase().contains("already used"));
		}
	}

	@Test
	public void testUpdateWithSameName() throws ServerFault, SQLException {
		ItemValue<Group> group1 = createGroup();
		ItemValue<Group> group2 = createGroup();

		group2.value.name = group1.value.name;

		try {
			getGroupService(adminSecurityContext).update(group2.uid, group2.value);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.ALREADY_EXISTS, sf.getCode());
			assertTrue(sf.getMessage().toLowerCase().contains("already used"));
		}
	}

	@Test
	public void testByEmail() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		ItemValue<Group> created = getGroupService(adminSecurityContext).getComplete(group.uid);

		ItemValue<Group> found = getGroupService(adminSecurityContext)
				.byEmail(created.value.emails.iterator().next().address);
		assertNotNull(found);

		found = getGroupService(adminSecurityContext).byEmail("wtf@email.lan");
		assertNull(found);
	}

	@Test
	public void testByName() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		ItemValue<Group> created = getGroupService(adminSecurityContext).getComplete(group.uid);

		ItemValue<Group> found = getGroupService(adminSecurityContext).byName(created.value.name);
		assertNotNull(found);

		found = getGroupService(adminSecurityContext).byName("wtf");
		assertNull(found);
	}

	@Test
	public void testAddMemberUserFromOtherContainer() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		String fakeDomainUid = System.nanoTime() + ".lan";
		Container otherUserContainer = Container.create(UserHelper.getContainerUid(fakeDomainUid), "users",
				"Other users container", "me", true);
		otherUserContainer = containerHome.create(otherUserContainer);

		UserStore userStore = new UserStore(JdbcTestHelper.getInstance().getDataSource(), otherUserContainer);
		ContainerStoreService<User> userStoreService = new ContainerStoreService<>(
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM, otherUserContainer, userStore);

		ItemValue<User> user = defaultUser();

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.uid;

		userStoreService.create(user.uid, user.value.login, user.value);

		try {
			getGroupService(adminSecurityContext).add(group.uid, Arrays.asList(member));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertEquals("No user with uid " + member.uid + " found", sf.getMessage().replaceAll("[\r\n]", ""));
		}
	}

	@Test
	public void testAddMemberInvalidMember1() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		Member member = new Member();
		member.type = null;
		member.uid = UUID.randomUUID().toString();

		try {
			getGroupService(adminSecurityContext).add(group.uid, Arrays.asList(member));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().toLowerCase().equals("invalid member"));
		}
	}

	@Test
	public void testAddMemberInvalidMember2() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = null;

		try {
			getGroupService(adminSecurityContext).add(group.uid, Arrays.asList(member));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().toLowerCase().equals("invalid member"));
		}
	}

	@Test
	public void testAddMemberInvalidMember3() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = "";

		try {
			getGroupService(adminSecurityContext).add(group.uid, Arrays.asList(member));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			System.out.println(sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().toLowerCase().equals("invalid member"));
		}
	}

	@Test
	public void testAddMemberGroupFromOtherContainer() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		String fakeDomainUid = System.nanoTime() + ".lan";
		Container otherDomainContainer = Container.create(fakeDomainUid, "dir", "Other groups container", "me", true);
		otherDomainContainer = containerHome.create(otherDomainContainer);

		GroupStore groupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), otherDomainContainer);
		ContainerStoreService<Group> groupStoreService = new ContainerStoreService<>(
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM, otherDomainContainer, groupStore);

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = UIDGenerator.uid();

		group.value.emails.iterator().next().address = group.value.name + "@" + fakeDomainUid;
		groupStoreService.create(group.uid, group.value.name, group.value);

		try {
			getGroupService(adminSecurityContext).add(group.uid, Arrays.asList(member));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertEquals("No group with uid " + member.uid + " found", sf.getMessage().replaceAll("[\r\n]", ""));
		}

	}

	@Test
	public void groupWithNoMailbox() throws ServerFault, SQLException {
		Group group = defaultGroup();
		group.emails.clear();
		String uid = UUID.randomUUID().toString();

		ContainerGroupStoreService groupStoreService = new ContainerGroupStoreService(
				new BmTestContext(SecurityContext.SYSTEM), domainContainer, domain);
		groupStoreService.create(uid, group.name, group);

		ItemValue<Group> g = getGroupService(adminSecurityContext).getComplete(uid);
		assertNotNull(g.value.emails);
		assertEquals(0, g.value.emails.size());
	}

	@Test
	public void testCreateWithSameNameDifferentContainer() throws ServerFault, SQLException {
		String fakeDomainUid = System.nanoTime() + ".lan";
		Container otherDomainContainer = Container.create(fakeDomainUid, "dir", "Other groups container", "me", true);
		otherDomainContainer = containerHome.create(otherDomainContainer);

		GroupStore groupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), otherDomainContainer);
		ContainerStoreService<Group> groupStoreService = new ContainerStoreService<>(
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM, otherDomainContainer, groupStore);

		Group group = defaultGroup();
		String uid = UUID.randomUUID().toString();
		groupStoreService.create(uid, group.name, group);

		String nuid = UUID.randomUUID().toString();
		group.emails.iterator().next().address = group.name + "@test.lan";
		getGroupService(adminSecurityContext).create(nuid, group);

		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(nuid);
		assertIGroupValueEquals(nuid, null, group, createdGroup);
	}

	@Test
	public void testCreateGroupsWithSameEmail() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		getGroupService(adminSecurityContext).create(uid, group);

		uid = UUID.randomUUID().toString();
		Group group2 = defaultGroup();
		group2.emails = group.emails;

		try {
			getGroupService(adminSecurityContext).create(uid, group2);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.ALREADY_EXISTS, sf.getCode());
			assertEquals("Following emails of mailbox " + uid + ":_" + group2.name + " are already in use: "
					+ group.emails.iterator().next().address, sf.getMessage());
		}
	}

	@Test
	public void testCreateGroupsWithEmailToSanitize() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		String address = "  sanitize-" + group.emails.iterator().next().address + "   ";
		group.emails.iterator().next().address = address;
		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		assertEquals(address.trim().toLowerCase(), createdGroup.value.emails.iterator().next().address);
	}

	@Test
	public void testCreateGroupsWithInvalidEmail() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		String address = "invalidaddress";
		group.emails.iterator().next().address = address;

		try {
			getGroupService(adminSecurityContext).create(uid, group);
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().toLowerCase().contains("invalid email address")
					&& sf.getMessage().toLowerCase().contains(address));
		}
	}

	@Test
	public void testCreateGroupWithMailArchivedNullDataLocation() {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.dataLocation = null;
		group.mailArchived = true;

		try {
			getGroupService(adminSecurityContext).create(uid, group);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.INVALID_PARAMETER
					&& sf.getMessage().startsWith("Undefined data location server for group:")) {
				fail("datalocation auto-assign fail");
			}
			fail(sf.getMessage());
		}
	}

	@Test
	public void testUpdateGroupWithMailArchivedNullDataLocation() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		group = createdGroup.value;
		group.mailArchived = true;
		group.dataLocation = null;

		try {
			getGroupService(adminSecurityContext).update(uid, group);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.INVALID_PARAMETER
					&& sf.getMessage().startsWith("Undefined data location server for group:")) {
				fail("datalocation auto-assign fail");
			}
			fail(sf.getMessage());
		}
	}

	@Test
	public void testCreateGroupWithMailArchivedEmptyDataLocation() {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.mailArchived = true;
		group.dataLocation = "";

		try {
			getGroupService(adminSecurityContext).create(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Undefined data location server for group:"));
			assertTrue(sf.getMessage().endsWith(group.name));
		}
	}

	@Test
	public void testUpdateGroupWithMailArchivedEmptyDataLocation() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		group = createdGroup.value;
		group.mailArchived = true;
		group.dataLocation = "";

		try {
			getGroupService(adminSecurityContext).update(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Undefined data location server for group:"));
			assertTrue(sf.getMessage().endsWith(group.name));
		}
	}

	@Test
	public void testCreateGroupWithMailArchivedBlankEmptyDataLocation() {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.mailArchived = true;
		group.dataLocation = "   ";

		try {
			getGroupService(adminSecurityContext).create(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Undefined data location server for group:"));
			assertTrue(sf.getMessage().endsWith(group.name));
		}
	}

	@Test
	public void testUpdateGroupWithMailArchivedBlankEmptyDataLocation() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		group = createdGroup.value;
		group.mailArchived = true;
		group.dataLocation = "   ";

		try {
			getGroupService(adminSecurityContext).update(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Undefined data location server for group:"));
			assertTrue(sf.getMessage().endsWith(group.name));
		}
	}

	@Test
	public void testCreateGroupWithMailArchivedInvalidDataLocation() {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.mailArchived = true;
		group.dataLocation = "invalid-server-uid";

		try {
			getGroupService(adminSecurityContext).create(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Server doesn't exist:"));
			assertTrue(sf.getMessage().endsWith(group.dataLocation));
		}
	}

	@Test
	public void testUpdateGroupWithMailArchivedInvalidDataLocation() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		group = createdGroup.value;
		group.mailArchived = true;
		group.dataLocation = "invalid-server-uid";

		try {
			getGroupService(adminSecurityContext).update(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Server doesn't exist:"));
			assertTrue(sf.getMessage().endsWith(group.dataLocation));
		}
	}

	@Test
	public void testCreateGroupWithMailArchivedDataLocationNotImapServer() {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.mailArchived = true;
		group.dataLocation = FAKE_IP;

		try {
			getGroupService(adminSecurityContext).create(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().equals("Server uid: " + group.dataLocation + " not taggued as mail/imap"));
		}
	}

	@Test
	public void testUpdateGroupWithMailArchivedDataLocationNotImapServer() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		group = createdGroup.value;
		group.mailArchived = true;
		group.dataLocation = FAKE_IP;

		try {
			getGroupService(adminSecurityContext).update(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().equals("Server uid: " + group.dataLocation + " not taggued as mail/imap"));
		}
	}

	@Test
	public void testCreateGroupWithMailArchivedDataLocationNotAssigned() {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.mailArchived = true;
		group.dataLocation = NOTASSIGNED_IP;

		try {
			getGroupService(adminSecurityContext).create(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage()
					.equals("Server uid: " + group.dataLocation + " not assigned to domain: " + domainUid));
		}
	}

	@Test
	public void testUpdateGroupWithMailArchivedDataLocationNotAssigned() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		group = createdGroup.value;
		group.mailArchived = true;
		group.dataLocation = NOTASSIGNED_IP;

		try {
			getGroupService(adminSecurityContext).update(uid, group);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage()
					.equals("Server uid: " + group.dataLocation + " not assigned to domain: " + domainUid));
		}
	}

	@Test
	public void testCreateGroupWithMailArchived() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.mailArchived = true;
		group.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		getGroupService(adminSecurityContext).create(uid, group);

		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);
		assertIGroupValueEquals(uid, null, group, createdGroup);
	}

	@Test
	public void testUpdateGroupWithMailArchived() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();

		getGroupService(adminSecurityContext).create(uid, group);
		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(uid);

		group = createdGroup.value;
		group.mailArchived = true;
		group.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		getGroupService(adminSecurityContext).update(uid, group);

		ItemValue<Group> updatedGroup = getGroupService(adminSecurityContext).getComplete(uid);
		assertIGroupValueEquals(uid, null, group, updatedGroup);
	}

	@Test
	public void testUpdateGroupsWithEmailToSanitize() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();
		String address = "  sanitize-" + group.value.emails.iterator().next().address + "   ";
		group.value.emails.iterator().next().address = address;

		getGroupService(adminSecurityContext).update(group.uid, group.value);

		ItemValue<Group> createdGroup = getGroupService(adminSecurityContext).getComplete(group.uid);

		assertEquals(address.trim().toLowerCase(), createdGroup.value.emails.iterator().next().address);
	}

	@Test
	public void testUpdateGroupsWithInvalidEmail() throws ServerFault, SQLException {
		ItemValue<Group> group = createGroup();

		String address = "  sanitize-" + group.value.emails.iterator().next().address + "   ";
		group.value.emails.iterator().next().address = address;

		try {
			getGroupService(adminSecurityContext).update(group.uid, group.value);
			// FIXME doesnt fail ?
			// fail("should fail because email is invalid");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().toLowerCase().contains("invalid email address")
					&& sf.getMessage().toLowerCase().contains(address));
		}
	}

	@Test
	public void testUpdateGroupsWithSameEmail() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		getGroupService(adminSecurityContext).create(uid, group);

		String group2Uid = UUID.randomUUID().toString();
		Group group2 = defaultGroup();
		getGroupService(adminSecurityContext).create(group2Uid, group2);

		group2.emails = group.emails;

		try {
			getGroupService(adminSecurityContext).update(group2Uid, group2);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.ALREADY_EXISTS, sf.getCode());
			assertEquals("Following emails of mailbox " + group2Uid + ":_" + group2.name + " are already in use: "
					+ group.emails.iterator().next().address, sf.getMessage());
		}
	}

	@Test
	public void testGetParentsNullUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getParents(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetParentsEmptyUid() throws ServerFault, InterruptedException, SQLException {
		try {
			getGroupService(adminSecurityContext).getParents("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testGetParentsInexistantGroup() throws ServerFault {
		try {
			getGroupService(adminSecurityContext).getParents(UUID.randomUUID().toString());
			fail("Testmust thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testGetParentsExistantGroup() throws ServerFault, SQLException {
		ItemValue<Group> group1 = createGroup();

		List<ItemValue<Group>> parents = getGroupService(adminSecurityContext).getParents(group1.uid);
		assertNotNull(parents);
		assertEquals(0, parents.size());
	}

	@Test
	public void testGetParents() throws ServerFault, SQLException {
		ItemValue<Group> group1 = createGroup();

		ItemValue<Group> group2 = createGroup();
		Member g2AsMember = new Member();
		g2AsMember.type = Member.Type.group;
		g2AsMember.uid = group2.uid;

		ItemValue<Group> group3 = createGroup();
		Member g3AsMember = new Member();
		g3AsMember.type = Member.Type.group;
		g3AsMember.uid = group3.uid;

		ItemValue<Group> group4 = createGroup();
		Member g4AsMember = new Member();
		g4AsMember.type = Member.Type.group;
		g4AsMember.uid = group4.uid;

		getGroupService(adminSecurityContext).add(group1.uid, Arrays.asList(g2AsMember, g3AsMember));
		getGroupService(adminSecurityContext).add(group2.uid, Arrays.asList(g4AsMember));

		List<ItemValue<Group>> parents = getGroupService(adminSecurityContext).getParents(group4.uid);
		assertNotNull(parents);
		assertEquals(2, parents.size());

		ItemValue<Group> previous = null;
		for (ItemValue<Group> parent : parents) {
			if (previous == null) {
				previous = parent;
			} else {
				assertFalse(previous.uid.equals(parent.uid));
			}

			if (group1.uid.equals(parent.uid)) {
				assertEquals(group1.uid, parent.uid);
				assertNotNull(parent.value);
				assertEquals(group1.value.name, parent.value.name);
				continue;
			}

			if (group2.uid.equals(parent.uid)) {
				assertEquals(group2.uid, parent.uid);
				assertNotNull(parent.value);
				assertEquals(group2.value.name, parent.value.name);
				continue;
			}

			fail("Unknon group UID " + parent.uid);
		}
	}

	@Test
	public void testCustomProperties() throws ServerFault {

		IGroup service = getGroupService(adminSecurityContext);

		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		service.create(uid, group);

		ItemValue<Group> created = service.getComplete(uid);
		assertEquals(0, created.value.properties.size());

		Map<String, String> properties = new HashMap<String, String>();
		group.properties = properties;
		service.update(uid, group);
		created = service.getComplete(uid);
		assertEquals(0, created.value.properties.size());

		properties.put("custom prop", "wat da funk");
		group.properties = properties;
		service.update(uid, group);
		created = service.getComplete(uid);
		assertEquals(1, created.value.properties.size());
		assertEquals("wat da funk", created.value.properties.get("custom prop"));
	}

	@Test
	public void testRoles() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		getGroupService(adminSecurityContext).create(uid, group);

		Set<String> roles = getGroupService(adminSecurityContext).getRoles(uid);
		assertEquals(0, roles.size());

		getGroupService(adminSecurityContext).setRoles(uid,
				new HashSet<>(Arrays.asList(BasicRoles.ROLE_MANAGE_GROUP, BasicRoles.ROLE_MANAGE_USER)));

		roles = getGroupService(adminSecurityContext).getRoles(uid);
		assertEquals(2, roles.size());
		assertTrue(roles.contains(BasicRoles.ROLE_MANAGE_GROUP));
		assertTrue(roles.contains(BasicRoles.ROLE_MANAGE_USER));

		try {
			getGroupService(SecurityContext.ANONYMOUS).setRoles(uid, new HashSet<String>());
			fail("only admin should be able to call setRoles");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		try {
			getGroupService(user1SecurityContext).setRoles(uid, new HashSet<String>());
			fail("only admin should be able to call setRoles");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getGroupService(adminSecurityContext).setRoles(uid,
					new HashSet<>(Arrays.asList(BasicRoles.ROLE_SYSTEM_MANAGER)));
			fail("you cannot delegate roles that you dont have");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getGroupService(adminSecurityContext).setRoles("fakeUid", new HashSet<String>());
			fail("should failed because user does not exist");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testVCardWithArchivedMember() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		getGroupService(adminSecurityContext).create(uid, group);

		List<Member> membersToAdd = getMembers(6);
		assertEquals(6, membersToAdd.size());
		getGroupService(adminSecurityContext).add(uid, membersToAdd);

		ItemValue<VCard> vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);
		assertNotNull(vcard);

		// 1 user, 2 external users, 2 groups ( and 1 archived user)
		assertEquals(5, vcard.value.organizational.member.size());

		// archived user
		ItemValue<User> user = testContext.provider().instance(IUser.class, domainUid)
				.getComplete(membersToAdd.get(1).uid);
		assertNotNull(user);
		user.value.archived = false;
		testContext.provider().instance(IUser.class, domainUid).update(membersToAdd.get(1).uid, user.value);
		// no need to call touch because of the hook
		// getGroupService(adminSecurityContext).touch(group.uid);

		vcard = testContext.provider().instance(IDirectory.class, domainUid).getVCard(uid);

		// 2 user, 2 external users and 2 groups
		assertEquals(6, vcard.value.organizational.member.size());
	}

	@Test
	public void testGroupVCardNote() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup();
		group.description = "this is description";

		getGroupService(adminSecurityContext).create(uid, group);

		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);
		ItemValue<VCard> vcard = dir.getVCard(uid);
		assertEquals(group.description, vcard.value.explanatory.note);

		group.description = "updated description";
		getGroupService(adminSecurityContext).update(uid, group);
		dir = testContext.provider().instance(IDirectory.class, domainUid);
		vcard = dir.getVCard(uid);
		assertEquals(group.description, vcard.value.explanatory.note);
	}

	@Test
	public void testSearchProfileGroups() {
		GroupSearchQuery q = new GroupSearchQuery();
		q.properties.put("is_profile", "true");
		List<ItemValue<Group>> groups = getGroupService(adminSecurityContext).search(q);

		assertEquals(2, groups.size());
		groups.forEach(g -> {
			assertTrue("true".equals(g.value.properties.get("is_profile")));
		});

	}

	private TaskStatus waitTaskEnd(TaskRef taskRef) throws ServerFault {
		TaskStatus status = null;
		while (true) {
			ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class,
					taskRef.id);
			status = task.status();
			if (status.state.ended) {
				break;
			}
		}

		return status;
	}

}
