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
package net.bluemind.system.ldap.export.services;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.netflix.spectator.api.Registry;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.Member.Type;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.system.ldap.export.hook.LdapServerHook;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroup;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroup.MembersList;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroups;
import net.bluemind.system.ldap.export.objects.DomainDirectoryRoot;
import net.bluemind.system.ldap.export.objects.DomainDirectoryUser;
import net.bluemind.system.ldap.export.objects.DomainDirectoryUsers;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class LdapExportService {
	private static final Logger logger = LoggerFactory.getLogger(LdapExportService.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), LdapExportService.class);

	public static LdapExportService build(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain) {
		IDirectory dirService = context.provider().instance(IDirectory.class, domain.uid);
		IUser userService = context.provider().instance(IUser.class, domain.uid);
		IGroup groupService = context.provider().instance(IGroup.class, domain.uid);
		IDomainSettings domainSettingsService = context.provider().instance(IDomainSettings.class, domain.uid);

		return new LdapExportService(domain, dirService, userService, groupService, domainSettingsService, server);
	}

	public static Optional<LdapExportService> build(String domainUid) {
		if (domainUid == null || domainUid.isEmpty()) {
			throw new ServerFault("Invalid domain UID", ErrorCode.INVALID_PARAMETER);
		}

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		ItemValue<Domain> domain = context.provider().instance(IDomains.class, domainUid).get(domainUid);
		if (domain == null) {
			throw new ServerFault(String.format("Domain %s not found", domainUid), ErrorCode.UNKNOWN);
		}

		List<ItemValue<Server>> ldapExportServers = ldapExportServer(context, domain.uid);
		if (ldapExportServers.size() != 1) {
			return Optional.empty();
		}

		IDirectory dirService = context.provider().instance(IDirectory.class, domain.uid);
		IUser userService = context.provider().instance(IUser.class, domain.uid);
		IGroup groupService = context.provider().instance(IGroup.class, domain.uid);
		IDomainSettings domainSettingsService = context.provider().instance(IDomainSettings.class, domain.uid);

		return Optional.of(new LdapExportService(domain, dirService, userService, groupService, domainSettingsService,
				ldapExportServers.get(0)));
	}

	public static List<ItemValue<Server>> ldapExportServer(BmContext context, String domainUid) {
		IServer server = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<String> ldapExportServersUids = server.byAssignment(domainUid, LdapServerHook.LDAPTAG);

		if (ldapExportServersUids.size() == 0) {
			return Collections.emptyList();
		}

		return ldapExportServersUids.stream().map(serverUid -> server.getComplete(serverUid))
				.collect(Collectors.toList());
	}

	private final ItemValue<Domain> domain;
	private final IDirectory dirService;
	private final IUser userService;
	private final IGroup groupService;
	private final IDomainSettings domainSettingsService;
	private final ItemValue<Server> ldapExportServer;

	private Optional<Integer> domainPasswordLifetime = Optional.empty();

	private LdapExportService(ItemValue<Domain> domain, IDirectory dirService, IUser userService, IGroup groupService,
			IDomainSettings domainSettingsService, ItemValue<Server> ldapExportServer) {
		this.domain = domain;
		this.dirService = dirService;
		this.userService = userService;
		this.groupService = groupService;
		this.domainSettingsService = domainSettingsService;
		this.ldapExportServer = ldapExportServer;
	}

	public void sync() throws Exception {
		Long lastVersion = 0l;
		ContainerChangeset<String> changeset = null;
		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapExportServer)) {
			lastVersion = getVersion(ldapCon);

			changeset = dirService.changeset(lastVersion);
			if (changeset == null) {
				throw new ServerFault("Unable to get changeset for domain: " + domain.uid, ErrorCode.INVALID_PARAMETER);
			}

			setDomainPasswordLifetime();

			doSync(ldapCon, changeset);
			setVersion(ldapCon, changeset.version);
			registry.gauge(idFactory.name("dirVersion", "domainUid", domain.uid, "source", "ldap-export"))
					.set(changeset.version);
		} catch (Exception e) {
			logger.error("Fail to update LDAP with changes from {} to {}", lastVersion,
					(changeset == null) ? "unknown" : changeset.version);
			throw e;
		}
	}

	private void setDomainPasswordLifetime() {
		try {
			Integer passwordLifetimeSetting = Integer
					.valueOf(domainSettingsService.get().get(DomainSettingsKeys.password_lifetime.name()));
			if (passwordLifetimeSetting > 0) {
				domainPasswordLifetime = Optional.of(passwordLifetimeSetting);
			}
		} catch (NumberFormatException nfe) {
		}
	}

	private void setVersion(LdapConnection ldapCon, long version) throws LdapException {
		ModifyRequestImpl modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(new Dn(new DomainDirectoryRoot(domain).getDn()));
		modifyRequest.replace("bmVersion", Long.toString(version));

		ldapCon.modify(modifyRequest);
	}

	private Long getVersion(LdapConnection ldapCon) throws LdapInvalidDnException, LdapException {
		long version = 0l;

		Entry domainEntry = ldapCon.lookup(new DomainDirectoryRoot(domain).getDn(), "bmVersion");
		if (domainEntry != null) {
			if (domainEntry.get("bmVersion") != null) {
				try {
					version = Long.parseLong(domainEntry.get("bmVersion").getString());
				} catch (NumberFormatException | LdapInvalidAttributeValueException e) {
				}
			}
		} else {
			initDomainTree(ldapCon);
		}

		return version;
	}

	private void initDomainTree(LdapConnection ldapCon) {
		LdapHelper.addLdapEntry(ldapCon, new DomainDirectoryRoot(domain).getLdapEntry());
		LdapHelper.addLdapEntry(ldapCon, new DomainDirectoryUsers(domain).getLdapEntry());
		LdapHelper.addLdapEntry(ldapCon, new DomainDirectoryGroups(domain).getLdapEntry());
	}

	private void doSync(LdapConnection ldapCon, ContainerChangeset<String> changeset)
			throws LdapException, CursorException {
		for (String uid : changeset.deleted) {
			deleteLdapEntry(ldapCon, uid);
		}

		for (String uid : changeset.created) {
			DirEntry dirEntry = dirService.findByEntryUid(uid);
			if (dirEntry == null) {
				logger.warn("no direntry for uid {}", uid);
				continue;
			}

			if (dirEntry.system) {
				continue;
			}

			switch (dirEntry.kind) {
			case USER:
				createUser(ldapCon, dirEntry.entryUid);
				break;
			case GROUP:
				createGroup(ldapCon, dirEntry.entryUid);
				break;
			default:
				logger.warn("Ignore uid {}, unsupported kind {}", uid, dirEntry.kind);
				break;
			}
		}

		for (String uid : changeset.updated) {
			DirEntry dirEntry = dirService.findByEntryUid(uid);
			if (dirEntry == null) {
				logger.warn("no direntry for uid {}", uid);
				continue;
			}

			if (dirEntry.system) {
				deleteLdapEntry(ldapCon, dirEntry.entryUid);
				continue;
			}

			switch (dirEntry.kind) {
			case USER:
				updateUser(ldapCon, dirEntry.entryUid);
				break;
			case GROUP:
				updateGroup(ldapCon, dirEntry.entryUid);
				break;
			default:
				logger.warn("Ignore uid {}, unsupported kind {}", uid, dirEntry.kind);
				break;
			}
		}
	}

	private void updateGroup(LdapConnection ldapCon, String uid) throws LdapException, CursorException {
		List<Entry> ldapEntries = LdapHelper.getLdapEntryFromUid(ldapCon, domain, uid);
		if (ldapEntries.size() != 1) {
			resetLdapGroup(ldapCon, ldapEntries, uid);
			return;
		}

		Entry groupLdapEntry = ldapEntries.get(0);

		ItemValue<Group> group = groupService.getComplete(uid);
		if (group == null) {
			throw new ServerFault("Unable to find group UID: " + uid);
		}

		DomainDirectoryGroup.MembersList members = getGroupMembers(group);
		DomainDirectoryGroup ddg = new DomainDirectoryGroup(domain, group, members);

		updateLdapGroupEntry(ldapCon, uid, ddg, groupLdapEntry);
	}

	private void updateLdapGroupEntry(LdapConnection ldapCon, String groupUid,
			DomainDirectoryGroup domainDirectoryGroup, Entry groupLdapEntry)
			throws LdapException, CursorException, LdapInvalidDnException {
		LdapHelper.modifyLdapEntry(ldapCon, domainDirectoryGroup.getModifyRequest(groupLdapEntry));

		manageParentsMemberUid(ldapCon, groupLdapEntry, groupUid, domainDirectoryGroup);

		if (!groupLdapEntry.getDn().getName().equals(domainDirectoryGroup.getDn())) {
			ldapCon.rename(groupLdapEntry.getDn(), new Rdn(domainDirectoryGroup.getRDn()));

			// JUnit: testExportGroupMember_renameGroupMember
			updateParentsGroupsMembersAttributes(ldapCon,
					groupService.getParents(groupUid).stream().map(g -> g.uid).collect(Collectors.toList()),
					MemberAttrUpdate.build("member", groupLdapEntry.getDn().getName(), domainDirectoryGroup.getDn()));
		}
	}

	private void manageParentsMemberUid(LdapConnection ldapCon, Entry groupLdapEntry, String groupUid,
			DomainDirectoryGroup ddg) throws LdapException, CursorException {
		List<String> removedMembersUids = ddg.getRemovedMembersUid(groupLdapEntry);
		List<String> addedMembersUids = ddg.getAddedMembersUid(groupLdapEntry);

		if (removedMembersUids.isEmpty() && addedMembersUids.isEmpty()) {
			return;
		}

		List<String> parentsGroups = groupService.getParents(groupUid).stream().map(g -> g.uid)
				.collect(Collectors.toList());

		for (String removedMemberUid : removedMembersUids) {
			ItemValue<User> user = userService.byLogin(removedMemberUid);
			if (user == null) {
				// User was already deleted - do nothing
				continue;
			}

			List<String> userGroups = userService.memberOfGroups(user.uid);
			updateParentsGroupsMembersAttributes(ldapCon,
					parentsGroups.stream().filter(g -> !userGroups.contains(g)).collect(Collectors.toList()),
					MemberAttrUpdate.build("memberUid", removedMemberUid, null));
		}

		updateParentsGroupsMembersAttributes(ldapCon, parentsGroups, addedMembersUids.stream()
				.map(m -> MemberAttrUpdate.build("memberUid", null, m)).toArray(MemberAttrUpdate[]::new));
	}

	private void resetLdapGroup(LdapConnection ldapCon, List<Entry> ldapEntries, String uid)
			throws LdapException, CursorException {
		logger.warn("{} LDAP entries with same bmUid {} - remove and re-recreate LDAP entry", ldapEntries.size(), uid);
		for (Entry entry : ldapEntries) {
			logger.warn("Removing entry DN {}", entry.getDn().toString());
			ldapCon.delete(entry.getDn());
		}

		createGroup(ldapCon, uid);
	}

	private void createGroup(LdapConnection ldapCon, String uid) throws LdapException, CursorException {
		ItemValue<Group> group = groupService.getComplete(uid);
		if (group == null) {
			throw new ServerFault("Unable to find group UID: " + uid);
		}

		DomainDirectoryGroup.MembersList members = getGroupMembers(group);
		DomainDirectoryGroup domainDirectoryGroup = new DomainDirectoryGroup(domain, group, members);

		Entry entry = ldapCon.lookup(domainDirectoryGroup.getDn());
		if (entry != null) {
			updateLdapGroupEntry(ldapCon, uid, domainDirectoryGroup, entry);
			return;
		}

		createLdapGroupEntry(ldapCon, uid, domainDirectoryGroup);
	}

	private void createLdapGroupEntry(LdapConnection ldapCon, String groupUid,
			DomainDirectoryGroup domainDirectoryGroup) throws LdapException, CursorException {
		Entry ldapEntry = domainDirectoryGroup.getLdapEntry();

		LdapHelper.addLdapEntry(ldapCon, ldapEntry);

		// Manage parent group is created before child group in the same
		// changeset
		// JUnit: testExportGroupMember_createGroupHierarchy
		updateParentsGroupsMembersAttributes(ldapCon,
				groupService.getParents(groupUid).stream().map(g -> g.uid).collect(Collectors.toList()),
				MemberAttrUpdate.build("member", ldapEntry.getDn().getName(), ldapEntry.getDn().getName()));
	}

	private MembersList getGroupMembers(ItemValue<Group> group) throws LdapException, CursorException {
		MembersList membersList = new DomainDirectoryGroup.MembersList();
		Set<String> usersAlreadyManaged = new HashSet<>();

		for (Member member : groupService.getMembers(group.uid)) {
			switch (member.type) {
			case user:
				ItemValue<User> user = userService.getComplete(member.uid);
				if (user == null) {
					throw new ServerFault("Unable to find user uid: " + member.uid);
				}

				membersList.member.add(new DomainDirectoryUser(domain, domainPasswordLifetime, user).getDn());
				membersList.memberUid.add(user.value.login);

				usersAlreadyManaged.add(member.uid);
				break;
			case group:
				group = groupService.getComplete(member.uid);
				if (group == null) {
					throw new ServerFault("Unable to find group uid: " + member.uid);
				}

				membersList.member.add(new DomainDirectoryGroup(domain, group).getDn());
				break;
			case external_user:
				// nothing to do
				break;
			default:
				throw new ServerFault("Unknown member type: " + member.type.name(), ErrorCode.INVALID_PARAMETER);
			}
		}

		for (Member member : groupService.getExpandedMembers(group.uid)) {
			if (usersAlreadyManaged.contains(member.uid) || member.type == Type.external_user) {
				continue;
			}

			ItemValue<User> user = userService.getComplete(member.uid);
			if (user == null) {
				throw new ServerFault("Unable to find user uid: " + member.uid);
			}

			membersList.memberUid.add(user.value.login);

			usersAlreadyManaged.add(member.uid);
		}

		return membersList;
	}

	private void deleteLdapEntry(LdapConnection ldapCon, String entryUid) throws LdapException, CursorException {
		for (Entry entry : LdapHelper.getLdapEntryFromUid(ldapCon, domain, entryUid, "dn")) {
			ldapCon.delete(entry.getDn());
		}
	}

	private void createUser(LdapConnection ldapCon, String uid) throws ServerFault, LdapException, CursorException {
		ItemValue<User> user = userService.getComplete(uid);
		if (user == null) {
			throw new ServerFault("Unable to find user UID: " + uid);
		}

		byte[] userPhoto = userService.getPhoto(uid);
		DomainDirectoryUser domainDirectoryUser = new DomainDirectoryUser(domain, domainPasswordLifetime, user,
				userPhoto);

		Entry entry = ldapCon.lookup(domainDirectoryUser.getDn());
		if (entry != null) {
			updateLdapUserEntry(ldapCon, uid, domainDirectoryUser, entry);
			return;
		}

		createLdapUserEntry(ldapCon, uid, domainDirectoryUser);
	}

	private void createLdapUserEntry(LdapConnection ldapCon, String userUid, DomainDirectoryUser domainDirectoryUser)
			throws LdapException, CursorException {
		Entry ldapEntry = domainDirectoryUser.getLdapEntry();

		LdapHelper.addLdapEntry(ldapCon, ldapEntry);

		// Manage user parent group is created before user in the same
		// changeset
		// JUnit: testExportGroupMember_addUserToGroupChild
		updateParentsGroupsMembersAttributes(ldapCon, userService.memberOfGroups(userUid),
				MemberAttrUpdate.build("member", ldapEntry.getDn().getName(), ldapEntry.getDn().getName()));
	}

	private void updateUser(LdapConnection ldapCon, String uid) throws LdapException, CursorException {
		List<Entry> ldapEntries = LdapHelper.getLdapEntryFromUid(ldapCon, domain, uid);
		if (ldapEntries.size() != 1) {
			resetLdapUser(ldapCon, ldapEntries, uid);
			return;
		}

		Entry userLdapEntry = ldapEntries.get(0);

		ItemValue<User> user = userService.getComplete(uid);
		if (user == null) {
			throw new ServerFault("Unable to find user UID: " + uid);
		}
		DomainDirectoryUser ddu = new DomainDirectoryUser(domain, domainPasswordLifetime, user,
				userService.getPhoto(uid));
		updateLdapUserEntry(ldapCon, uid, ddu, userLdapEntry);
	}

	private void updateLdapUserEntry(LdapConnection ldapCon, String userUid, DomainDirectoryUser ddu,
			Entry userLdapEntry)
			throws LdapException, LdapInvalidDnException, CursorException, LdapInvalidAttributeValueException {
		LdapHelper.modifyLdapEntry(ldapCon, ddu.getModifyRequest(userLdapEntry));

		if (!userLdapEntry.getDn().getName().equals(ddu.getDn())) {
			ldapCon.rename(userLdapEntry.getDn(), new Rdn(ddu.getRDn()));

			updateParentsGroupsMembersAttributes(ldapCon, userService.memberOfGroups(userUid),
					MemberAttrUpdate.build("member", userLdapEntry.getDn().getName(), ddu.getDn()),
					MemberAttrUpdate.build("memberUid", userLdapEntry.get("uid").getString(), ddu.getRDnValue()));
		}
	}

	private static class MemberAttrUpdate {
		public final String name;
		public final String oldValue;
		public final String newValue;

		public static MemberAttrUpdate build(String name, String oldValue, String newValue) {
			return new MemberAttrUpdate(name, oldValue, newValue);
		}

		public MemberAttrUpdate(String name, String oldValue, String newValue) {
			this.name = name;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public ModifyRequest setModifications(ModifyRequest modifyRequest, Entry entry) {
			Attribute attr = entry.get(name);

			if (!Strings.isNullOrEmpty(oldValue) && !Strings.isNullOrEmpty(newValue) && attr.contains(oldValue)) {
				modifyRequest.remove(name, oldValue);
				modifyRequest.add(name, newValue);
			} else if (Strings.isNullOrEmpty(newValue) && !Strings.isNullOrEmpty(oldValue) && attr.contains(oldValue)) {
				modifyRequest.remove(name, oldValue);
			} else if (Strings.isNullOrEmpty(oldValue) && !Strings.isNullOrEmpty(newValue)
					&& !attr.contains(newValue)) {
				modifyRequest.add(name, newValue);
			}

			return modifyRequest;
		}
	}

	private void updateParentsGroupsMembersAttributes(LdapConnection ldapCon, Collection<String> parentGroupUids,
			MemberAttrUpdate... updates) throws LdapException, CursorException {
		if (updates.length == 0) {
			return;
		}

		String[] attrsToManage = Stream.of(updates).map(u -> u.name).toArray(String[]::new);

		for (String parentUid : parentGroupUids) {
			for (Entry entry : LdapHelper.getLdapEntryFromUid(ldapCon, domain, parentUid, attrsToManage)) {
				ModifyRequest modifyRequest = new ModifyRequestImpl();
				for (MemberAttrUpdate update : updates) {
					modifyRequest = update.setModifications(modifyRequest, entry);
				}

				if (modifyRequest.getModifications().size() != 0) {
					modifyRequest.setName(entry.getDn());

					ModifyResponse response = ldapCon.modify(modifyRequest);
					if (response.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS) {
						throw new ServerFault("Fail to update member values on entry DN: " + entry.getDn() + ", code: "
								+ response.getLdapResult().getResultCode() + " - "
								+ response.getLdapResult().getDiagnosticMessage());
					}
				}
			}
		}
	}

	private void resetLdapUser(LdapConnection ldapCon, List<Entry> ldapEntries, String uid)
			throws LdapException, ServerFault, CursorException {
		logger.warn("{} LDAP entries with same bmUid {} - remove and re-recreate LDAP entry", ldapEntries.size(), uid);
		for (Entry entry : ldapEntries) {
			logger.warn("Removing entry DN {}", entry.getDn().toString());
			ldapCon.delete(entry.getDn());
		}

		createUser(ldapCon, uid);
	}
}
