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
package net.bluemind.group.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.ValidationResult;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.service.IInCoreExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.hook.GroupMessage;
import net.bluemind.group.hook.IGroupHook;
import net.bluemind.group.service.GroupHelper;
import net.bluemind.group.service.IInCoreGroup;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.api.IRoles;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.server.api.IServer;
import net.bluemind.user.service.IInCoreUser;

public class GroupService implements IGroup, IInCoreGroup {

	private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
	private final ContainerGroupStoreService storeService;
	private final SecurityContext securityContext;
	private final Container groupContainer;
	private final List<IGroupHook> groupsHooks;
	private final IInCoreMailboxes mailboxes;
	private final IServiceProvider serviceProvider;
	private final BmContext context;
	private final String domainUid;
	private final Sanitizer sanitizer;
	private final GroupValidator groupValidator;
	private final Validator validator;
	private RBACManager rbacManager;
	private DirEventProducer dirEventProducer;

	public GroupService(BmContext context, ItemValue<Domain> domain, Container container, List<IGroupHook> groupsHooks)
			throws ServerFault {
		this.context = context;
		this.domainUid = domain.uid;
		this.serviceProvider = context.getServiceProvider();
		this.groupsHooks = groupsHooks;
		groupContainer = container;
		this.securityContext = context.getSecurityContext();
		storeService = new ContainerGroupStoreService(context, container, domain);

		mailboxes = context.su().provider().instance(IInCoreMailboxes.class, domainUid);

		IServer serverService = context.su().getServiceProvider().instance(IServer.class,
				InstallationId.getIdentifier());
		groupValidator = new GroupValidator(serverService, domainUid);

		sanitizer = new Sanitizer(context);
		validator = new Validator(context);

		rbacManager = new RBACManager(context).forContainer(container);
		dirEventProducer = new DirEventProducer(domainUid, VertxPlatform.eventBus());

	}

	@Override
	public void create(String uid, Group group) throws ServerFault {
		createWithExtId(uid, null, group);
	}

	@Override
	public void createWithExtId(String uid, String extId, Group group) throws ServerFault {
		sanitizer.create(group);
		sanitizer.create(new DirDomainValue<>(domainUid, uid, group));
		groupValidator.validate(uid, extId, group);
		validator.create(group);

		rbacManager.forOrgUnit(group.orgUnitUid).check(BasicRoles.ROLE_MANAGE_GROUP);
		// ext point sanitizer
		if (storeService.nameAlreadyUsed(null, group)) {
			throw new ServerFault("Group name: " + group.name + " already used", ErrorCode.ALREADY_EXISTS);
		}

		group.emails = EmailHelper.sanitizeAndValidate(group.emails);

		mailboxes.validate(uid, GroupHelper.groupToMailbox(group));

		storeService.createWithExtId(uid, extId, group);
		mailboxes.created(uid, GroupHelper.groupToMailbox(group));
		logger.debug("Created {}", uid);
		for (IGroupHook gh : groupsHooks) {
			gh.onGroupCreated(new GroupMessage(iv(uid, group), securityContext, groupContainer));
		}

		dirEventProducer.changed(uid, storeService.getVersion());
	}

	ItemValue<Group> iv(String uid, Group g) {
		Item it = Item.create(uid, null);
		return ItemValue.create(it, g);
	}

	@Override
	public void update(String uid, Group group) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_GROUP);
		ItemValue<Group> previousItemValue = getFull(uid);
		if (previousItemValue == null || previousItemValue.value == null) {
			logger.error("Group uid: {} doesn't exist !", uid);
			throw new ServerFault("Group uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}
		Group previous = previousItemValue.value;

		if (!StringUtils.equals(group.orgUnitUid, previous.orgUnitUid)) {
			rbacManager.forOrgUnit(group.orgUnitUid).check(BasicRoles.ROLE_MANAGE_GROUP);
		}
		// ext point sanitizer
		sanitizer.update(previous, group);
		sanitizer.update(new DirDomainValue<>(domainUid, uid, previous), new DirDomainValue<>(domainUid, uid, group));

		groupValidator.validate(uid, group);
		validator.update(previous, group);

		group.emails = EmailHelper.sanitizeAndValidate(group.emails);
		mailboxes.validate(uid, GroupHelper.groupToMailbox(group));

		storeService.update(uid, group);

		mailboxes.updated(uid, GroupHelper.groupToMailbox(previous), GroupHelper.groupToMailbox(group));

		for (IGroupHook gh : groupsHooks) {
			gh.onGroupUpdated(new GroupMessage(iv(uid, previous), securityContext, groupContainer),
					new GroupMessage(iv(uid, group), securityContext, groupContainer));
		}
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void touch(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_GROUP);
		ItemValue<Group> previousItemValue = getFull(uid);
		if (previousItemValue == null || previousItemValue.value == null) {
			logger.error("Group uid: {} doesn't exist !", uid);
			throw new ServerFault("Group uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		storeService.update(uid, previousItemValue.value);
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public ItemValue<Group> getComplete(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);
		ParametersValidator.notNullAndNotEmpty(uid);

		return getFull(uid);
	}

	private ItemValue<Group> getFull(String uid) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<DirEntryAndValue<Group>> ret = storeService.get(uid, null);
		return asGroup(ret);
	}

	private ItemValue<Group> asGroup(ItemValue<DirEntryAndValue<Group>> itemValue) {
		if (itemValue == null) {
			return null;
		}

		return ItemValue.create(itemValue, itemValue.value.value);
	}

	@Override
	public ItemValue<Group> byEmail(String email) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_GROUP);
		ItemValue<DirEntryAndValue<Group>> ret = storeService.findByEmailFull(email);
		return asGroup(ret);
	}

	@Override
	public ItemValue<Group> byName(String name) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);
		ItemValue<DirEntryAndValue<Group>> ret = storeService.byName(name);
		return asGroup(ret);
	}

	@Override
	public TaskRef delete(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_GROUP);

		return context.provider().instance(ITasksManager.class).run(monitor -> {
			monitor.begin(2, "Deleting group " + uid + "@" + domainUid);

			ParametersValidator.notNullAndNotEmpty(uid);

			ItemValue<DirEntryAndValue<Group>> previousItemValue = storeService.get(uid, null);

			if (previousItemValue == null) {
				logger.warn("delete non existing group {}@{}", uid, domainUid);
				return;
			}
			Group previous = asGroup(previousItemValue).value;

			List<String> memberOfGroups = storeService.getMemberOfGroup(uid);
			for (String parentUid : memberOfGroups) {
				remove(parentUid, Arrays.asList(Member.group(uid)));
			}

			monitor.progress(1, "Deleting group mailbox ...");
			mailboxes.deleted(uid, GroupHelper.groupToMailbox(previous));
			monitor.progress(2, "Group mailbox deleted");

			storeService.delete(uid);

			dirEventProducer.deleted(uid, storeService.getVersion());
			for (IGroupHook gh : groupsHooks) {
				gh.onGroupDeleted(new GroupMessage(iv(uid, previous), securityContext, groupContainer));
			}

			monitor.end(true, "Group deleted", JsonUtils.asString(""));
		});

	}

	@Override
	public ItemValue<Group> getByExtId(String extId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);
		ParametersValidator.notNullAndNotEmpty(extId);

		return asGroup(storeService.getByExtId(extId));
	}

	@Override
	public void add(String uid, List<Member> members) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<Group> group = getFull(uid);
		if (group == null || group.value == null) {
			logger.error("Group uid: {} doesn't exist !", uid);
			throw new ServerFault("Group uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		validMembers(members);

		if (members.size() == 0) {
			return;
		}

		checkCanManageGroupMembers(group, members);

		Set<String> currentMembers = storeService.getMembers(uid).stream().map((m) -> {
			return m.uid;
		}).collect(Collectors.toSet());

		List<Member> alreadyPresent = members.stream().filter((m) -> {
			return currentMembers.contains(m.uid);
		}).collect(Collectors.toList());

		if (!alreadyPresent.isEmpty()) {
			logger.error("Group uid: {}: members ({}) are already in group", uid, alreadyPresent);
			if (members.size() == alreadyPresent.size()) {
				throw new ServerFault("Group uid: " + uid + " all users are already in the group.",
						ErrorCode.INVALID_PARAMETER);
			}
			members.removeAll(alreadyPresent);
		}

		storeService.addMembers(uid, members);

		for (IGroupHook gh : groupsHooks) {
			gh.onAddMembers(new GroupMessage(iv(uid, group.value), securityContext, groupContainer, members));
		}

		dirEventProducer.changed(uid, storeService.getVersion());
	}

	private void checkCanManageGroupMembers(ItemValue<Group> group, List<Member> members) {

		if (group.value.profile()) {
			if (!rbacManager.forEntry(group.uid).can(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS)) {
				// first of all, current user MUST BE in target group (if cannot
				// directly manage group members
				if (!securityContext.getMemberOf().contains(group.uid)) {
					throw new ServerFault(String.format("%s@%s Doesnt have role %s on dirEntry %s@%s ", //
							context.getSecurityContext().getSubject(), context.getSecurityContext().getContainerUid(), //
							BasicRoles.ROLE_MANAGE_GROUP_MEMBERS, //
							group.uid, domainUid), ErrorCode.PERMISSION_DENIED);
				}

				// members must be "manageable" by current user
				for (Member member : members) {
					rbacManager.forEntry(member.uid).check(BasicRoles.ROLE_MANAGE_USER);
				}
			}
			// else current user can ROLE_MANAGE_GROUP_MEMBERS
		} else {
			rbacManager.forEntry(group.uid).check(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS);
		}
	}

	private void validMembers(List<Member> members) throws ServerFault {
		ArrayList<String> usersUids = new ArrayList<String>();
		ArrayList<String> groupsUids = new ArrayList<String>();
		ArrayList<String> externalUsersUids = new ArrayList<String>();

		for (Member member : members) {
			if (member.type == null || member.uid == null || member.uid.isEmpty()) {
				logger.error("Invalid member");
				throw new ServerFault("Invalid member", ErrorCode.INVALID_PARAMETER);
			}

			if (member.type == Member.Type.user) {
				usersUids.add(member.uid);
			} else if (member.type == Member.Type.group) {
				groupsUids.add(member.uid);
			} else if (member.type == Member.Type.external_user) {
				externalUsersUids.add(member.uid);
			} else {
				throw new ServerFault("Unknown type of member", ErrorCode.INVALID_PARAMETER);
			}
		}

		StringBuffer log = new StringBuffer();

		ValidationResult groupValidity = this.validate(groupsUids.toArray(new String[0]));
		if (!groupValidity.valid) {
			groupValidity.validationResults.forEach((uid, valid) -> {
				if (!valid) {
					log.append(String.format("No group with uid %s found%s", uid, "\r\n"));
				}
			});
		}

		ValidationResult userValidity = serviceProvider.instance(IInCoreUser.class, domainUid)
				.validate(usersUids.toArray(new String[0]));
		if (!userValidity.valid) {
			userValidity.validationResults.forEach((uid, valid) -> {
				if (!valid) {
					log.append(String.format("No user with uid %s found%s", uid, "\r\n"));
				}
			});
		}

		ValidationResult externalUsersValidity = serviceProvider.instance(IInCoreExternalUser.class, domainUid)
				.validate(externalUsersUids.toArray(new String[0]));
		if (!externalUsersValidity.valid) {
			externalUsersValidity.validationResults.forEach((uid, valid) -> {
				if (!valid) {
					log.append(String.format("No external user with uid %s found%s", uid, "\r\n"));
				}
			});
		}

		if (!groupValidity.valid || !externalUsersValidity.valid || !userValidity.valid) {
			logger.warn(log.toString());
			throw new ServerFault(log.toString(), ErrorCode.INVALID_PARAMETER);
		}
	}

	@Override
	public List<Member> getMembers(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<Group> group = getFull(uid);
		if (group == null || group.value == null) {
			logger.error("Group uid: {} doesn't exist !", uid);
			throw new ServerFault("Group uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		return storeService.getMembers(uid);
	}

	@Override
	public List<Member> getExpandedMembers(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<Group> group = getFull(uid);
		if (group == null || group.value == null) {
			logger.error("Group uid: {} doesn't exist !", uid);
			throw new ServerFault("Group uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		return storeService.getFlatUsersMembers(uid);
	}

	@Override
	public List<Member> getExpandedUserMembers(String uid) throws ServerFault {
		return this.getExpandedMembers(uid).stream().filter(m -> m.type == Member.Type.user)
				.collect(Collectors.toList());
	}

	@Override
	public void remove(String uid, List<Member> members) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<Group> group = getFull(uid);
		if (group == null || group.value == null) {
			logger.error("Group uid: {} doesn't exist !", uid);
			throw new ServerFault("Group uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		validMembers(members);

		if (members.size() == 0) {
			return;
		}

		checkCanManageGroupMembers(group, members);

		storeService.removeMembers(uid, members);
		for (IGroupHook uh : groupsHooks) {
			uh.onRemoveMembers(new GroupMessage(iv(uid, group.value), securityContext, groupContainer, members));
		}
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public List<ItemValue<Group>> getParents(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<Group> group = getFull(uid);
		if (group == null || group.value == null) {
			logger.error("Group uid: {} doesn't exist !", uid);
			throw new ServerFault("Group uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		List<String> parentsUid = storeService.getParents(uid);

		return storeService.getMultipleValues(parentsUid);
	}

	@Override
	public List<String> allUids() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);

		return storeService.allUids();
	}

	@Override
	public Set<String> getRoles(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_GROUP, BasicRoles.ROLE_MANAGER);

		return storeService.getRoles(uid);
	}

	@Override
	public void setRoles(String uid, Set<String> roles) throws ServerFault {

		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_GROUP);

		if (roles == null) {
			roles = Collections.emptySet();
		}

		HashSet<String> rolesToCheck = new HashSet<>(roles);

		for (RoleDescriptor role : context.provider().instance(IRoles.class).getRoles()) {
			if (role.delegable) {
				rolesToCheck.remove(role.id);
			}
		}

		// do not check already assigned roles
		Set<String> previousRoles = storeService.getRoles(uid);
		rolesToCheck.removeAll(previousRoles);

		if (!rolesToCheck.isEmpty() && !rbacManager.can(BasicRoles.ROLE_SYSTEM_MANAGER)
		// we can only delegate roles we have
				&& !rbacManager.roles().containsAll(rolesToCheck)) {

			Set<String> neededRoles = new HashSet<>(rolesToCheck);
			neededRoles.removeAll(rbacManager.roles());
			throw new ServerFault("cannot assign roles which current user doesnt have (needed roles {"
					+ String.join(",", neededRoles) + "} )", ErrorCode.PERMISSION_DENIED);
		}

		ItemValue<Group> item = storeService.get(uid);
		if (item == null) {
			throw new ServerFault("group " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		storeService.setRoles(uid, roles);
	}

	@Override
	public Set<String> getGroupsWithRoles(List<String> roles) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP_MEMBERS);

		return storeService.getGroupsWithRoles(roles);
	}

	@Override
	public List<ItemValue<Group>> search(GroupSearchQuery query) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_GROUP);

		sanitizer.create(query);
		return storeService.search(query).stream().map(g -> asGroup(g)).collect(Collectors.toList());
	}

	@Override
	public void setExtId(String uid, String extId) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER);

		ItemValue<Group> previous = getFull(uid);
		if (previous == null) {
			throw new ServerFault("group " + uid + " not found in domain " + domainUid, ErrorCode.NOT_FOUND);
		}
		storeService.setExtId(uid, extId);
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public ValidationResult validate(String[] groupUids) throws ServerFault {
		boolean valid = storeService.allValid(groupUids);
		if (valid) {
			return new ValidationResult(valid, groupUids);
		} else {
			Map<String, Boolean> validationResults = new HashMap<>();
			for (String uid : groupUids) {
				validationResults.put(uid, storeService.allValid(new String[] { uid }));
			}
			return new ValidationResult(valid, validationResults);
		}
	}

}
