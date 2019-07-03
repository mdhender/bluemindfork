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
package net.bluemind.system.importation.commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class CoreServices implements ICoreServices {
	private class ImportStats {
		private int created = 0;
		private int updated = 0;
		private int suspended = 0;
		private int deleted = 0;
	}

	private ImportStats userStats;
	private ImportStats groupStats;
	private IUser userService;
	private IMailboxes mailboxService;
	private IGroup groupService;

	private Map<String, String> userUidExtId = new HashMap<>();
	private Map<String, String> groupUidExtId = new HashMap<>();

	public static ICoreServices build(String domainUid) throws ServerFault {
		if (domainUid == null || domainUid.trim().isEmpty()) {
			throw new IllegalArgumentException();
		}

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IUser userService = provider.instance(IUser.class, domainUid);
		IMailboxes mailboxService = provider.instance(IMailboxes.class, domainUid);

		IGroup groupService = provider.instance(IGroup.class, domainUid);

		return new CoreServices(userService, mailboxService, groupService);
	}

	/**
	 * @param userContainerManagement
	 * @param userService
	 * @param mailboxService
	 * @param groupContainerManagement
	 * @param groupService
	 */
	private CoreServices(IUser userService, IMailboxes mailboxService, IGroup groupService) {
		this.userService = userService;
		this.mailboxService = mailboxService;
		this.groupService = groupService;

		this.userStats = new ImportStats();
		this.groupStats = new ImportStats();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getUserStats()
	 */
	@Override
	public Map<String, String> getUserStats() {
		return ImmutableMap.of("en",
				userStats.created + " users created, " + userStats.updated + " users updated, " + userStats.suspended
						+ " users suspended, " + userStats.deleted + " users deleted",
				"fr",
				userStats.created + " utilisateurs créés, " + userStats.updated + " utilisateurs mis à jour, "
						+ userStats.suspended + " utilisateurs suspendus, " + userStats.deleted
						+ " utilisateurs supprimés");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getGroupStats()
	 */
	@Override
	public Map<String, String> getGroupStats() {
		return ImmutableMap.of("en",
				groupStats.created + " groups created, " + groupStats.updated + " groups updated, " + groupStats.deleted
						+ " groups deleted",
				"fr", groupStats.created + " groupes créés, " + groupStats.updated + " groupes mis à jour, "
						+ groupStats.deleted + " groupes supprimés");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * deleteGroup (java.lang.String)
	 */
	@Override
	public void deleteGroup(String groupUid) throws ServerFault {
		groupService.delete(groupUid);
		groupStats.deleted++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * createGroup (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void createGroup(ItemValue<Group> group) throws ServerFault {
		groupService.createWithExtId(group.uid, group.externalId, group.value);
		groupStats.created++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * updateGroup (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void updateGroup(ItemValue<Group> group) throws ServerFault {
		groupService.update(group.uid, group.value);
		groupStats.updated++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * deleteUser (java.lang.String)
	 */
	@Override
	public void suspendUser(ItemValue<User> user) throws ServerFault {
		user.value.archived = true;
		userService.update(user.uid, user.value);
		userStats.suspended++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * createUser (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void createUser(ItemValue<User> user) throws ServerFault {
		userService.createWithExtId(user.uid, user.externalId, user.value);
		userStats.created++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * updateUser (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	public void updateUser(ItemValue<User> user) throws ServerFault {
		userService.update(user.uid, user.value);
		userStats.updated++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getAllGroupItems()
	 */
	@Override
	public List<String> getImportedGroupsExtId() throws ServerFault {
		List<String> groupsExtIds = new ArrayList<>();

		for (String groupUid : groupService.allUids()) {
			if (groupUidExtId.containsKey(groupUid)) {
				groupsExtIds.add(groupUidExtId.get(groupUid));
				continue;
			}

			ItemValue<Group> group = groupService.getComplete(groupUid);
			if (group.externalId == null || group.externalId.isEmpty()) {
				continue;
			}

			groupsExtIds.add(group.externalId);
			groupUidExtId.put(groupUid, group.externalId);
		}

		return groupsExtIds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getAllUserItems()
	 */
	@Override
	public List<String> getImportedUsersExtId() throws ServerFault {
		List<String> usersExtIds = new ArrayList<>();

		for (String userUid : userService.allUids()) {
			if (userUidExtId.containsKey(userUid)) {
				usersExtIds.add(userUidExtId.get(userUid));
				continue;
			}

			ItemValue<User> user = userService.getComplete(userUid);
			if (user.externalId == null || user.externalId.isEmpty()) {
				continue;
			}

			usersExtIds.add(user.externalId);
			userUidExtId.put(userUid, user.externalId);
		}

		return usersExtIds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getMailboxFilter(java.lang.String)
	 */
	@Override
	public MailFilter getMailboxFilter(String uuid) throws ServerFault {
		return mailboxService.getMailboxFilter(uuid);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * setMailboxFilter(java.lang.String, net.bluemind.mailbox.api.MailFilter)
	 */
	@Override
	public void setMailboxFilter(String mailboxUid, MailFilter filter) throws ServerFault {
		mailboxService.setMailboxFilter(mailboxUid, filter);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getGroupComplete(java.lang.String)
	 */
	@Override
	public ItemValue<Group> getGroupByExtId(String extId) throws ServerFault {
		return groupService.getByExtId(extId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getGroupMembers(java.lang.String)
	 */
	@Override
	public List<Member> getGroupMembers(String uid) throws ServerFault {
		return groupService.getMembers(uid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * removeMembers(java.lang.String, java.util.List)
	 */
	@Override
	public void removeMembers(String uid, List<Member> membersToRemove) throws ServerFault {
		groupService.remove(uid, membersToRemove);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * addMembers (java.lang.String, java.util.List)
	 */
	@Override
	public void addMembers(String uid, List<Member> membersToAdd) throws ServerFault {
		groupService.add(uid, membersToAdd);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * getUserComplete(java.lang.String)
	 */
	@Override
	public ItemValue<User> getUserByExtId(String extId) throws ServerFault {
		return userService.byExtId(extId);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * memberOf (java.lang.String)
	 */
	@Override
	public List<ItemValue<Group>> memberOf(String uid) throws ServerFault {
		return userService.memberOf(uid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * userExternalId(java.lang.String)
	 */
	@Override
	public String userExternalId(String uid) throws ServerFault {
		if (userUidExtId.containsKey(uid)) {
			return userUidExtId.get(uid);
		}

		ItemValue<User> user = userService.getComplete(uid);
		if (user == null) {
			return null;
		}

		userUidExtId.put(uid, user.externalId);
		return user.externalId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.importation.internal.tools.ICoreServices#
	 * groupExternalId(java.lang.String)
	 */
	@Override
	public String groupExternalId(String uid) throws ServerFault {
		if (groupUidExtId.containsKey(uid)) {
			return groupUidExtId.get(uid);
		}

		ItemValue<Group> group = groupService.getComplete(uid);
		if (group == null) {
			return null;
		}

		groupUidExtId.put(uid, group.externalId);
		return group.externalId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.bluemind.system.importation.commons.ICoreServices#userSetPhoto(java.
	 * lang.String, byte[])
	 */
	@Override
	public void userSetPhoto(String uid, byte[] photo) throws ServerFault {
		userService.setPhoto(uid, photo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.bluemind.system.importation.commons.ICoreServices#userDeletePhoto(
	 * java.lang.String)
	 */
	@Override
	public void userDeletePhoto(String uid) throws ServerFault {
		userService.deletePhoto(uid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.bluemind.system.importation.commons.ICoreServices#setMailboxQuota(
	 * java.lang.String, int)
	 */
	@Override
	public void setMailboxQuota(String uid, int mailboxQuota) {
		ItemValue<Mailbox> mailbox = mailboxService.getComplete(uid);

		if (mailbox == null) {
			throw new ServerFault("Unable to find mailbox uid: " + uid);
		}

		if (mailbox.value.quota == null) {
			mailbox.value.quota = 0;
		}

		if (mailbox.value.quota != mailboxQuota) {
			mailbox.value.quota = mailboxQuota;
			mailboxService.update(uid, mailbox.value);
		}
	}
}
