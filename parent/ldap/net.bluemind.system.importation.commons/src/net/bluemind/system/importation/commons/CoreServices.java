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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
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
		private int unsuspended = 0;
		private int deleted = 0;
	}

	private class ExtIdMapper<T> {
		public final String uid;
		public final ItemValue<T> entity;

		public ExtIdMapper(String uid, ItemValue<T> entity) {
			this.uid = uid;
			this.entity = entity;
		}

		public void logInvalid(ImportLogger importLogger) {
			if (entity != null) {
				return;
			}

			HashMap<String, String> messages = new HashMap<>(2);
			messages.put("en", String.format("Unable to get entity UID: %s", uid));
			messages.put("fr", String.format("Impossible d'obtenir l'entité d'UID: %s", uid));
			importLogger.error(messages);
		}
	}

	private final ImportStats userStats;
	private final ImportStats groupStats;

	private final IUser userService;
	private final IMailboxes mailboxService;
	private final IGroup groupService;

	public static ICoreServices build(String domainUid) {
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
	 * @param userService
	 * @param mailboxService
	 * @param groupService
	 */
	private CoreServices(IUser userService, IMailboxes mailboxService, IGroup groupService) {
		this.userService = userService;
		this.mailboxService = mailboxService;
		this.groupService = groupService;

		this.userStats = new ImportStats();
		this.groupStats = new ImportStats();
	}

	@Override
	public Map<String, String> getUserStats() {
		return ImmutableMap.of(
				"en", userStats.created + " users created, " + userStats.updated + " users updated, "
						+ userStats.suspended + " users suspended, " + userStats.unsuspended + " users unsuspended",
				"fr",
				userStats.created + " utilisateurs créés, " + userStats.updated + " utilisateurs mis à jour, "
						+ userStats.suspended + " utilisateurs suspendus, " + userStats.unsuspended
						+ " utilisateurs ré-activés");
	}

	@Override
	public Map<String, String> getGroupStats() {
		return ImmutableMap.of("en",
				groupStats.created + " groups created, " + groupStats.updated + " groups updated, " + groupStats.deleted
						+ " groups deleted",
				"fr", groupStats.created + " groupes créés, " + groupStats.updated + " groupes mis à jour, "
						+ groupStats.deleted + " groupes supprimés");
	}

	@Override
	public void deleteGroup(String groupUid) {
		groupService.delete(groupUid);
		groupStats.deleted++;
	}

	@Override
	public void createGroup(ItemValue<Group> group) {
		groupService.createWithExtId(group.uid, group.externalId, group.value);
		groupStats.created++;
	}

	@Override
	public void updateGroup(ItemValue<Group> group) {
		groupService.update(group.uid, group.value);
		groupStats.updated++;
	}

	@Override
	public void suspendUser(ItemValue<User> user) {
		user.value.archived = true;
		userService.update(user.uid, user.value);
		userStats.suspended++;
	}

	@Override
	public void unsuspendUser(ItemValue<User> user) {
		user.value.archived = false;
		userService.update(user.uid, user.value);
		userStats.unsuspended++;
	}

	@Override
	public void createUser(ItemValue<User> user) {
		userService.createWithExtId(user.uid, user.externalId, user.value);
		userStats.created++;
	}

	@Override
	public void updateUser(ItemValue<User> user) {
		userService.update(user.uid, user.value);
		userStats.updated++;
	}

	@Override
	public Set<String> getImportedGroupsExtId(ImportLogger importLogger) {
		return groupService.allUids().stream().map(uid -> new ExtIdMapper<Group>(uid, groupService.getComplete(uid)))
				.peek(gE -> gE.logInvalid(importLogger)).filter(gE -> gE.entity != null).map(gE -> gE.entity.externalId)
				.filter(extUid -> !Strings.isNullOrEmpty(extUid)).collect(Collectors.toSet());
	}

	@Override
	public ExtUidState getUsersExtIdByState(ImportLogger importLogger) {
		return new ExtUidState(
				userService.allUids().stream().map(uid -> new ExtIdMapper<User>(uid, userService.getComplete(uid)))
						.peek(uE -> uE.logInvalid(importLogger))
						.filter(uE -> uE.entity != null && !Strings.isNullOrEmpty(uE.entity.externalId))
						.map(uE -> uE.entity).collect(Collectors.partitioningBy(u -> u.value.archived,
								Collectors.mapping(u1 -> u1.externalId, Collectors.toSet()))));
	}

	@Override
	public MailFilter getMailboxFilter(String uuid) {
		return mailboxService.getMailboxFilter(uuid);

	}

	@Override
	public void setMailboxFilter(String mailboxUid, MailFilter filter) {
		mailboxService.setMailboxFilter(mailboxUid, filter);

	}

	@Override
	public ItemValue<Group> getGroupByExtId(String extId) {
		return groupService.getByExtId(extId);
	}

	@Override
	public ItemValue<Group> getGroupByName(String name) {
		return groupService.byName(name);
	}

	@Override
	public List<Member> getGroupMembers(String uid) {
		return groupService.getMembers(uid);
	}

	@Override
	public void removeMembers(String uid, List<Member> membersToRemove) {
		groupService.remove(uid, membersToRemove);

	}

	@Override
	public void addMembers(String uid, List<Member> membersToAdd) {
		groupService.add(uid, membersToAdd);

	}

	@Override
	public ItemValue<User> getUserByExtId(String extId) {
		return userService.byExtId(extId);

	}

	@Override
	public List<ItemValue<Group>> memberOf(String uid) {
		return userService.memberOf(uid);
	}

	@Override
	public void userSetPhoto(String uid, byte[] photo) {
		userService.setPhoto(uid, photo);
	}

	@Override
	public void userDeletePhoto(String uid) {
		userService.deletePhoto(uid);
	}

	@Override
	public void setUserMailRouting(Routing routing, String userUid) {
		ItemValue<User> user = userService.getComplete(userUid);

		if (user.value.routing == Routing.none || user.value.routing == routing) {
			return;
		}

		user.value.routing = routing;
		userService.update(user.uid, user.value);
	}
}
