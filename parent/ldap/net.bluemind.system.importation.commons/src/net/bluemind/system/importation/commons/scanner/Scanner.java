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
package net.bluemind.system.importation.commons.scanner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.lib.ldap.GroupMemberAttribute;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.CoreServices;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.ICoreServices.ExtUidState;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.enhancer.GroupMembershipData;
import net.bluemind.system.importation.commons.enhancer.IScannerEnhancer;
import net.bluemind.system.importation.commons.exceptions.DirectoryConnectionFailed;
import net.bluemind.system.importation.commons.managers.GroupManager;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.i18n.Messages;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.user.api.User;

public abstract class Scanner {
	private enum SuspendExcuse {
		NOT_FOUND, SUSPEND_IN_DIRECTORY;
	}

	@SuppressWarnings("serial")
	private class EntryNotFound extends Exception {
		public EntryNotFound() {
			super("DN not found in directory");
		}
	}

	private class EntryInfos {
		public final Dn dn;
		public final boolean suspended;

		public EntryInfos(Dn dn, boolean suspended) {
			this.dn = dn;
			this.suspended = suspended;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Scanner.class);

	protected final ImportLogger importLogger;
	protected final ItemValue<Domain> domain;
	protected final ICoreServices coreService;
	protected LdapConProxy ldapCon;

	protected Scanner(ImportLogger importLogger, ItemValue<Domain> domain) {
		this.importLogger = importLogger;
		this.domain = domain;
		this.coreService = CoreServices.build(domain.uid);
	}

	protected Scanner(ImportLogger importLogger, ICoreServices coreService, ItemValue<Domain> domain) {
		this.importLogger = importLogger;
		this.domain = domain;
		this.coreService = coreService;
	}

	/**
	 * Scan directory for updated entries since lastUpdate (Incremental mode)
	 * 
	 * @param lastUpdate
	 */
	public void scan() {
		if (getParameter().lastUpdate.isPresent()) {
			logger.info("Incremental {} scan for: {}, modified since: {}", getKind(), getParameter(),
					getParameter().lastUpdate.get());
		} else {
			logger.info("Global {} scan for: {}", getKind(), getParameter());
		}

		logger.info("Import {} directory using scanner: {}", getKind(), this.getClass().getSimpleName());
		importLogger.info(Messages.importWithScanner(getKind(), this.getClass().getSimpleName()));

		try {
			logger.info("Doing before import operations from {}", getKind());
			beforeImport();

			ldapCon = getConnection();

			setupSplitGroup();

			logger.info("Manage BM users suspend state from {}", getKind());
			Set<Dn> directoryExistingUsersDn = manageUsersArchiveStatus();

			logger.info("Deleting groups from BM which are removed in {}", getKind());
			Set<Dn> directoryExistingGroupsDn = deletedGroups();

			logger.info("Updating or creating BM users from {}", getKind());
			scanUsers(directoryExistingUsersDn);

			logger.info("Updating or creating BM groups from {}", getKind());
			scanGroups(directoryExistingGroupsDn);

			logger.info("Doing after import operations from {}", getKind());
			afterImport();
		} catch (DirectoryConnectionFailed dcf) {
			logger.error(dcf.getMessage(), dcf);
			importLogger.error(Messages.directoriesConnectionFailed());
		} catch (ServerFault sf) {
			importLogger.reportException(sf);
			throw sf;
		} finally {
			if (ldapCon != null) {
				try {
					ldapCon.close();
				} catch (IOException e) {
					logger.warn("Closing directory connexion failed!", e);
				}
			}

			reset();

			importLogger.info(coreService.getUserStats());
			importLogger.info(coreService.getGroupStats());
			importLogger.logStatus();
		}
	}

	protected abstract void setupSplitGroup();

	protected abstract void reset();

	protected abstract String getKind();

	protected abstract Parameters getParameter();

	protected abstract LdapConProxy getConnection();

	protected abstract Optional<UuidMapper> getUuidMapperFromExtId(String externalId);

	protected abstract Set<UuidMapper> uuidMapperFromExtIds(Set<String> importedUsersExtId);

	protected abstract Optional<UuidMapper> getUuidMapperFromEntry(Entry entry);

	protected abstract Optional<UserManager> getUserManager(Entry entry);

	protected abstract Optional<GroupManager> getGroupManager(Entry entry);

	protected abstract GroupMemberAttribute getGroupMembersAttributeName();

	protected abstract boolean doNotImportUser(Entry entry);

	protected abstract boolean doNotImportGroup(Entry entry);

	protected abstract PagedSearchResult allUsersFromDirectory() throws LdapException;

	protected abstract PagedSearchResult allGroupsFromDirectory() throws LdapException;

	protected abstract PagedSearchResult usersDnByLastModification(Optional<String> lastUpdate) throws LdapException;

	protected abstract PagedSearchResult groupsDnByLastModification(Optional<String> lastUpdate) throws LdapException;

	protected abstract Optional<Entry> getUserFromDn(Dn dn) throws LdapException;

	protected abstract Optional<Entry> getGroupFromDn(Dn dn) throws LdapException;

	protected abstract void manageUserGroups(UserManager userManager);

	protected abstract Optional<Dn> getMemberDnFromLogin(String userLogin);

	protected abstract List<IScannerEnhancer> getScannerEnhancerHooks();

	protected abstract boolean isSuspended(Entry entry);

	/**
	 * Manage directory deleted users (suspended - AD only, removed - deleted or
	 * moved out of filter scope) or in incremental mode, added directory users in
	 * filter scope without update since last successful incremental import
	 * 
	 * @return Users DN existing in directory and <b>not found</b> in BlueMind. They
	 *         must be created in incremental mode even if not updated in directory
	 *         since last incremental update
	 */
	private Set<Dn> manageUsersArchiveStatus() {
		Map<UuidMapper, EntryInfos> directoryEntryInfosByUuid = new HashMap<>();
		try (PagedSearchResult cursor = allUsersFromDirectory()) {
			while (cursor.next()) {
				Response response = cursor.get();
				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				Entry entry = ((SearchResultEntry) response).getEntry();
				getUuidMapperFromEntry(entry).ifPresent(extUid -> directoryEntryInfosByUuid.put(extUid,
						new EntryInfos(entry.getDn(), isSuspended(entry))));
			}
		} catch (LdapException | CursorException | LdapSearchException e) {
			throw new ServerFault(e);
		}

		ExtUidState bmExtUidState = coreService.getUsersExtIdByState(importLogger);
		Set<UuidMapper> active = uuidMapperFromExtIds(bmExtUidState.active);
		Set<UuidMapper> suspended = uuidMapperFromExtIds(bmExtUidState.suspended);

		updateUsersArchiveStatus(directoryEntryInfosByUuid, active, suspended);

		return Sets
				.difference(directoryEntryInfosByUuid.keySet(),
						Stream.concat(active.stream(), suspended.stream()).collect(Collectors.toSet()))
				.stream().map(directoryEntryInfosByUuid::get).map(ei -> ei.dn).collect(Collectors.toSet());
	}

	/**
	 * Manage user archive flag.
	 * 
	 * Needed to manage archive status for users matching one of this rules:
	 * <ul>
	 * <li>removed in directory (global or incremental)</li>
	 * <li>moved out of filter scope (global or incremental)</li>
	 * <li>moved in filter scope without update since last successful incremental
	 * import</li>
	 * </ul>
	 * 
	 * @param directoryEntryInfosByUuid
	 * @param active
	 * @param suspended
	 */
	private void updateUsersArchiveStatus(Map<UuidMapper, EntryInfos> directoryEntryInfosByUuid, Set<UuidMapper> active,
			Set<UuidMapper> suspended) {
		// Suspend BlueMind users who are active in BlueMind and suspend in directory
		Sets.intersection(active,
				directoryEntryInfosByUuid.entrySet().stream().filter(es -> es.getValue().suspended)
						.map(es -> es.getKey()).collect(Collectors.toSet()))
				.stream().map(UuidMapper::getExtId)
				.forEach(extId -> suspendUser(SuspendExcuse.SUSPEND_IN_DIRECTORY, extId));
		// Suspend BlueMind users who are active in BlueMind and not found in directory
		Sets.difference(active, directoryEntryInfosByUuid.keySet()).stream().map(UuidMapper::getExtId)
				.forEach(extId -> suspendUser(SuspendExcuse.NOT_FOUND, extId));

		// Unsuspend BlueMind users who are suspended in BlueMind and active in
		// directory
		Sets.intersection(suspended,
				directoryEntryInfosByUuid.entrySet().stream().filter(es -> !es.getValue().suspended)
						.map(es -> es.getKey()).collect(Collectors.toSet()))
				.stream().map(UuidMapper::getExtId).forEach(this::unsuspendUser);
	}

	private void unsuspendUser(String extId) {
		try {
			ItemValue<User> user = coreService.getUserByExtId(extId);

			if (user != null) {
				if (!user.value.archived) {
					return;
				}

				importLogger.info(Messages.unsuspendUser(user));
				coreService.unsuspendUser(user);
			} else {
				importLogger.warning(Messages.userNotFound(extId));
			}
		} catch (ServerFault e) {
			importLogger.error(Messages.unsuspendingBMUserFailed(extId, e));
		}
	}

	private void suspendUser(SuspendExcuse excuse, String extId) {
		try {
			ItemValue<User> user = coreService.getUserByExtId(extId);

			if (user != null) {
				if (user.value.archived) {
					return;
				}

				switch (excuse) {
				case NOT_FOUND:
					importLogger.info(Messages.suspenUserNotFound(user));
					break;
				case SUSPEND_IN_DIRECTORY:
					importLogger.info(Messages.suspenUserSuspendInDirectory(user));
					break;
				default:
					break;
				}
				coreService.suspendUser(user);
			} else {
				importLogger.warning(Messages.userNotFound(extId));
			}
		} catch (ServerFault e) {
			importLogger.error(Messages.suspendingBMUserFailed(extId, e));
		}
	}

	/**
	 * 
	 * @return Groups DN existing in directory and <b>not found</b> in BlueMind.
	 *         They must be created in incremental mode even if not updated in
	 *         directory since last incremental update
	 */
	private Set<Dn> deletedGroups() {
		Set<UuidMapper> bmExtUid = uuidMapperFromExtIds(coreService.getImportedGroupsExtId(importLogger));

		Map<UuidMapper, Dn> directoryDnByExtuid = new HashMap<>();
		try (PagedSearchResult cursor = allGroupsFromDirectory()) {
			while (cursor.next()) {
				Response response = cursor.get();
				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				Entry entry = ((SearchResultEntry) response).getEntry();
				getUuidMapperFromEntry(entry)
						.ifPresent(groupExtUid -> directoryDnByExtuid.put(groupExtUid, entry.getDn()));
			}
		} catch (LdapException | CursorException | LdapSearchException e) {
			throw new ServerFault(e);
		}

		SetView<UuidMapper> deletedExtUuids = Sets.difference(bmExtUid, directoryDnByExtuid.keySet());

		for (UuidMapper deletedExtUid : deletedExtUuids) {
			try {
				ItemValue<Group> group = coreService.getGroupByExtId(deletedExtUid.getExtId());

				if (group != null) {
					importLogger.info(Messages.deleteGroup(group));
					coreService.deleteGroup(group.uid);
				} else {
					importLogger.warning(Messages.deletedGroupNotFound(deletedExtUid.getExtId()));
				}
			} catch (ServerFault sf) {
				importLogger.error(Messages.failedToDeleteGroup(deletedExtUid.getExtId(), sf));
			}

			directoryDnByExtuid.remove(deletedExtUid);
		}

		return Sets.difference(directoryDnByExtuid.keySet(), bmExtUid).stream().map(directoryDnByExtuid::get)
				.collect(Collectors.toSet());
	}

	/**
	 * 
	 * @param directoryExistingGroupsDn Groups DN to scan even if not modified since
	 *                                  last import
	 */
	private void scanGroups(Set<Dn> directoryExistingGroupsDn) {
		Set<Dn> entriesDn = new HashSet<>();
		try (PagedSearchResult cursor = groupsDnByLastModification(getParameter().lastUpdate)) {
			while (cursor.next()) {
				Response response = cursor.get();
				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entriesDn.add(((SearchResultEntry) response).getObjectName());
			}
		} catch (CursorException | LdapException | LdapSearchException e) {
			throw new ServerFault(e);
		}

		Stream.concat(entriesDn.stream(), Sets.difference(directoryExistingGroupsDn, entriesDn).stream())
				.map(this::manageGroup).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet())
				.forEach(this::manageGroupMembers);
	}

	private Optional<GroupManager> manageGroup(Dn groupDn) {
		Entry entry = null;
		try {
			// modifyTimestamp and canonicalName are return only if explicitly requested by
			// some directory server
			entry = Optional.ofNullable(ldapCon.lookup(groupDn, "*", "+", getParameter().ldapDirectory.extIdAttribute,
					"modifyTimestamp", "canonicalName")).orElseThrow(() -> new EntryNotFound());
		} catch (EntryNotFound | LdapException le) {
			logger.error(String.format("%s: %s", groupDn.getName(), le.getMessage()), le);
			importLogger.error(Messages.failedLookupEntryDn(groupDn, le));
			return Optional.empty();
		}

		logger.info("Managing Ldap group: {}", entry.getDn().getName());

		return getGroupManager(entry).map(this::applyGroupManagerUpdates).orElse(Optional.empty());
	}

	private Optional<GroupManager> applyGroupManagerUpdates(GroupManager groupManager) {
		try {
			ItemValue<Group> currentGroup = coreService.getGroupByExtId(groupManager.getExternalId(importLogger));
			groupManager.update(importLogger, currentGroup);

			if (currentGroup == null) {
				importLogger.info(Messages.createGroup(groupManager.group.value.name));
				coreService.createGroup(groupManager.group);
			} else {
				importLogger.info(Messages.updateGroup(groupManager.group.value.name));
				coreService.updateGroup(groupManager.group);
			}

			return Optional.of(groupManager);
		} catch (Exception e) {
			logger.error("Fail to manage group: " + groupManager.entry.getDn().getName(), e);
			importLogger.error(Messages.failedToManageBMGroup(groupManager.entry, e));
		}

		return Optional.empty();
	}

	private void manageGroupMembers(GroupManager groupManager) {
		// Getting LDAP group users members
		Set<Member> ldapGroupMembers = getGroupMembers(groupManager);

		// Getting BM group users members
		Set<Member> bmGroupMembers = new HashSet<>(coreService.getGroupMembers(groupManager.group.uid));

		SetView<Member> membersToRemove = Sets.difference(bmGroupMembers, ldapGroupMembers);
		if (!membersToRemove.isEmpty()) {
			coreService.removeMembers(groupManager.group.uid, new ArrayList<>(membersToRemove));
			if (groupManager.isSplitDomainGroup(importLogger)) {
				membersToRemove.stream().filter(member -> member.type == Member.Type.user)
						.forEach(member -> coreService.setUserMailRouting(Routing.internal, member.uid));
			}
		}

		SetView<Member> membersToAdd = Sets.difference(ldapGroupMembers, bmGroupMembers);
		if (!membersToAdd.isEmpty()) {
			try {
				coreService.addMembers(groupManager.group.uid, new ArrayList<>(membersToAdd));
			} catch (ServerFault sf) {
				if (sf.getCode() == ErrorCode.INCLUSION_GROUP_LOOP) {
					logger.warn("Error on group {} update, add members cause group loop",
							groupManager.entry.getDn().getName());
					importLogger.warning(Messages.groupMemberAddLoop(groupManager.entry.getDn().getName()));
				}
			}

			if (groupManager.isSplitDomainGroup(importLogger)) {
				membersToAdd.stream().filter(member -> member.type == Member.Type.user)
						.forEach(member -> coreService.setUserMailRouting(Routing.external, member.uid));
			}
		}

		groupMembershipEnhancer(groupManager.group, membersToAdd, membersToRemove);
	}

	private void groupMembershipEnhancer(ItemValue<Group> group, SetView<Member> membersToAdd,
			SetView<Member> membersToRemove) {
		List<IScannerEnhancer> hooks = getScannerEnhancerHooks();

		if (hooks.isEmpty()) {
			return;
		}

		long timeBefore = System.currentTimeMillis();
		// Use dedicated directory connection for hook as connection parameters may be
		// altered by hook.
		try (LdapConProxy ldapCon = getConnection()) {
			for (IScannerEnhancer iee : hooks) {
				iee.groupMembershipUpdates(importLogger.withoutStatus(), getParameter(), domain, ldapCon,
						new GroupMembershipData(group, membersToAdd, membersToRemove));
			}
		} catch (IOException e) {
			logger.error("Directory connection error", e);
		}

		logger.info("Ending group {} ({}) member add/delete enhancement in {}ms", group.value.name, group.uid,
				System.currentTimeMillis() - timeBefore);
	}

	private Set<Member> getGroupMembers(GroupManager groupManager) {
		Set<String> groupMembers = groupManager.getGroupMembers(getGroupMembersAttributeName());

		Set<Member> members = new HashSet<>();

		Set<String> memberIgnored = new HashSet<>();
		for (String groupMember : groupMembers) {
			Optional<Dn> memberDn = getMemberDn(groupMember);
			if (!memberDn.isPresent()) {
				memberIgnored.add(groupMember);
				continue;
			}

			Optional<Member> member = getUserMember(memberDn.get());
			if (member.isPresent()) {
				members.add(member.get());
			} else {
				member = getGroupMember(memberDn.get());
				if (member.isPresent()) {
					members.add(member.get());
				} else {
					memberIgnored.add(groupMember);
				}
			}
		}

		if (!memberIgnored.isEmpty()) {
			importLogger
					.info(Messages.groupMembersNotFound(groupManager.entry.getDn().getName(), memberIgnored.size()));
			logger.warn("Members ignored for group {}: {}", groupManager.entry.getDn(), memberIgnored);
		}

		return members;
	}

	/**
	 * Convert groupMember to DN. Search for user DN using groupMember as login if
	 * groupMember is not a valid DN.
	 * 
	 * @param groupMember valid DN or user login
	 * @return
	 */
	private Optional<Dn> getMemberDn(String groupMember) {
		try {
			return Optional.of(new Dn(groupMember));
		} catch (LdapInvalidDnException e) {
			return getMemberDnFromLogin(groupMember);
		}
	}

	private Optional<Member> getUserMember(Dn userDn) {
		Entry entry = null;

		try {
			entry = getUserFromDn(userDn).orElse(null);
		} catch (LdapException le) {
			importLogger.error(Messages.groupMemberCheckFail(userDn.getName(), le));
		}

		if (entry == null) {
			return Optional.empty();
		}

		String extId = getUuidMapperFromEntry(entry).map(UuidMapper::getExtId).orElse(null);
		if (Strings.isNullOrEmpty(extId)) {
			return Optional.empty();
		}

		ItemValue<User> itemUser = coreService.getUserByExtId(extId);
		if (itemUser == null) {
			// User exists in directory but not in BlueMind
			// Try to import if matching filter - ignore incremental mode
			manageUser(entry.getDn());

			itemUser = coreService.getUserByExtId(extId);
			if (itemUser == null) {
				// User import failed...
				return Optional.empty();
			}
		}

		Member member = new Member();
		member.uid = itemUser.uid;
		member.type = Member.Type.user;

		return Optional.of(member);
	}

	private Optional<Member> getGroupMember(Dn groupDn) {
		Entry entry = null;

		try {
			entry = getGroupFromDn(groupDn).orElse(null);
		} catch (LdapException le) {
		}

		if (entry == null) {
			return Optional.empty();
		}

		String extId = getUuidMapperFromEntry(entry).map(UuidMapper::getExtId).orElse(null);
		if (Strings.isNullOrEmpty(extId)) {
			return Optional.empty();
		}

		ItemValue<Group> itemGroup = coreService.getGroupByExtId(extId);
		if (itemGroup == null) {
			// Group exists in directory but not in BlueMind
			// Try to import if matching filter - ignore incremental mode
			manageGroup(entry.getDn()).ifPresent(this::manageGroupMembers);

			itemGroup = coreService.getGroupByExtId(extId);
			if (itemGroup == null) {
				// Group import failed...
				return Optional.empty();
			}
		}

		Member member = new Member();
		member.uid = itemGroup.uid;
		member.type = Member.Type.group;

		return Optional.of(member);
	}

	/**
	 * 
	 * @param directoryExistingUsersDn User DN to scan even if not modified since
	 *                                 last import
	 */
	private void scanUsers(Set<Dn> directoryExistingUsersDn) {
		Set<Dn> entriesDn = new HashSet<>();
		try (PagedSearchResult cursor = usersDnByLastModification(getParameter().lastUpdate)) {
			while (cursor.next()) {
				Response response = cursor.get();
				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entriesDn.add(((SearchResultEntry) response).getObjectName());
			}
		} catch (CursorException | LdapException | LdapSearchException e) {
			throw new ServerFault(e);
		}

		Stream.concat(entriesDn.stream(), Sets.difference(directoryExistingUsersDn, entriesDn).stream())
				.forEach(this::manageUser);
	}

	private void manageUser(Dn userDn) {
		Entry entry = null;
		try {
			// modifyTimestamp and canonicalName are return only if explicitly requested by
			// some directory
			entry = Optional.ofNullable(ldapCon.lookup(userDn, "*", "+", getParameter().ldapDirectory.extIdAttribute,
					"modifyTimestamp", "canonicalName")).orElseThrow(() -> new EntryNotFound());
		} catch (EntryNotFound | LdapException le) {
			logger.warn("{}: {}", userDn.getName(), le.getMessage(), le);
			importLogger.warning(Messages.failedLookupEntryDn(userDn, le));
			return;
		}

		logger.info("Managing Ldap user: {}", entry.getDn().getName());

		getUserManager(entry).ifPresent(this::applyUserManagerUpdates);
	}

	private void applyUserManagerUpdates(UserManager userManager) {
		try {
			ItemValue<User> userItem = coreService.getUserByExtId(userManager.getExternalId(importLogger));
			MailFilter userMailFilter = null;
			if (userItem != null) {
				userMailFilter = coreService.getMailboxFilter(userItem.uid);
			}

			userManager.update(importLogger, userItem, userMailFilter);

			if (userManager.create) {
				importLogger.info(Messages.createUser(userManager.user.value.login));
				coreService.createUser(userManager.user);
			} else {
				importLogger.info(Messages.updateUser(userManager.user.value.login));
				coreService.updateUser(userManager.user);
			}

			userManager.getUpdatedMailFilter().ifPresent(mf -> coreService.setMailboxFilter(userManager.user.uid, mf));
		} catch (Exception e) {
			logger.error("Error on managing user DN: {}", userManager.entry.getDn().getName(), e);
			importLogger.error(Messages.manageUserFailed(userManager.entry, e));
			return;
		}

		manageUserPhoto(userManager);

		try {
			manageUserGroups(userManager);
		} catch (Exception e) {
			logger.error("Error on managing user DN: {} groups membership", userManager.entry.getDn().getName(), e);
			importLogger.error(Messages.manageUserGroupsMemberships(userManager.entry, e));
		}
	}

	private void manageUserPhoto(UserManager userManager) {
		try {
			if (userManager.userPhoto != null) {
				coreService.userSetPhoto(userManager.user.uid, userManager.userPhoto);
			} else {
				coreService.userDeletePhoto(userManager.user.uid);
			}
		} catch (Exception e) {
			logger.warn("Unable to manage user photo for DN {}: {}", userManager.entry.getDn(), e.getMessage());
			importLogger.warning(Messages.manageUserPhotoFailed(userManager.entry, e));
		}
	}

	private void beforeImport() {
		List<IScannerEnhancer> hooks = getScannerEnhancerHooks();

		if (hooks.isEmpty()) {
			return;
		}

		importLogger.info(Messages.beforeImport(getKind()));

		long timeBefore = System.currentTimeMillis();
		// Use dedicated directory connection for before hook as connection parameters
		// may be altered by hook.
		try (LdapConProxy ldapCon = getConnection()) {
			for (IScannerEnhancer iee : hooks) {
				iee.beforeImport(importLogger.withoutStatus(), getParameter(), domain, ldapCon);
			}
		} catch (IOException e) {
			logger.error("Directory connection error", e);
		}

		importLogger.info(Messages.beforeEndImport(getKind(), System.currentTimeMillis() - timeBefore));
	}

	private void afterImport() {
		List<IScannerEnhancer> hooks = getScannerEnhancerHooks();

		if (hooks.isEmpty()) {
			return;
		}

		importLogger.info(Messages.afterImport(getKind()));
		long timeBefore = System.currentTimeMillis();
		for (IScannerEnhancer iee : getScannerEnhancerHooks()) {
			iee.afterImport(importLogger.withoutStatus(), getParameter(), domain, ldapCon);
		}

		importLogger.info(Messages.afterEndImport(getKind(), System.currentTimeMillis() - timeBefore));
	}

	// TODO: must not be static
	public static void manageUserGroups(LdapConnection ldapCon, ICoreServices coreService, UserManager userManager,
			Function<String, Optional<? extends UuidMapper>> getUuidMapperFromExtId) throws ServerFault {
		// Getting BM user groups members
		List<ItemValue<Group>> userGroups;
		userGroups = new LinkedList<>(coreService.memberOf(userManager.user.uid));

		// Getting user groups member of list from directory
		List<? extends UuidMapper> directoryUserGroupsUuids = userManager.getUserGroupsMemberGuid(ldapCon);

		Member userAsMember = new Member();
		userAsMember.type = Member.Type.user;
		userAsMember.uid = userManager.user.uid;

		// Remove user from BM group if user is no more directory member
		// and store other group GUID
		// Ignore users with null or empty extId
		ArrayList<UuidMapper> userGroupsUuids = new ArrayList<>();
		for (ItemValue<Group> userGroup : userGroups) {
			Optional<? extends UuidMapper> uuidMapper = getUuidMapperFromExtId.apply(userGroup.externalId);
			if (!uuidMapper.isPresent()) {
				continue;
			}

			if (directoryUserGroupsUuids.contains(uuidMapper.get())) {
				userGroupsUuids.add(uuidMapper.get());
			} else {
				coreService.removeMembers(userGroup.uid, Arrays.asList(userAsMember));
			}
		}

		// Add user to BM group if user is a directory member and not already a
		// BM member
		for (UuidMapper adUserGroupUuid : directoryUserGroupsUuids) {
			if (!userGroupsUuids.contains(adUserGroupUuid)) {
				ItemValue<Group> groupMemberOf = coreService.getGroupByExtId(adUserGroupUuid.getExtId());
				if (groupMemberOf == null) {
					continue;
				}

				coreService.addMembers(groupMemberOf.uid, Arrays.asList(userAsMember));
			}
		}
	}
}
