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
package net.bluemind.directory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.DirEntryQuery.StateFilter;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class DirectoryTests {

	private Container domainsContainer;

	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();
		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(esServer);
		domainsContainer = containerStore.get(DomainsContainerIdentifier.getIdentifier());
		assertNotNull(domainsContainer);

		domainUid = "test" + System.currentTimeMillis() + ".lan";
		PopulateHelper.createTestDomain(domainUid);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void testGetRoot() throws ServerFault {
		IDirectory dir = service();
		DirEntry rootEntry = dir.getRoot();
		assertNotNull(rootEntry);
		assertEquals(domainUid, rootEntry.path);
	}

	@Test
	public void testGetEntries() throws ServerFault {
		IDirectory dir = service();
		List<DirEntry> entries = dir.getEntries("");

		for (DirEntry entry : entries) {
			System.err.println(entry.path);
		}
		// should find domain path and addressbook path and
		// two group
		assertEquals(4, entries.size());
		assertEquals(domainUid, entries.get(0).path);
		assertEquals(domainUid + "/addressbooks/addressbook_" + domainUid, entries.get(1).path);

		entries = dir.getEntries(domainUid);
		// self (domainUid path) is not in result
		assertEquals(3, entries.size());
		assertEquals(domainUid + "/addressbooks/addressbook_" + domainUid, entries.get(0).path);
	}

	@Test
	public void testDelete() throws ServerFault, SQLException {
		IDirectory dir = service();

		List<DirEntry> entries = dir.getEntries(domainUid);
		int dirEntrySize = entries.size();

		ContainerStore cs = new ContainerStore(null, JdbcActivator.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		assertNotNull(cs.get("addressbook_" + domainUid));

		// delete addressbook
		TaskRef tr = dir.delete(domainUid + "/addressbooks/addressbook_" + domainUid);
		waitTaskEnd(tr);
		assertNull(cs.get("addressbook_" + domainUid));

		entries = dir.getEntries(domainUid);
		assertEquals(dirEntrySize - 1, entries.size());
	}

	@Test
	public void testDeleteWrongPath() throws ServerFault {
		IDirectory dir = service();

		try {
			TaskRef tr = dir.delete(domainUid + "/groups");
			waitTaskEnd(tr);
			fail("should not succeed because delete only work on one entry, not hierarchy");
		} catch (ServerFault e) {

		}
	}

	@Test
	public void testSearchFilterByEmail() throws ServerFault, IOException {
		IDirectory dir = service();
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		User admin = new User();
		admin.login = "test";
		admin.contactInfos = new VCard();
		admin.contactInfos.identification.name.familyNames = "myName";
		admin.password = "test";
		admin.routing = Mailbox.Routing.none;

		admin.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		admin.emails = Arrays.asList(Email.create("test" + "@" + domainUid, true, true),
				Email.create("test2" + "@alias" + domainUid, false, false));
		String uid = UIDGenerator.uid();
		userService.create(uid, admin);

		assertEquals(1, dir.search(DirEntryQuery.filterEmail("test@" + domainUid)).total);
		assertEquals(0, dir.search(DirEntryQuery.filterEmail("test2@" + domainUid)).total);
		assertEquals(0, dir.search(DirEntryQuery.filterEmail("test@fakeDomain.net")).total);
	}

	@Test
	public void testSearchFilterByEmailInvalid() throws ServerFault, IOException {
		IDirectory dir = service();
		try {
			dir.search(DirEntryQuery.filterEmail("test"));
			fail("should not pass");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testChangelog_parentGroupOnUserDelete() throws ServerFault, IOException {
		String userUid = PopulateHelper.addUser("test-" + System.nanoTime(), domainUid);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);

		String group1Uid = UIDGenerator.uid();
		Group group = new Group();
		group.name = "test-" + System.nanoTime();
		groupService.create(group1Uid, group);

		String group2Uid = UIDGenerator.uid();
		group = new Group();
		group.name = "test-" + System.nanoTime();
		groupService.create(group2Uid, group);

		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.user(userUid)));

		long changeSetVersion = service().changeset(0l).version;

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid)
				.delete(userUid);
		waitTaskEnd(tr);

		ContainerChangeset<String> changeset = service().changeset(changeSetVersion);
		assertTrue(changeset.created.isEmpty());
		assertTrue(changeset.deleted.contains(userUid));
		assertEquals(1, changeset.deleted.size());
		assertTrue(changeset.updated.contains(group2Uid));
		assertTrue(changeset.updated.contains(group1Uid));
		assertEquals(2, changeset.updated.size());
	}

	@Test
	public void testChangelog_parentGroupOnGroupDelete() throws ServerFault, IOException {
		String userUid = PopulateHelper.addUser("test-" + System.nanoTime(), domainUid);

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);

		String group1Uid = UIDGenerator.uid();
		Group group = new Group();
		group.name = "test-" + System.nanoTime();
		groupService.create(group1Uid, group);

		String group2Uid = UIDGenerator.uid();
		group = new Group();
		group.name = "test-" + System.nanoTime();
		groupService.create(group2Uid, group);

		String group3Uid = UIDGenerator.uid();
		group = new Group();
		group.name = "test-" + System.nanoTime();
		groupService.create(group3Uid, group);

		groupService.add(group1Uid, Arrays.asList(Member.group(group2Uid)));
		groupService.add(group2Uid, Arrays.asList(Member.group(group3Uid)));
		groupService.add(group3Uid, Arrays.asList(Member.user(userUid)));

		long changeSetVersion = service().changeset(0l).version;

		TaskRef tr = groupService.delete(group3Uid);
		waitTaskEnd(tr);

		ContainerChangeset<String> changeset = service().changeset(changeSetVersion);
		assertTrue(changeset.created.isEmpty());
		assertTrue(changeset.deleted.contains(group3Uid));
		assertEquals(1, changeset.deleted.size());
		assertTrue(changeset.updated.contains(group2Uid));
		assertEquals(1, changeset.updated.size());
	}

	@Test
	public void testByEmail() throws ServerFault, IOException {
		IDirectory dir = service();
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		User admin = new User();
		admin.login = "test";
		admin.contactInfos = new VCard();
		admin.contactInfos.identification.name.familyNames = "myName";
		admin.password = "test";
		admin.routing = Mailbox.Routing.none;

		admin.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		admin.emails = Arrays.asList(Email.create("test" + "@" + domainUid, true, true),
				Email.create("test2" + "@alias" + domainUid, false, false));
		String uid = UIDGenerator.uid();
		userService.create(uid, admin);

		assertNotNull(dir.getByEmail("test@" + domainUid));
		assertNull(dir.getByEmail("test2@" + domainUid));
		assertNull(dir.getByEmail("test@fakeDomain.net"));
		assertNotNull(dir.getByEmail("test2@alias" + domainUid));
	}

	@Test
	public void testByRoles() throws ServerFault, IOException {
		IDirectory dir = service();

		String userUid1 = PopulateHelper.addUser("test1-" + System.nanoTime(), domainUid);
		String userUid2 = PopulateHelper.addUser("test2-" + System.nanoTime(), domainUid);
		String userUid3 = PopulateHelper.addUser("test3-" + System.nanoTime(), domainUid);
		PopulateHelper.addGroup(domainUid, "group1" + System.nanoTime(), "group1", new ArrayList<Member>());

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);

		userService.setRoles(userUid1, new HashSet<String>(Arrays.asList("role1", "role2", "role3")));
		userService.setRoles(userUid2, new HashSet<String>(Arrays.asList("role2", "role3")));
		userService.setRoles(userUid3, new HashSet<String>(Arrays.asList("role3")));

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);

		String group1Uid = UIDGenerator.uid();
		Group group = new Group();
		group.name = "test-" + System.nanoTime();
		groupService.create(group1Uid, group);

		groupService.setRoles(group1Uid, new HashSet<String>(Arrays.asList("role1")));

		List<ItemValue<DirEntry>> byRoles1 = dir.getByRoles(Arrays.asList("role1", "role2", "role3"));
		assertTrue(contained(byRoles1, userUid1));
		assertTrue(contained(byRoles1, group1Uid));
		assertTrue(contained(byRoles1, userUid2));
		assertTrue(contained(byRoles1, userUid3));
		List<ItemValue<DirEntry>> byRoles2 = dir.getByRoles(Arrays.asList("role2"));
		assertTrue(contained(byRoles2, userUid1));
		assertFalse(contained(byRoles2, group1Uid));
		assertTrue(contained(byRoles2, userUid2));
		assertFalse(contained(byRoles2, userUid3));
	}

	private boolean contained(List<ItemValue<DirEntry>> entry, String uid) {
		return entry.stream().filter(e -> e.uid.equals(uid)).findFirst().isPresent();
	}

	@Test
	public void testDirEntryDataLocation() throws ServerFault {
		IDirectory dir = service();
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		User u = new User();
		u.login = "logn" + UUID.randomUUID();
		u.contactInfos = new VCard();
		u.contactInfos.identification.name.familyNames = "myName";
		u.password = "password";
		u.routing = Mailbox.Routing.none;
		u.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		String uid = UUID.randomUUID().toString();
		userService.create(uid, u);

		DirEntry de = dir.findByEntryUid(uid);
		assertEquals(u.dataLocation, de.dataLocation);
	}

	@Test
	public void testDirEntryI18n() throws ServerFault {
		IDirectory dir = service();

		// en
		DirEntry entry = dir.getEntry(String.format("%s/%s/%s", domainUid, "addressbooks", "addressbook_" + domainUid));
		assertEquals("Directory", entry.displayName);

		// fr
		SecurityContext ctxFr = new SecurityContext(null, "system", Collections.emptyList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), Collections.emptyMap(), "global.virt", "fr",
				"internal-system", false);
		IDirectory dirFr = ServerSideServiceProvider.getProvider(ctxFr).instance(IDirectory.class, domainUid);
		entry = dirFr.getEntry(String.format("%s/%s/%s", domainUid, "addressbooks", "addressbook_" + domainUid));
		assertEquals("Annuaire", entry.displayName);
	}

	@Test
	public void testSearchFilterByKindAndState() throws ServerFault, IOException {
		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);
		Group group = new Group();
		group.name = "group";
		group.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		groupService.create(UIDGenerator.uid(), group);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);

		User user = new User();
		user.login = "test";
		user.contactInfos = new VCard();
		user.contactInfos.identification.name.familyNames = "myName";
		user.password = "test";
		user.routing = Mailbox.Routing.none;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		userService.create(UIDGenerator.uid(), user);

		user = new User();
		user.login = "archived";
		user.archived = true;
		user.contactInfos = new VCard();
		user.contactInfos.identification.name.familyNames = "archived";
		user.password = "archived";
		user.routing = Mailbox.Routing.none;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		String uid = UIDGenerator.uid();
		userService.create(uid, user);

		DirEntryQuery deq = DirEntryQuery.filterKind(Kind.USER);
		deq.stateFilter = StateFilter.Archived;
		ListResult<ItemValue<DirEntry>> result = service().search(deq);
		assertEquals(1, result.total);
		assertEquals(uid, result.values.get(0).uid);
	}

	private void waitTaskEnd(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		TaskStatus status = task.status();
		if (status.state == State.InError) {
			throw new ServerFault("xfer error");
		}
	}

	private IDirectory service() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid);
	}
}
