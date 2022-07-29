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
package net.bluemind.group.persistence.tests;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.Member.Type;
import net.bluemind.group.persistence.GroupStore;

public class GroupStoreTests {
	private static Logger logger = LoggerFactory.getLogger(GroupStoreTests.class);
	private GroupStore groupStore;
	private ItemStore domainItemStore;
	private ContainerStore containerStore;
	private Container userContainer;

	public static class MemberWithItem extends Member {
		public Item item;
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), securityContext);
		String containerId = "users_" + System.nanoTime() + ".fr";
		userContainer = Container.create(containerId, "domain", containerId, "me", true);
		userContainer = containerStore.create(userContainer);
		assertNotNull(userContainer);

		domainItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), userContainer, securityContext);

		groupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), userContainer);

		logger.debug("stores: {} {}", domainItemStore, groupStore);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws Exception {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);
	}

	private void compareGroup(Group g1, Group g2) {
		assertNotNull("Nothing found", g2);
		assertEquals(g1.name, g2.name);
		assertEquals(g1.description, g2.description);
		assertEquals(g1.hidden, g2.hidden);
		assertEquals(g1.hiddenMembers, g2.hiddenMembers);
	}

	@Test
	public void testUpdate() throws Exception {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group found = groupStore.get(item);

		g = getDefaultGroup();
		g.description = "fake desc";
		g.hidden = !g.hidden;
		g.hiddenMembers = !g.hiddenMembers;
		groupStore.update(item, g);

		found = groupStore.get(item);
		compareGroup(g, found);
	}

	@Test
	public void testDelete() throws Exception {
		Item item = initAndCreateGroup();

		groupStore.delete(item);
		Group found = groupStore.get(item);
		assertNull(found);
	}

	@Test
	public void testDeleteWithMemberAndParent() throws SQLException, ServerFault {
		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		groupStore.addGroupsMembers(item1, Arrays.asList(item2));

		List<MemberWithItem> userMembers = getUserMember(1);
		List<MemberWithItem> groupMembers = getGroupMember(1);
		List<MemberWithItem> externalUserMembers = getExternalUserMember(1);

		groupStore.addUsersMembers(item2, membersToList(userMembers));
		groupStore.addGroupsMembers(item2, membersToList(groupMembers));
		groupStore.addExternalUsersMembers(item2, membersToList(externalUserMembers));

		groupStore.delete(item2);

		assertEquals(0, groupStore.getParents(domainItemStore.get(groupMembers.get(0).uid)).size());
		assertEquals(0, groupStore.getMembers(item1).size());
	}

	@Test
	public void testDeleteAll() throws Exception {
		Item item = initAndCreateGroup();

		groupStore.deleteAll();
		Group found = groupStore.get(item);
		assertNull(found);
	}

	@Test
	public void testDeleteAllWithMemberAndParent() throws Exception {
		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		groupStore.addGroupsMembers(item1, Arrays.asList(item2));
		groupStore.addGroupsMembers(item2, membersToList(getGroupMember(1)));
		groupStore.addUsersMembers(item2, membersToList(getUserMember(1)));
		groupStore.addExternalUsersMembers(item2, membersToList(getExternalUserMember(1)));

		groupStore.deleteAll();
		assertNull(groupStore.get(item1));
		assertNull(groupStore.get(item2));
	}

	private Group getDefaultGroup() {
		return getDefaultGroup(null);
	}

	private Group getDefaultGroup(String prefix) {
		Group g = new Group();

		if (prefix == null || prefix.isEmpty()) {
			prefix = "test";
		}

		g.name = prefix + System.nanoTime();
		g.description = "description " + g.name;

		g.hidden = false;
		g.hiddenMembers = false;

		Email e = new Email();
		e.address = g.name + "@blue-mind.loc";
		g.emails = new ArrayList<Email>(1);
		g.emails.add(e);

		return g;
	}

	@Test
	public void testAddUsersMembers() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);

		List<MemberWithItem> usersMembers = getUserMember(3);
		groupStore.addUsersMembers(item, membersToList(usersMembers));

		List<Member> members = groupStore.getMembers(item);
		compareMembers(usersMembers, members);
	}
	
	@Test
	public void testAddExternalUsersMembers() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);

		List<MemberWithItem> externalUsersMembers = getExternalUserMember(3);
		groupStore.addExternalUsersMembers(item, membersToList(externalUsersMembers));

		List<Member> members = groupStore.getMembers(item);
		compareMembers(externalUsersMembers, members);
	}

	private List<MemberWithItem> getUserMember(int count) throws SQLException {
		List<MemberWithItem> usersMembers = new ArrayList<MemberWithItem>(count);
		for (int i = 0; i < count; i++) {
			String itemUid = UUID.randomUUID().toString();
			Item item = domainItemStore.create(Item.create(itemUid, null));

			MemberWithItem userMember = new MemberWithItem();
			userMember.type = Type.user;
			userMember.uid = itemUid;
			userMember.item = item;
			usersMembers.add(userMember);
		}

		return usersMembers;
	}
	
	private List<MemberWithItem> getGroupMember(int count) throws SQLException {
		List<MemberWithItem> groupsMembers = new ArrayList<MemberWithItem>(count);
		for (int i = 0; i < count; i++) {
			Item item = initAndCreateGroup();

			MemberWithItem groupMember = new MemberWithItem();
			groupMember.type = Type.group;
			groupMember.uid = item.uid;
			groupMember.item = item;
			groupsMembers.add(groupMember);
		}

		return groupsMembers;
	}
	
	private List<MemberWithItem> getExternalUserMember(int count) throws SQLException {
		List<MemberWithItem> externalUserMembers = new ArrayList<MemberWithItem>(count);
		for (int i = 0; i < count; i++) {
			Item item = initAndCreateGroup();

			MemberWithItem groupMember = new MemberWithItem();
			groupMember.type = Type.external_user;
			groupMember.uid = item.uid;
			groupMember.item = item;
			externalUserMembers.add(groupMember);
		}

		return externalUserMembers;
	}

	private void compareMembers(List<? extends Member> expected, List<? extends Member> found) {
		assertEquals(expected.size(), found.size());
		int count = 0;
		for (Member expectedMember : expected) {
			for (Member memberFound : found) {
				logger.info("Comparing : " + expectedMember.type + " " + memberFound.type + " " + expectedMember.uid + " " + memberFound.uid);
				if (expectedMember.type == memberFound.type && memberFound.uid.equals(expectedMember.uid)) {
					count++;
					break;
				}
			}
		}
		assertEquals(expected.size(), count);
	}

	@Test
	public void testAddGroupsMembers() throws SQLException, ServerFault {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);

		List<MemberWithItem> groupsMembers = getGroupMember(3);
		groupStore.addGroupsMembers(item, membersToList(groupsMembers));

		List<Member> members = groupStore.getMembers(item);
		compareMembers(groupsMembers, members);
	}

	/**
	 * Add a member, then add another member. Check that both are members.
	 * 
	 * @throws SQLException
	 * @throws ServerFault
	 */
	@Test
	public void testAddMultipleMember() throws SQLException, ServerFault {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);

		List<MemberWithItem> groupsMembers = getGroupMember(2);
		for (MemberWithItem groupMember : groupsMembers) {
			groupStore.addGroupsMembers(item, membersToList(Arrays.asList(groupMember)));
		}

		List<MemberWithItem> usersMembers = getUserMember(2);
		for (MemberWithItem userMember : usersMembers) {
			groupStore.addUsersMembers(item, membersToList(Arrays.asList(userMember)));
		}
		
		List<MemberWithItem> externalUsersMembers = getExternalUserMember(2);
		for (MemberWithItem externalUserMember : externalUsersMembers) {
			groupStore.addExternalUsersMembers(item, membersToList(Arrays.asList(externalUserMember)));
		}

		List<Member> members = new ArrayList<>(groupsMembers);
		members.addAll(usersMembers);
		members.addAll(externalUsersMembers);
		List<Member> addedMembers = groupStore.getMembers(item);
		compareMembers(members, addedMembers);
	}

	/**
	 * Add members, then add other members. Check that all are members.
	 * 
	 * @throws SQLException
	 * @throws ServerFault
	 */
	@Test
	public void testAddMultipleMembers() throws SQLException, ServerFault {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);

		List<MemberWithItem> groupsMembers = getGroupMember(2);
		groupStore.addGroupsMembers(item, membersToList(groupsMembers));
		List<MemberWithItem> usersMembers = getUserMember(2);
		groupStore.addUsersMembers(item, membersToList(usersMembers));
		List<MemberWithItem> externalUsersMembers = getExternalUserMember(2);
		groupStore.addExternalUsersMembers(item, membersToList(externalUsersMembers));

		List<MemberWithItem> members = groupsMembers;
		members.addAll(usersMembers);
		members.addAll(externalUsersMembers);

		List<Member> addedMembers = groupStore.getMembers(item);
		compareMembers(members, addedMembers);
	}

	private List<Item> membersToList(List<MemberWithItem> groupsMembers) {
		return groupsMembers.stream().map(m -> m.item).collect(Collectors.toList());
	}

	@Test
	public void testRemoveMembers() throws SQLException, ServerFault {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);

		List<MemberWithItem> groupsMembers = getGroupMember(2);
		groupStore.addGroupsMembers(item, membersToList(groupsMembers));

		List<MemberWithItem> usersMembers = getUserMember(2);
		groupStore.addUsersMembers(item, membersToList(usersMembers));
		
		List<MemberWithItem> externalUsersMembers = getExternalUserMember(2);
		groupStore.addExternalUsersMembers(item, membersToList(externalUsersMembers));

		List<Member> allMembers = new ArrayList<Member>();
		allMembers.addAll(groupsMembers);
		allMembers.addAll(usersMembers);
		allMembers.addAll(externalUsersMembers);
		
		List<Member> addedMembers = groupStore.getMembers(item);
		compareMembers(allMembers, addedMembers);

		ArrayList<String> toRemove = new ArrayList<String>();
		toRemove.add(groupsMembers.get(0).uid);
		toRemove.add(usersMembers.get(0).uid);
		toRemove.add(externalUsersMembers.get(0).uid);

		groupStore.removeUsersMembers(item, Arrays.asList(usersMembers.get(0).item.id));
		groupStore.removeGroupsMembers(item, Arrays.asList(groupsMembers.get(0).item.id));
		groupStore.removeExternalUsersMembers(item, Arrays.asList(externalUsersMembers.get(0).item.id));
		
		addedMembers = groupStore.getMembers(item);
		for (String remove : toRemove) {
			for (Member addedMember : addedMembers) {
				assertFalse(remove.equals(addedMember.uid));
			}
		}
	}

	@Test
	public void testGetMembers() throws SQLException, ServerFault {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		compareGroup(g, created);

		List<MemberWithItem> groupsMembers = getGroupMember(2);
		groupStore.addGroupsMembers(item, membersToList(groupsMembers));
		List<MemberWithItem> usersMembers = getUserMember(2);
		groupStore.addUsersMembers(item, membersToList(usersMembers));
		List<MemberWithItem> externalUsersMembers = getExternalUserMember(2);
		groupStore.addExternalUsersMembers(item, membersToList(externalUsersMembers));

		List<Member> members = new ArrayList<>(groupsMembers);
		members.addAll(usersMembers);
		members.addAll(externalUsersMembers);

		List<Member> addedMembers = groupStore.getMembers(item);

		assertEquals(members.size(), addedMembers.size());

		for (Member member : members) {
			boolean found = false;
			for (Member getMember : addedMembers) {
				if (member.type == getMember.type && member.uid.equals(getMember.uid)) {
					found = true;
					break;
				}
			}
			assertTrue(found);
		}
	}

	@Test
	public void testAddUserMembersAlreadyMember() throws SQLException, ServerFault {
		Item item = initAndCreateGroup();

		List<MemberWithItem> usersMembers = getUserMember(1);
		groupStore.addUsersMembers(item, membersToList(usersMembers));

		try {
			groupStore.addUsersMembers(item, membersToList(usersMembers));
			fail("Test must thrown an exception");
		} catch (Throwable t) {
			assertTrue(t.getMessage().contains("violate") && t.getMessage().contains("t_group_usermember_pkey"));
		}
	}
	
	@Test
	public void testAddExternalUserMembersAlreadyMember() throws SQLException, ServerFault {
		Item item = initAndCreateGroup();

		List<MemberWithItem> externalUserMembers = getExternalUserMember(1);
		groupStore.addExternalUsersMembers(item, membersToList(externalUserMembers));

		try {
			groupStore.addExternalUsersMembers(item, membersToList(externalUserMembers));
			fail("Test must thrown an exception");
		} catch (Throwable t) {
			assertTrue(t.getMessage().contains("violate") && t.getMessage().contains("t_group_externalusermember_pkey"));
		}
	}

	@Test
	public void testAddGroupsMembersAlreadyMember() throws SQLException, ServerFault {
		Item item = initAndCreateGroup();

		List<MemberWithItem> groupsMembers = getGroupMember(1);
		groupStore.addGroupsMembers(item, membersToList(groupsMembers));

		try {
			groupStore.addGroupsMembers(item, membersToList(groupsMembers));
			fail("Test must thrown an exception");
		} catch (Throwable t) {
			assertTrue(t.getMessage().contains("violate") && t.getMessage().contains("t_group_groupmember_pkey"));
		}
	}

	/**
	 * 1. Create groups g1 and g2<br>
	 * 2. Add a user and g2 as member of g1<br>
	 * 3. Add a user to g2
	 * 
	 * Check g1 and g2 users members
	 * 
	 * @throws SQLException
	 * @throws ServerFault
	 */
	@Test
	public void testGetFlatUsersMembers() throws SQLException, ServerFault {
		ArrayList<Member> members = new ArrayList<Member>();

		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		List<MemberWithItem> g1Members = getUserMember(1);
		groupStore.addUsersMembers(item1, membersToList(g1Members));
		members.add(g1Members.get(0));

		groupStore.addGroupsMembers(item1, Arrays.asList(item2));

		List<MemberWithItem> g2Members = getUserMember(1);
		groupStore.addUsersMembers(item2, membersToList(g2Members));
		members.add(g2Members.get(0));

		compareMembers(members, groupStore.getFlatUsersMembers(item1));
		compareMembers(g2Members, groupStore.getFlatUsersMembers(item2));
	}
	
	/**
	 * 1. Create groups g1 and g2<br>
	 * 2. Add an external user to g1 and g2 as member of g1<br>
	 * 3. Add an external user to g2
	 * 
	 * Check g1 and g2 users members
	 * 
	 * @throws SQLException
	 * @throws ServerFault
	 */
	@Test
	public void testGetFlatUsersMembersWithExternalUsers() throws SQLException, ServerFault {
		ArrayList<Member> members = new ArrayList<Member>();

		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		List<MemberWithItem> g1Members = getExternalUserMember(1);
		groupStore.addExternalUsersMembers(item1, membersToList(g1Members));
		members.add(g1Members.get(0));
		logger.info("STEP 1");
		groupStore.addGroupsMembers(item1, Arrays.asList(item2));
		
		logger.info("STEP 2 : " + groupStore.getFlatUsersMembers(item1).size());
		compareMembers(members, groupStore.getFlatUsersMembers(item1));
		logger.info("STEP 3");
		
		List<MemberWithItem> g2Members = getExternalUserMember(1);
		groupStore.addExternalUsersMembers(item2, membersToList(g2Members));
		members.add(g2Members.get(0));

		compareMembers(members, groupStore.getFlatUsersMembers(item1));
		compareMembers(g2Members, groupStore.getFlatUsersMembers(item2));
	}

	/**
	 * 1. Create groups g1 and g2<br>
	 * 2. Add a user to g1<br>
	 * 3. Add a user to g2<br>
	 * 4. Add a g2 to g1<br>
	 * 
	 * Check g1 and g2 users members
	 * 
	 * @throws SQLException
	 * @throws ServerFault
	 */
	@Test
	public void testGetFlatUsersMembers2() throws SQLException, ServerFault {
		ArrayList<Member> g1UsersMembers = new ArrayList<Member>();

		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		List<MemberWithItem> g1Members = getUserMember(1);
		groupStore.addUsersMembers(item1, membersToList(g1Members));
		g1UsersMembers.add(g1Members.get(0));

		List<MemberWithItem> g2Members = getUserMember(1);
		groupStore.addUsersMembers(item2, membersToList(g2Members));
		g1UsersMembers.add(g2Members.get(0));

		groupStore.addGroupsMembers(item1, Arrays.asList(item2));

		compareMembers(g1UsersMembers, groupStore.getFlatUsersMembers(item1));
		compareMembers(g2Members, groupStore.getFlatUsersMembers(item2));
	}

	/**
	 * 1. Create groups g1, g2, g3 and g4<br>
	 * 2. Add a user to g1<br>
	 * 3. Add a user to g2<br>
	 * 4. Add an external user to g3 <br>
	 * 5. Add a user to g4<br>
	 * 6. Add a g2 to g1<br>
	 * 7. Add a g3 to g1<br>
	 * 8. Add a g4 to g2<br>
	 * 9. Add a g4 to g3<br>
	 * 
	 * Check g1 users members
	 * 
	 * 10. Remove user from g2
	 * 
	 * Check g1 users members
	 * 
	 * @throws SQLException
	 * @throws ServerFault
	 */
	@Test
	public void testGetFlatUsersMembers3() throws SQLException, ServerFault {
		ArrayList<Member> g1UsersMembers = new ArrayList<Member>();

		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();
		Item item3 = initAndCreateGroup();
		Item item4 = initAndCreateGroup();

		// Add user member to g1
		List<MemberWithItem> g1Members = getUserMember(1);
		groupStore.addUsersMembers(item1, membersToList(g1Members));
		g1UsersMembers.add(g1Members.get(0));

		// Add user member to g2
		List<MemberWithItem> g2Members = getUserMember(1);
		groupStore.addUsersMembers(item2, membersToList(g2Members));

		// Add g2 to g1
		Member g2AsMember = new Member();
		g2AsMember.type = Member.Type.group;
		g2AsMember.uid = item2.uid;
		groupStore.addGroupsMembers(item1, Arrays.asList(item2));
		g1UsersMembers.add(g2Members.get(0));

		// Add an external user to g3
		List<MemberWithItem> g3Members = getExternalUserMember(1);
		groupStore.addExternalUsersMembers(item3, membersToList(g3Members));

		// Add g3 to g1
		Member g3AsMember = new Member();
		g3AsMember.type = Member.Type.group;
		g3AsMember.uid = item3.uid;
		groupStore.addGroupsMembers(item1, Arrays.asList(item3));
		g1UsersMembers.add(g3Members.get(0));
		//g1UsersMembers.addAll(g3Members);

		// Add user member to g4
		List<MemberWithItem> g4Members = getUserMember(1);
		groupStore.addUsersMembers(item4, membersToList(g4Members));

		// Add g4 to g2 and g3
		Member g4AsMember = new Member();
		g4AsMember.type = Member.Type.group;
		g4AsMember.uid = item4.uid;
		groupStore.addGroupsMembers(item2, Arrays.asList(item4));
		groupStore.addGroupsMembers(item3, Arrays.asList(item4));
		g1UsersMembers.add(g4Members.get(0));

		compareMembers(g1UsersMembers, groupStore.getFlatUsersMembers(item1));

		// Remove user from g2
		groupStore.removeUsersMembers(item2, Arrays.asList(g2Members.get(0).item.id));

		Iterator<Member> it = g1UsersMembers.iterator();
		while (it.hasNext()) {
			Member m = it.next();
			if (m.uid.equals(g2Members.get(0).uid)) {
				it.remove();
			}
		}

		compareMembers(g1UsersMembers, groupStore.getFlatUsersMembers(item1));
	}

	private Item initAndCreateGroup() throws SQLException {
		return initAndCreateGroup(null);
	}

	private Item initAndCreateGroup(String prefix) throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup(prefix);
		groupStore.create(item, g);

		return item;
	}

	@Test
	public void testGroupLoop() throws SQLException, ServerFault {
		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		Member g1AsMember = new Member();
		g1AsMember.type = Member.Type.group;
		g1AsMember.uid = item1.uid;
		groupStore.addGroupsMembers(item2, Arrays.asList(item1));

		Member g2AsMember = new Member();
		g2AsMember.type = Member.Type.group;
		g2AsMember.uid = item2.uid;

		try {
			groupStore.addGroupsMembers(item1, Arrays.asList(item2));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().toLowerCase().contains("group loop detected"));
		}
	}

	@Test
	public void testGetUserGroups() throws SQLException, ServerFault {
		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		List<MemberWithItem> g2Members = getUserMember(1);
		groupStore.addUsersMembers(item2, membersToList(g2Members));

		Member g2AsMember = new Member();
		g2AsMember.type = Member.Type.group;
		g2AsMember.uid = item2.uid;
		groupStore.addGroupsMembers(item1, Arrays.asList(item2));

		Item userItem = domainItemStore.get(g2Members.get(0).uid);
		List<String> userGroups = groupStore.getUserGroups(userContainer, userItem);
		
		assertEquals(2, userGroups.size());
		int count = 0;
		for (String userGroup : userGroups) {
			if (userGroup.equals(item1.uid) || userGroup.equals(item2.uid)) {
				count++;
			}
		}

		assertEquals(2, count);
	}

	@Test
	public void testAddMeToMyself() throws SQLException, ServerFault {
		Item item = initAndCreateGroup();
		Member gAsMember = new Member();
		gAsMember.type = Member.Type.group;
		gAsMember.uid = item.uid;

		try {
			groupStore.addGroupsMembers(item, Arrays.asList(item));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().toLowerCase().contains("group loop detected"));
		}
	}

	@Test
	public void testSearchByName() throws SQLException {
		Item g1Item = initAndCreateGroup("aa");

		Group created = groupStore.get(g1Item);

		String found = groupStore.byName(created.name);
		assertNotNull(found);

		found = groupStore.byName("osef");
		assertNull(found);
	}

	@Test
	public void testCreateWithSameName() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		String item2Uid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(item2Uid, null));
		Item item2 = domainItemStore.get(item2Uid);

		Group g2 = getDefaultGroup();
		g2.name = g.name;
		try {
			groupStore.create(item2, g2);
			fail("Test must thrown an exception");
		} catch (SQLException sqle) {
			assertTrue(sqle.getMessage().toLowerCase().contains("duplicate key")
					&& sqle.getMessage().toLowerCase().contains("t_group_container_id_name_key"));
		}
	}

	@Test
	public void testUpdateWithSameName() throws SQLException {
		Item g1Item = initAndCreateGroup();
		Group g1 = groupStore.get(g1Item);
		Item g2Item = initAndCreateGroup();
		Group g2 = groupStore.get(g2Item);

		g2.name = g1.name;
		try {
			groupStore.update(g2Item, g2);
		} catch (SQLException sqle) {
			assertTrue(sqle.getMessage().toLowerCase().contains("duplicate key")
					&& sqle.getMessage().toLowerCase().contains("t_group_container_id_name_key"));
		}
	}

	@Test
	public void testNameAlreadyUsedDifferentNameSameContainerButNotMe() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		String itemUid2 = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid2, null));
		Item item2 = domainItemStore.get(itemUid2);

		Group g2 = getDefaultGroup();
		assertFalse(groupStore.nameAlreadyUsed(item2.id, g2));
	}

	@Test
	public void testNameAlreadyUsedDifferentNameSameContainer() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group g2 = getDefaultGroup();
		assertFalse(groupStore.nameAlreadyUsed(g2));
	}

	@Test
	public void testNameAlreadyUsedSameNameSameContainer() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		assertTrue(groupStore.nameAlreadyUsed(g));
	}

	@Test
	public void testNameAlreadyUsedSameNameSameContainerCaseInsensitive() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		g.name = g.name.toUpperCase();
		assertTrue(groupStore.nameAlreadyUsed(g));
	}

	@Test
	public void testNameAlreadyUsedSameNameSameContainerButNotMe() throws SQLException {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		assertFalse(groupStore.nameAlreadyUsed(item.id, g));
	}

	@Test
	public void testNameAlreadyUsedSameNameDifferentContainer() throws SQLException {
		String containerId = "fake_" + System.nanoTime() + ".fr";
		Container domain = Container.create(containerId, "domain", containerId, "me", true);
		domain = containerStore.create(domain);
		GroupStore fakeGroupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), domain);

		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		fakeGroupStore.create(item, g);

		assertFalse(groupStore.nameAlreadyUsed(g));
	}

	@Test
	public void testGetParentsNoParent() throws SQLException, ServerFault {
		Item item1 = initAndCreateGroup();

		List<String> parents = groupStore.getParents(item1);
		assertNotNull(parents);
		assertEquals(0, parents.size());
	}

	@Test
	public void testGetParents() throws SQLException, ServerFault {
		Item item1 = initAndCreateGroup();
		Item item2 = initAndCreateGroup();

		Member g2AsMember = new Member();
		g2AsMember.type = Member.Type.group;
		g2AsMember.uid = item2.uid;
		groupStore.addGroupsMembers(item1, Arrays.asList(item2));

		assertEquals(0, groupStore.getParents(item1).size());
		assertEquals(1, groupStore.getParents(item2).size());
		assertEquals(item1.uid, groupStore.getParents(item2).get(0));
	}

	@Test
	public void testCustomProperties() throws Exception {
		String itemUid = UUID.randomUUID().toString();
		domainItemStore.create(Item.create(itemUid, null));
		Item item = domainItemStore.get(itemUid);

		Group g = getDefaultGroup();
		groupStore.create(item, g);

		Group created = groupStore.get(item);
		assertEquals(0, created.properties.size());

		Map<String, String> properties = new HashMap<String, String>();
		g.properties = properties;
		groupStore.update(item, g);
		created = groupStore.get(item);
		assertEquals(0, created.properties.size());

		properties.put("wat", "da funk");
		g.properties = properties;
		groupStore.update(item, g);
		created = groupStore.get(item);
		assertEquals(1, created.properties.size());
		assertEquals("da funk", created.properties.get("wat"));
	}
}
