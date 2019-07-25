/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.ldap.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroup;
import net.bluemind.system.ldap.export.objects.DomainDirectoryUser;
import net.bluemind.system.ldap.export.verticle.LdapExportVerticle;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class LdapExportServiceTests {
	private static final String LDAPTAG = "directory/bm-master";

	private ItemValue<Domain> domain;
	private ItemValue<Server> ldapRoleServer;

	@Before
	public void before() throws Exception {
		LdapExportVerticle.suspended = true;

		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();

		String domainUid = "test" + System.currentTimeMillis() + ".lan";

		SecurityContext domainAdmin = BmTestContext
				.contextWithSession("testUser", "test", domainUid, SecurityContext.ROLE_ADMIN).getSecurityContext();

		domain = PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.domainAdmin(domainUid, domainAdmin.getSubject());

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		initAndAssignLdapExportServer();
	}

	@Test
	public void testExportService_builder() throws Exception {
		try {
			LdapExportService.build(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			LdapExportService.build("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			LdapExportService.build("invalidUid");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.UNKNOWN, sf.getCode());
		}

		assertNotNull(LdapExportService.build(domain.uid));

		String noLdapExportDomainUid = "test" + System.currentTimeMillis() + ".lan";
		PopulateHelper.createTestDomain(noLdapExportDomainUid);
		assertNull(LdapExportService.build(noLdapExportDomainUid));
	}

	@Test
	public void testExport_syncAll() throws Exception {
		checkBmVersion();

		String userUid = PopulateHelper.addUser(UUID.randomUUID().toString(), domain.value.name);
		String groupUid = addGroup();
		String systemGroupUid = addGroup(true);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		Results results = checkSync();

		assertNotEquals(0, results.userExported.size());
		assertTrue(results.userExported.contains(userUid));

		assertNotEquals(0, results.userNotExported.size());

		assertNotEquals(0, results.groupExported.size());
		assertTrue(results.groupExported.contains(groupUid));

		assertNotEquals(0, results.groupNotExported.size());
		assertTrue(results.groupNotExported.contains(systemGroupUid));
	}

	@Test
	public void testExportUser_hidden() throws Exception {
		checkBmVersion();

		User user = PopulateHelper.getUser("test-" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		user.hidden = true;
		PopulateHelper.addUser(domain.value.name, user);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		checkBmVersion();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapEntry = ldapCon.lookup("uid=" + user.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapEntry);
		assertEquals(1, ldapEntry.get("bmHidden").size());
		assertTrue(ldapEntry.get("bmHidden").contains("true"));
	}

	@Test
	public void testExportGroup_hidden() throws Exception {
		checkBmVersion();

		Group group = getGroup();
		group.hidden = true;
		addGroup(group);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		checkBmVersion();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapEntry = ldapCon.lookup("cn=" + group.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapEntry);
		assertEquals(1, ldapEntry.get("bmHidden").size());
		assertTrue(ldapEntry.get("bmHidden").contains("true"));
	}

	@Test
	public void testExportGroup_removeFromLdapIfDeleted() throws Exception {
		String groupUid = addGroup();

		LdapExportService.build(domain.uid).sync();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		ItemValue<Group> group = groupService.getComplete(groupUid);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);

		TaskRef tr = groupService.delete(group.uid);
		waitFor(tr);

		LdapExportService.build(domain.uid).sync();

		ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapGroup);
	}

	@Test
	public void testExportUser_systemNotExported() throws Exception {
		checkBmVersion();

		User user = PopulateHelper.getUser("test-" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		user.system = true;
		PopulateHelper.addUser(domain.value.name, user);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		checkBmVersion();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapEntry = ldapCon.lookup("uid=" + user.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapEntry);
	}

	@Test
	public void testExport_syncAllWithNoDomainRoot() throws Exception {
		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		LdapHelper.deleteTree(ldapCon, "dc=" + domain.value.name + ",dc=local");

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		checkSync();
	}

	@Test
	public void testExportUser_createIfAbsentOnUpdate() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		LdapHelper.deleteTree(ldapCon, "uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");

		Entry ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapUser);

		userService.update(user.uid, user.value);

		les = LdapExportService.build(domain.uid);
		les.sync();

		ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
	}

	@Test
	public void testExportUser_updateIfPresentOnCreate() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);

		Entry ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(userUid, ldapUser.get("bmuid").getString());
		assertNull(ldapUser.get("description"));

		// Force sync from beginning
		ModifyRequest modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(new Dn("dc=" + domain.value.name + ",dc=local"));
		modifyRequest.replace("bmVersion", "0");
		ldapCon.modify(modifyRequest);

		user.value.contactInfos = new VCard();
		user.value.contactInfos.explanatory.note = "Updated description";
		userService.update(user.uid, user.value);

		LdapExportService.build(domain.uid).sync();

		ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(userUid, ldapUser.get("bmuid").getString());
		assertEquals("Updated description", ldapUser.get("description").getString());
	}

	@Test
	public void testExportGroup_createIfAbsentOnUpdate() throws Exception {
		String groupUid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		ItemValue<Group> group = groupService.getComplete(groupUid);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		LdapHelper.deleteTree(ldapCon, "cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");

		Entry ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapGroup);

		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);
	}

	@Test
	public void testExportUser_resetLdapEntryIfMoreThanOneEntryFound() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);

		LdapExportService.build(domain.uid).sync();

		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);) {
			Entry ldapUser = ldapCon
					.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
			ldapUser.removeAttributes("uid");
			String newLogin = user.value.login + "-2";
			ldapUser.add("uid", newLogin);
			ldapUser.setDn("uid=" + newLogin + ",ou=users,dc=" + domain.value.name + ",dc=local");
			ldapCon.add(ldapUser);
		}

		userService.update(user.uid, user.value);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);

		List<Entry> entries = LdapHelper.getLdapEntryFromUid(ldapCon, domain, userUid);
		assertEquals(1, entries.size());
		assertEquals("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local",
				entries.get(0).getDn().getName());
		assertEquals(userUid, entries.get(0).get("bmUid").getString());
	}

	@Test
	public void testExportGroup_resetLdapEntryIfMoreThanOneEntryFound() throws Exception {
		String groupUid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		ItemValue<Group> group = groupService.getComplete(groupUid);

		LdapExportService.build(domain.uid).sync();

		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);) {
			Entry ldapGroup = ldapCon
					.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
			ldapGroup.removeAttributes("uid");
			String newName = group.value.name + "-2";
			ldapGroup.add("cn", newName);
			ldapGroup.setDn("cn=" + newName + ",ou=groups,dc=" + domain.value.name + ",dc=local");
			ldapCon.add(ldapGroup);
		}

		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);

		List<Entry> entries = LdapHelper.getLdapEntryFromUid(ldapCon, domain, groupUid);
		assertEquals(1, entries.size());
		assertEquals("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local",
				entries.get(0).getDn().getName());
		assertEquals(groupUid, entries.get(0).get("bmUid").getString());
	}

	@Test
	public void testExportUser_manageHiddenAttribute() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(1, ldapUser.get("bmHidden").size());
		assertTrue(ldapUser.get("bmHidden").contains("false"));

		user.value.hidden = true;
		userService.update(user.uid, user.value);

		les.sync();

		ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(1, ldapUser.get("bmHidden").size());
		assertTrue(ldapUser.get("bmHidden").contains("true"));

		user.value.hidden = false;
		userService.update(user.uid, user.value);

		les.sync();

		ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(1, ldapUser.get("bmHidden").size());
		assertTrue(ldapUser.get("bmHidden").contains("false"));
	}

	@Test
	public void testExportGroup_manageHiddenAttribute() throws Exception {
		String groupUid = addGroup();

		LdapExportService.build(domain.uid).sync();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		ItemValue<Group> group = groupService.getComplete(groupUid);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);
		assertEquals(1, ldapGroup.get("bmHidden").size());
		assertTrue(ldapGroup.get("bmHidden").contains("false"));

		group.value.hidden = true;
		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);
		assertEquals(1, ldapGroup.get("bmHidden").size());
		assertTrue(ldapGroup.get("bmHidden").contains("true"));

		group.value.hidden = false;
		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);
		assertEquals(1, ldapGroup.get("bmHidden").size());
		assertTrue(ldapGroup.get("bmHidden").contains("false"));
	}

	@Test
	public void testExportUser_removeFromLdapIfUpdatedToSystem() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);

		user.value.system = true;
		userService.update(user.uid, user.value);

		les.sync();

		ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapUser);
	}

	@Test
	public void testExportUser_removeFromLdapIfDeleted() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);

		TaskRef tr = userService.delete(user.uid);
		waitFor(tr);

		les.sync();

		ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapUser);
	}

	@Test
	public void testExportUser_updated() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);
		user.value.contactInfos.explanatory.note = "description";
		userService.update(user.uid, user.value);

		LdapExportService les = LdapExportService.build(domain.uid);
		les.sync();

		user.value.contactInfos.explanatory.note = "updated description";
		userService.update(user.uid, user.value);

		les.sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapUser = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(userUid, ldapUser.get("bmUid").getString());
		assertEquals("updated description", ldapUser.get("description").getString());
	}

	@Test
	public void testExportGroup_updated() throws Exception {
		String groupUid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		ItemValue<Group> group = groupService.getComplete(groupUid);
		group.value.description = "description";
		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		group.value.description = "updated description";
		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);
		assertEquals(groupUid, ldapGroup.get("bmUid").getString());
		assertEquals("updated description", ldapGroup.get("description").getString());
	}

	@Test
	public void testExportUser_renamed() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.value.name);
		ItemValue<User> user = userService.getComplete(userUid);

		LdapExportService.build(domain.uid).sync();

		String oldLogin = user.value.login;
		user.value.login = "newlogin";
		userService.update(user.uid, user.value);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapUser = ldapCon.lookup("uid=newlogin,ou=users,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(userUid, ldapUser.get("bmUid").getString());

		ldapUser = ldapCon.lookup("uid=" + oldLogin + ",ou=users,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapUser);
	}

	@Test
	public void testExportGroup_renamed() throws Exception {
		String groupUid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		ItemValue<Group> group = groupService.getComplete(groupUid);

		LdapExportService.build(domain.uid).sync();

		String oldName = group.value.name;
		group.value.name = "newlogin";
		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry ldapUser = ldapCon.lookup("cn=newlogin,ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapUser);
		assertEquals(groupUid, ldapUser.get("bmUid").getString());

		ldapUser = ldapCon.lookup("cn=" + oldName + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNull(ldapUser);
	}

	@Test
	public void testExportGroupMember_membersOnCreate() throws Exception {
		String userLogin = "test" + System.nanoTime();
		String userUid = PopulateHelper.addUser(userLogin, domain.value.name, Mailbox.Routing.none);
		String groupUid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(groupUid, Arrays.asList(Member.user(userUid)));

		LdapExportService.build(domain.uid).sync();

		checkUserIsMemberOfGroup(groupUid, userUid, userLogin);
	}

	@Test
	public void testExportGroupMember_addMember() throws Exception {
		String userLogin = "test" + System.nanoTime();
		String userUid = PopulateHelper.addUser(userLogin, domain.value.name, Mailbox.Routing.none);
		String groupUid = addGroup();

		LdapExportService.build(domain.uid).sync();

		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer)) {
			List<Entry> ldapGroup = LdapHelper.getLdapEntryFromUid(ldapCon, domain, groupUid);
			assertEquals(1, ldapGroup.size());

			Attribute memberAttribute = ldapGroup.get(0).get("member");
			assertNull(memberAttribute);

			Attribute memberUidAttribute = ldapGroup.get(0).get("memberUid");
			assertNull(memberUidAttribute);
		}

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(groupUid, Arrays.asList(Member.user(userUid)));

		LdapExportService.build(domain.uid).sync();

		checkUserIsMemberOfGroup(groupUid, userUid, userLogin);
	}

	@Test
	public void testExportGroupMember_removeMember() throws Exception {
		String userLogin = "test" + System.nanoTime();
		String userUid = PopulateHelper.addUser(userLogin, domain.value.name, Mailbox.Routing.none);
		String groupUid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(groupUid, Arrays.asList(Member.user(userUid)));

		LdapExportService.build(domain.uid).sync();

		checkUserIsMemberOfGroup(groupUid, userUid, userLogin);

		groupService.remove(groupUid, Arrays.asList(Member.user(userUid)));

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		List<Entry> ldapGroup = LdapHelper.getLdapEntryFromUid(ldapCon, domain, groupUid);
		assertEquals(1, ldapGroup.size());

		Attribute memberAttribute = ldapGroup.get(0).get("member");
		assertNull(memberAttribute);

		Attribute memberUidAttribute = ldapGroup.get(0).get("memberUid");
		assertNull(memberUidAttribute);
	}

	@Test
	public void testExportGroupMember_createGroupHierarchy() throws Exception {
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);
		String user2Login = "test" + System.nanoTime();
		String user2Uid = PopulateHelper.addUser(user2Login, domain.value.name, Mailbox.Routing.none);
		String group1Uid = addGroup();
		String group2Uid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2Login, user2Uid);
	}

	@Test
	public void testExportGroupMember_removeUserFromGroupChild() throws Exception {
		Group group1 = getGroup();
		String group1Uid = addGroup(group1);
		Group group2 = getGroup();
		String group2Uid = addGroup(group2);
		Group group3 = getGroup();
		String group3Uid = addGroup(group3);
		String user1Uid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		User user2 = PopulateHelper.getUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		String user2Uid = PopulateHelper.addUser(domain.value.name, user2);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));
		groupService.add(group3Uid, Arrays.asList(Member.user(user2Uid)));

		LdapExportService.build(domain.uid).sync();

		groupService.remove(group2Uid, Arrays.asList(Member.user(user2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkUserIsMemberOfGroup(group3Uid, user2Uid, user2.login);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		String user2Dn = new DomainDirectoryUser(domain, ItemValue.create(user2Uid, user2), null).getDn();

		Entry entry = ldapCon.lookup(new DomainDirectoryGroup(domain, ItemValue.create(group1Uid, group1)).getDn());
		Attribute attrs = entry.get("member");
		assertFalse(attrs.contains(user2Dn));
		attrs = entry.get("memberUid");
		assertFalse(attrs.contains(user2.login));

		entry = ldapCon.lookup(new DomainDirectoryGroup(domain, ItemValue.create(group2Uid, group2)).getDn());
		assertNull(entry.get("member"));
		assertNull(entry.get("memberUid"));
	}

	@Test
	public void testExportGroupMember_addUserToGroupChild() throws Exception {
		Group group1 = getGroup();
		String group1Uid = addGroup(group1);
		Group group2 = getGroup();
		String group2Uid = addGroup(group2);
		User user1 = PopulateHelper.getUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		String user1Uid = PopulateHelper.addUser(domain.value.name, user1);
		User user2 = PopulateHelper.getUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		String user2Uid = PopulateHelper.addUser(domain.value.name, user2);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1.login, group2Uid, user2.login, user2Uid);
	}

	@Test
	public void testExportGroupWithExternalUserNotTaken() throws Exception {
		// create a user
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);

		// create a group
		String groupUid = addGroup();

		// create an external user
		String externalUser1Uid = PopulateHelper.addExternalUser(domain.value.name, "external@user.com", "displayName");

		// add all members to group
		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(groupUid, Arrays.asList(Member.user(user1Uid), Member.externalUser(externalUser1Uid)));

		LdapExportService.build(domain.uid).sync();

		checkUidIsNotMemberOfGroup(groupUid, externalUser1Uid);
		checkUserIsMemberOfGroup(groupUid, user1Uid, user1Login);
	}

	@Test
	public void testExportGroupMember_addGroupMember() throws Exception {
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);
		String user2Login = "test" + System.nanoTime();
		String user2Uid = PopulateHelper.addUser(user2Login, domain.value.name, Mailbox.Routing.none);
		String group1Uid = addGroup();
		String group2Uid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkUserIsMemberOfGroup(group1Uid, user1Uid, user1Login);
		checkUserIsMemberOfGroup(group2Uid, user2Uid, user2Login);

		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2Login, user2Uid);
	}

	@Test
	public void testExportGroupMember_removeGroupMember() throws Exception {
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);
		String user2Login = "test" + System.nanoTime();
		String user2Uid = PopulateHelper.addUser(user2Login, domain.value.name, Mailbox.Routing.none);
		String group1Uid = addGroup();
		String group2Uid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2Login, user2Uid);

		groupService.remove(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkUserIsMemberOfGroup(group1Uid, user1Uid, user1Login);
		checkUserIsMemberOfGroup(group2Uid, user2Uid, user2Login);
	}

	@Test
	public void testExportGroupMember_renameGroupMember() throws Exception {
		String group1Uid = addGroup();
		Group group2 = getGroup();
		String group2Uid = addGroup(group2);
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);
		String user2Login = "test" + System.nanoTime();
		String user2Uid = PopulateHelper.addUser(user2Login, domain.value.name, Mailbox.Routing.none);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2Login, user2Uid);

		group2.name = group2.name + "-new";
		groupService.update(group2Uid, group2);

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2Login, user2Uid);
	}

	@Test
	public void testExportGroupMember_renameUserMember() throws Exception {
		String group1Uid = addGroup();
		Group group2 = getGroup();
		String group2Uid = addGroup(group2);
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);
		User user2 = PopulateHelper.getUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		String user2Uid = PopulateHelper.addUser(domain.value.name, user2);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2.login, user2Uid);

		user2.login = user2.login + "-new";
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain.value.name)
				.update(user2Uid, user2);

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2.login, user2Uid);
	}

	@Test
	public void testExportGroupMember_deleteUserMemberFromChild() throws Exception {
		Group group1 = getGroup();
		String group1Uid = addGroup(group1);
		Group group2 = getGroup();
		String group2Uid = addGroup(group2);
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);
		User user2 = PopulateHelper.getUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		String user2Uid = PopulateHelper.addUser(domain.value.name, user2);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2.login, user2Uid);

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domain.value.name).delete(user2Uid);
		waitFor(tr);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		String user2Dn = new DomainDirectoryUser(domain, ItemValue.create(user2Uid, user2), null).getDn();

		Entry entry = ldapCon.lookup(new DomainDirectoryGroup(domain, ItemValue.create(group1Uid, group1)).getDn());
		Attribute attrs = entry.get("member");
		assertFalse(attrs.contains(user2Dn));
		attrs = entry.get("memberUid");
		assertFalse(attrs.contains(user2.login));

		entry = ldapCon.lookup(new DomainDirectoryGroup(domain, ItemValue.create(group2Uid, group2)).getDn());
		assertNull(entry.get("member"));
		assertNull(entry.get("memberUid"));
	}

	@Test
	public void testExportGroupMember_deleteGroupMemberFromChild() throws Exception {
		Group group1 = getGroup();
		String group1Uid = addGroup(group1);
		Group group2 = getGroup();
		String group2Uid = addGroup(group2);
		String user1Login = "test" + System.nanoTime();
		String user1Uid = PopulateHelper.addUser(user1Login, domain.value.name, Mailbox.Routing.none);
		User user2 = PopulateHelper.getUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		String user2Uid = PopulateHelper.addUser(domain.value.name, user2);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		groupService.add(group1Uid, Arrays.asList(Member.user(user1Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(user2Uid)));
		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));

		LdapExportService.build(domain.uid).sync();

		checkGroupHierarchyMembers(group1Uid, user1Uid, user1Login, group2Uid, user2.login, user2Uid);

		TaskRef tr = groupService.delete(group2Uid);
		waitFor(tr);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		String user2Dn = new DomainDirectoryUser(domain, ItemValue.create(user2Uid, user2), null).getDn();
		String group2Dn = new DomainDirectoryGroup(domain, ItemValue.create(group2Uid, group2)).getDn();

		Entry entry = ldapCon.lookup(group2Dn);
		assertNull(entry);

		entry = ldapCon.lookup(new DomainDirectoryGroup(domain, ItemValue.create(group1Uid, group1)).getDn());
		Attribute attrs = entry.get("member");
		assertFalse(attrs.contains(user2Dn));
		assertFalse(attrs.contains(group2Dn));
		attrs = entry.get("memberUid");
		assertFalse(attrs.contains(user2.login));
	}

	@Test
	public void testExportGroup_updateIfPresentOnCreate() throws Exception {
		String groupUid = addGroup();

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		ItemValue<Group> group = groupService.getComplete(groupUid);

		LdapExportService.build(domain.uid).sync();

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);

		Entry ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);
		assertEquals(groupUid, ldapGroup.get("bmuid").getString());
		assertNull(ldapGroup.get("description"));

		// Force sync from beginning
		ModifyRequest modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(new Dn("dc=" + domain.value.name + ",dc=local"));
		modifyRequest.replace("bmVersion", "0");
		ldapCon.modify(modifyRequest);

		group.value.description = "Updated description";
		groupService.update(group.uid, group.value);

		LdapExportService.build(domain.uid).sync();

		ldapGroup = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");
		assertNotNull(ldapGroup);
		assertEquals(groupUid, ldapGroup.get("bmuid").getString());
		assertEquals("Updated description", ldapGroup.get("description").getString());
	}

	/**
	 * Check:
	 * <ul>
	 * <li>user (<i>groupUserUid</i>, <i>groupUserLogin</i>) member of group
	 * (<i>groupUid</i>)</li>
	 * <li>user (<i>childGroupUserLogin</i>, <i>childGroupUserUid</i>) member of
	 * group (<i>childGroupUid</i>)</li>
	 * <li>group (<i>childGroupUid</i>) member of group (<i>groupUid</i>)</li>
	 * </ul>
	 * 
	 * @param groupUid
	 * @param groupUserUid
	 * @param groupUserLogin
	 * @param childGroupUid
	 * @param childGroupUserLogin
	 * @param childGroupUserUid
	 * @throws LdapException
	 * @throws CursorException
	 * @throws LdapInvalidAttributeValueException
	 */
	private void checkGroupHierarchyMembers(String groupUid, String groupUserUid, String groupUserLogin,
			String childGroupUid, String childGroupUserLogin, String childGroupUserUid)
			throws LdapException, CursorException, LdapInvalidAttributeValueException {
		checkUserIsMemberOfGroup(childGroupUid, childGroupUserUid, childGroupUserLogin);

		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);

		List<Entry> ldapUser1 = LdapHelper.getLdapEntryFromUid(ldapCon, domain, groupUserUid, "memberOf");
		assertEquals(1, ldapUser1.size());

		List<Entry> ldapGroup1 = LdapHelper.getLdapEntryFromUid(ldapCon, domain, groupUid, "member", "memberuid",
				"memberOf");
		assertEquals(1, ldapGroup1.size());

		List<Entry> ldapGroup2 = LdapHelper.getLdapEntryFromUid(ldapCon, domain, childGroupUid, "member", "memberuid",
				"memberOf");
		assertEquals(1, ldapGroup2.size());

		// Check group1 member and memberUid attributes
		Attribute memberAttribute = ldapGroup1.get(0).get("member");
		assertNotNull(memberAttribute);
		assertEquals(2, memberAttribute.size());
		assertTrue(memberAttribute.contains(ldapUser1.get(0).getDn().getName()));
		assertTrue(memberAttribute.contains(ldapGroup2.get(0).getDn().getName()));

		Attribute memberUidAttribute = ldapGroup1.get(0).get("memberUid");
		assertNotNull(memberUidAttribute);
		assertEquals(2, memberUidAttribute.size());
		assertTrue(memberUidAttribute.contains(groupUserLogin));
		assertTrue(memberUidAttribute.contains(childGroupUserLogin));

		// Check user1 memberOf attributes
		Attribute memberOfAttribute = ldapUser1.get(0).get("memberof");
		assertNotNull(memberOfAttribute);
		assertEquals(1, memberOfAttribute.size());
		assertEquals(ldapGroup1.get(0).getDn().getName(), memberOfAttribute.getString());

		// Check group2 memberOf attributes
		memberOfAttribute = ldapGroup2.get(0).get("memberof");
		assertNotNull(memberOfAttribute);
		assertEquals(1, memberOfAttribute.size());
		assertEquals(ldapGroup1.get(0).getDn().getName(), memberOfAttribute.getString());
	}

	/**
	 * Check user (<i>userUid</i>, <i>userLogin</i>) is member of group
	 * (<i>groupUid</i>).
	 * 
	 * @param groupUid
	 * @param userUid
	 * @param userLogin
	 * 
	 * @throws LdapException
	 * @throws CursorException
	 * @throws LdapInvalidAttributeValueException
	 */
	private void checkUserIsMemberOfGroup(String groupUid, String userUid, String userLogin)
			throws LdapException, CursorException, LdapInvalidAttributeValueException {
		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);

		List<Entry> ldapUser = LdapHelper.getLdapEntryFromUid(ldapCon, domain, userUid, "memberOf");
		assertEquals(1, ldapUser.size());

		List<Entry> ldapGroup = LdapHelper.getLdapEntryFromUid(ldapCon, domain, groupUid);
		assertEquals(1, ldapGroup.size());

		Attribute memberAttribute = ldapGroup.get(0).get("member");
		assertNotNull(memberAttribute);
		assertEquals(1, memberAttribute.size());
		assertEquals(ldapUser.get(0).getDn().getName(), memberAttribute.getString());

		Attribute memberUidAttribute = ldapGroup.get(0).get("memberUid");
		assertNotNull(memberUidAttribute);
		assertEquals(1, memberUidAttribute.size());
		assertEquals(userLogin, memberUidAttribute.getString());

		Attribute memberOfAttribute = ldapUser.get(0).get("memberof");
		assertNotNull(memberOfAttribute);
		assertEquals(1, memberOfAttribute.size());
		assertEquals(ldapGroup.get(0).getDn().getName(), memberOfAttribute.getString());
	}

	private void checkUidIsNotMemberOfGroup(String groupUid, String memberUid) throws LdapException, CursorException {
		assertEquals(0,
				LdapHelper
						.getLdapEntryFromUid(LdapHelper.connectDirectory(ldapRoleServer), domain, memberUid, "memberOf")
						.size());
	}

	private void initAndAssignLdapExportServer() throws ServerFault, SQLException, InterruptedException {
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		String ldapRoleServerUid = UUID.randomUUID().toString();
		Server ldapRoleServer = new Server();
		ldapRoleServer.name = "LDAP export server";
		ldapRoleServer.ip = new BmConfIni().get("bluemind/ldap");
		waitFor(serverService.create(ldapRoleServerUid, ldapRoleServer));

		this.ldapRoleServer = serverService.getComplete(ldapRoleServerUid);

		INodeClient nodeClient = NodeActivator.get(ldapRoleServer.ip);
		updateUserPassword(nodeClient, "admin0@global.virt", Token.admin0());

		waitFor(serverService.setTags(ldapRoleServerUid, Arrays.asList(LDAPTAG)));

		serverService.assign(ldapRoleServerUid, domain.uid, LDAPTAG);
		// Wait for domain assign ending
		Thread.sleep(1000);
	}

	private void waitFor(TaskRef taskRef) {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		assertEquals(State.Success, task.status().state);
	}

	private void updateUserPassword(INodeClient nodeClient, String login, String passwd) {
		NCUtils.exec(nodeClient, "/usr/local/sbin/updateUserPassword.sh " + login + " " + passwd);
	}

	private class Results {
		List<String> userNotExported = new ArrayList<>();
		List<String> userExported = new ArrayList<>();
		List<String> groupNotExported = new ArrayList<>();
		List<String> groupExported = new ArrayList<>();
	}

	private String addGroup() {
		return addGroup(false);
	}

	private String addGroup(boolean system) {
		Group group = getGroup();
		group.system = system;
		return addGroup(group);
	}

	private String addGroup(Group group) {
		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.value.name);
		String uid = UIDGenerator.uid();
		groupService.create(uid, group);
		return uid;
	}

	private Group getGroup() {
		Group group = new Group();
		group.name = UUID.randomUUID().toString();

		return group;
	}

	private Results checkSync() throws LdapException {
		IDirectory directoryService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, domain.uid);
		ContainerChangeset<String> changeSet = directoryService.changeset(0l);
		assertNotEquals(0, changeSet.version);
		checkBmVersion(changeSet.version);

		Results results = new Results();
		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		for (String uid : changeSet.created) {
			DirEntry dirEntry = directoryService.findByEntryUid(uid);
			assertNotNull(dirEntry);

			switch (dirEntry.kind) {
			case USER:
				checkUser(ldapCon, dirEntry, results);
				break;
			case GROUP:
				checkGroup(ldapCon, dirEntry, results);
				break;
			default:
				break;
			}
		}
		return results;
	}

	private void checkGroup(LdapConnection ldapCon, DirEntry dirEntry, Results results) throws LdapException {
		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domain.uid);
		ItemValue<Group> group = groupService.getComplete(dirEntry.entryUid);

		Entry ldapEntry = ldapCon.lookup("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local");

		if (dirEntry.system) {
			assertNull(ldapEntry);
			results.groupNotExported.add(dirEntry.entryUid);
		} else {
			assertNotNull(ldapEntry);
			results.groupExported.add(dirEntry.entryUid);
		}
	}

	private void checkUser(LdapConnection ldapCon, DirEntry dirEntry, Results results) throws LdapException {
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.uid);
		ItemValue<User> user = userService.getComplete(dirEntry.entryUid);

		Entry ldapEntry = ldapCon.lookup("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local");

		if (dirEntry.system) {
			assertNull(ldapEntry);
			results.userNotExported.add(dirEntry.entryUid);
		} else {
			assertNotNull(ldapEntry);
			results.userExported.add(dirEntry.entryUid);
		}
	}

	private void checkBmVersion() throws ServerFault, LdapException {
		checkBmVersion(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, domain.uid).changeset(0l).version);
	}

	private void checkBmVersion(long i) throws LdapException {
		LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer);
		Entry entry = ldapCon.lookup("dc=" + domain.value.name + ",dc=local", "bmVersion");
		assertEquals(i, Long.parseLong(entry.get("bmVersion").getString()));
	}
}
