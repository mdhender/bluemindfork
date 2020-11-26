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
package net.bluemind.system.ldap.importation.internal.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.user.api.User;

public class CoreServicesTest implements ICoreServices {
	public int userSetPhoto = 0;
	public int userDeletePhoto = 0;

	// Already exists on server
	public HashMap<String, ItemValue<Group>> groups = new HashMap<>();
	private HashMap<String, Set<Member>> groupMembers = new HashMap<>();
	private Map<String, Set<ItemValue<Group>>> userMemberOf = new HashMap<>();
	public Set<String> memberUpdateToInternal = new HashSet<>();
	public Set<String> memberUpdateToExternal = new HashSet<>();

	// Created during this run
	public Map<String, ItemValue<Group>> createdGroups = new HashMap<>();
	public Map<String, ItemValue<Group>> updatedGroups = new HashMap<>();

	// Already exists on server
	public HashMap<String, ItemValue<User>> users = new HashMap<>();
	public HashMap<String, MailFilter> usersMailfilters = new HashMap<>();
	// Created during this run
	public Map<String, ItemValue<User>> createdUsers = new HashMap<>();
	public Map<String, ItemValue<User>> updatedUsers = new HashMap<>();
	public Map<String, MailFilter> mailfiltersSet = new HashMap<>();
	public Map<String, Integer> quotaSet = new HashMap<>();

	public Set<String> existingGroupsExtIds = new HashSet<>();
	public Set<String> existingUsersExtIds = new HashSet<>();
	public Set<String> deletedGroupUids = new HashSet<>();
	public Set<String> suspendedUserUids = new HashSet<>();

	public HashMap<String, List<Member>> groupMembersToRemove = new HashMap<>();
	public HashMap<String, List<Member>> groupMembersToAdd = new HashMap<>();

	public CoreServicesTest() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getUserStats()
	 */
	@Override
	public Map<String, String> getUserStats() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getGroupStats()
	 */
	@Override
	public Map<String, String> getGroupStats() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * deleteGroup (java.lang.String)
	 */
	@Override
	public void deleteGroup(String deletedGroupUid) throws ServerFault {
		deletedGroupUids.add(deletedGroupUid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * createGroup (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void createGroup(ItemValue<Group> group) throws ServerFault {
		createdGroups.put(group.uid, group);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * updateGroup (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void updateGroup(ItemValue<Group> group) throws ServerFault {
		updatedGroups.put(group.uid, group);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * deleteUser (java.lang.String)
	 */
	@Override
	public void suspendUser(ItemValue<User> user) throws ServerFault {
		suspendedUserUids.add(user.uid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * createUser (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void createUser(ItemValue<User> user) throws ServerFault {
		createdUsers.put(user.uid, user);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * updateUser (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void updateUser(ItemValue<User> user) throws ServerFault {
		updatedUsers.put(user.uid, user);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getAllGroupItems()
	 */
	@Override
	public List<String> getImportedGroupsExtId() throws ServerFault {
		return new ArrayList<>(existingGroupsExtIds);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getAllUserItems()
	 */
	@Override
	public List<String> getImportedUsersExtId() throws ServerFault {
		return new ArrayList<>(existingUsersExtIds);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getMailboxFilter(java.lang.String)
	 */
	@Override
	public MailFilter getMailboxFilter(String uid) throws ServerFault {
		return usersMailfilters.get(uid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * setMailboxFilter(java.lang.String, net.bluemind.mailbox.api.MailFilter)
	 */
	@Override
	public void setMailboxFilter(String mailboxUid, MailFilter filter) throws ServerFault {
		mailfiltersSet.put(mailboxUid, filter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getGroupComplete(java.lang.String)
	 */
	@Override
	public ItemValue<Group> getGroupByExtId(String extId) throws ServerFault {
		for (ItemValue<Group> g : groups.values()) {
			if (g.externalId.equals(extId)) {
				return g;
			}
		}

		for (ItemValue<Group> g : createdGroups.values()) {
			if (g.externalId.equals(extId)) {
				return g;
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getGroupMembers(java.lang.String)
	 */
	@Override
	public List<Member> getGroupMembers(String uid) throws ServerFault {
		if (!groupMembers.containsKey(uid) || groupMembers.get(uid) == null) {
			return Collections.emptyList();
		}

		return new ArrayList<>(groupMembers.get(uid));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * removeMembers(java.lang.String, java.util.List)
	 */
	@Override
	public void removeMembers(String uid, List<Member> membersToRemove) throws ServerFault {
		if (groupMembersToRemove.containsKey(uid)) {
			groupMembersToRemove.get(uid).addAll(membersToRemove);
		} else {
			groupMembersToRemove.put(uid, new ArrayList<>());
			groupMembersToRemove.get(uid).addAll(membersToRemove);
		}

		membersToRemove.stream().filter(m -> m.type == Member.Type.user).forEach(m -> {
			removeUserFromGroup(m.uid, uid);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * addMembers (java.lang.String, java.util.List)
	 */
	@Override
	public void addMembers(String uid, List<Member> membersToAdd) throws ServerFault {
		if (membersToAdd.size() == 0) {
			return;
		}

		ArrayList<Member> toAdd = new ArrayList<>(membersToAdd);
		if (groupMembersToAdd.containsKey(uid)) {
			toAdd.addAll(groupMembersToAdd.get(uid));
		}

		groupMembersToAdd.put(uid, toAdd);

		membersToAdd.stream().filter(m -> m.type == Member.Type.user).forEach(m -> {
			addUserToGroups(m.uid, Arrays.asList(ItemValue.create(Item.create(uid, null), new Group())));
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getUserComplete(java.lang.String)
	 */
	@Override
	public ItemValue<User> getUserByExtId(String extId) throws ServerFault {
		for (ItemValue<User> u : users.values()) {
			if (u.externalId.equals(extId)) {
				return u;
			}
		}

		for (ItemValue<User> u : createdUsers.values()) {
			if (u.externalId.equals(extId)) {
				return u;
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * memberOf (java.lang.String)
	 */
	@Override
	public List<ItemValue<Group>> memberOf(String uid) throws ServerFault {
		Set<ItemValue<Group>> groups = userMemberOf.get(uid);

		if (groups == null) {
			return Collections.emptyList();
		}

		return new ArrayList<>(groups);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * userExternalId(java.lang.String)
	 */
	@Override
	public String userExternalId(String uid) throws ServerFault {
		if (users.containsKey(uid)) {
			return users.get(uid).externalId;
		}

		if (createdUsers.containsKey(uid)) {
			return createdUsers.get(uid).externalId;
		}

		if (updatedUsers.containsKey(uid)) {
			return updatedUsers.get(uid).externalId;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * groupExternalId(java.lang.String)
	 */
	@Override
	public String groupExternalId(String uid) throws ServerFault {
		if (groups.containsKey(uid)) {
			return groups.get(uid).externalId;
		}

		if (createdGroups.containsKey(uid)) {
			return createdGroups.get(uid).externalId;
		}

		if (updatedGroups.containsKey(uid)) {
			return updatedGroups.get(uid).externalId;
		}

		return null;
	}

	@Override
	public void userSetPhoto(String uid, byte[] photo) throws ServerFault {
		userSetPhoto++;
	}

	@Override
	public void userDeletePhoto(String uid) throws ServerFault {
		userDeletePhoto++;
	}

	public void addExistingUser(ItemValue<User> user) {
		existingUsersExtIds.add(user.externalId);
		users.put(user.uid, user);
		usersMailfilters.put(user.uid, new MailFilter());
	}

	public void addUserToGroups(String userUid, List<ItemValue<Group>> groups) {
		userMemberOf.put(userUid, new HashSet<>(groups));

		Member userAsMember = new Member();
		userAsMember.type = Member.Type.user;
		userAsMember.uid = userUid;

		for (ItemValue<Group> group : groups) {
			if (!groupMembers.containsKey(group.uid)) {
				groupMembers.put(group.uid, new HashSet<>());
			}

			groupMembers.get(group.uid).add(userAsMember);
		}
	}

	public void addGroupToGroup(String childGroupUid, ItemValue<Group> parentGroup) {
		Member groupAsMember = new Member();
		groupAsMember.type = Member.Type.group;
		groupAsMember.uid = childGroupUid;

		if (!groupMembers.containsKey(parentGroup.uid)) {
			groupMembers.put(parentGroup.uid, new HashSet<>());
		}

		groupMembers.get(parentGroup.uid).add(groupAsMember);
	}

	private void removeUserFromGroup(String userUid, String groupUid) {
		if (userMemberOf.containsKey(userUid)) {
			userMemberOf.put(userUid, userMemberOf.get(userUid).stream().filter(group -> !group.uid.equals(groupUid))
					.collect(Collectors.toSet()));
		}

		if (groupMembers.containsKey(groupUid)) {
			groupMembers.get(groupUid).remove(Member.user(userUid));
		}
	}

	@Override
	public void setMailboxQuota(String uid, int mailboxQuota) {
		quotaSet.put(uid, mailboxQuota);
	}

	@Override
	public void setUserMailRouting(Routing routing, String userUid) {
		if (routing == Routing.internal) {
			memberUpdateToInternal.add(userUid);
			return;
		}

		if (routing == Routing.external) {
			memberUpdateToExternal.add(userUid);
			return;
		}
	}

	@Override
	public ItemValue<Group> getGroupByName(String name) {
		return groups.entrySet().stream().filter(entry -> entry.getValue().value.name.equals(name)).findFirst()
				.map(Entry::getValue).orElse(null);
	}
}
