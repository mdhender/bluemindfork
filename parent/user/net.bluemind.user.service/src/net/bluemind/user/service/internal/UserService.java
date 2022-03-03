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
package net.bluemind.user.service.internal;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.authentication.persistence.APIKeyStore;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.ImageUtils;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.ValidationResult;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.directory.service.DirValueStoreService.MailboxAdapter;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.persistence.GroupStore;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.mailbox.service.internal.MailboxQuotaHelper;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.api.DefaultRoles;
import net.bluemind.role.api.IRoles;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.service.IInternalRoles;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IPasswordUpdater;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.IUserHook;
import net.bluemind.user.persistence.security.HashAlgorithm;
import net.bluemind.user.persistence.security.HashFactory;
import net.bluemind.user.service.IInCoreUser;
import net.bluemind.user.service.accounttype.UserAccountFactory;
import net.bluemind.user.service.passwordvalidator.PasswordValidator;

public class UserService implements IInCoreUser, IUser {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);
	private final ContainerUserStoreService storeService;
	private final Container userContainer;
	private final Domain domain;
	private final String domainName;
	private final SecurityContext context;
	private final GroupStore groupStore;
	private final List<IUserHook> userHooks;
	private final List<IPasswordUpdater> userPasswordUpdaters;
	private final BmContext bmContext;
	private final boolean globalVirt;
	private final Sanitizer sanitizer;
	private final APIKeyStore apikeyStore;
	private final Validator validator;
	private final PasswordValidator passwordValidator;
	private IInCoreMailboxes mailboxes;
	private RBACManager rbacManager;
	private UserEventProducer eventProducer;
	private MailboxAdapter<User> mailboxAdapter;

	public UserService(BmContext context, ItemValue<Domain> domain, Container container, List<IUserHook> userHooks,
			List<IPasswordUpdater> userPasswordUpdaters) throws ServerFault {
		this.eventProducer = new UserEventProducer(domain.uid, VertxPlatform.eventBus());
		this.userHooks = userHooks;
		this.userPasswordUpdaters = userPasswordUpdaters;
		userContainer = container;
		DataSource pool = context.getDataSource();
		this.domain = domain.value;
		this.domainName = domain.uid;
		globalVirt = "global.virt".equals(domain.uid);
		this.bmContext = context;
		this.context = context.getSecurityContext();
		storeService = new ContainerUserStoreService(context, container, domain, globalVirt);
		mailboxes = bmContext.su().provider().instance(IInCoreMailboxes.class, domainName);

		apikeyStore = new APIKeyStore(context.getDataSource(), context.getSecurityContext());

		groupStore = new GroupStore(pool, container);
		sanitizer = new Sanitizer(context);
		validator = new Validator(context);
		passwordValidator = new PasswordValidator(context);

		rbacManager = new RBACManager(context).forDomain(userContainer.uid);
		mailboxAdapter = UserMailboxAdapter.create(globalVirt);
	}

	@Override
	public void create(String uid, User user) throws ServerFault {
		createWithExtId(uid, null, user);
	}

	@Override
	public void createWithExtId(String uid, String extId, User user) throws ServerFault {
		ItemValue<User> itemValue = createItemValue(uid, user);
		itemValue.externalId = extId;
		createWithItem(itemValue);
	}

	private void createWithItem(ItemValue<User> userItemValue) throws ServerFault {
		User user = userItemValue.value;
		String uid = userItemValue.uid;
		rbacManager.forOrgUnit(user.orgUnitUid).check(BasicRoles.ROLE_MANAGE_USER);
		sanitizer.create(user);
		sanitizer.create(new DirDomainValue<>(domainName, uid, user));
		validator.create(user);
		passwordValidator.validate(user.password);

		if (byLogin(user.login) != null) {
			throw new ServerFault("user with login " + user.login + " already exists", ErrorCode.ALREADY_EXISTS);
		}

		for (IUserHook uh : userHooks) {
			uh.beforeCreate(bmContext, domainName, uid, user);
		}

		if (!globalVirt && !user.system) {
			if (null == user.quota) {
				user.quota = MailboxQuotaHelper
						.getDefaultQuota(bmContext.su().provider().instance(IDomainSettings.class, domainName).get(),
								DomainSettingsKeys.mailbox_max_user_quota.name(),
								DomainSettingsKeys.mailbox_default_user_quota.name())
						.orElse(null);
			}

			mailboxes.validate(uid, mailboxAdapter.asMailbox(domainName, uid, user));
		}

		String prevPass = user.password;
		if (StringUtils.isNotBlank(user.password)) {
			// we support setting the user password as a hash, directly
			// this is used for external user importers
			if (HashFactory.algorithm(user.password) == HashAlgorithm.UNKNOWN) {
				user.password = HashFactory.getDefault().create(user.password);
			}
			user.passwordLastChange = new Date();
		}

		MailFilter filter = null;
		if (!globalVirt && !user.system) {
			filter = transformExternalEmailsToForwards(user, new MailFilter());
		}
		storeService.create(userItemValue);
		ItemValue<User> item = createItemValue(uid, user);
		if (!globalVirt && !user.system) {
			mailboxes.created(uid, mailboxAdapter.asMailbox(domainName, uid, user));
			if (user.routing == Routing.internal) {
				mailboxes.setMailboxFilter(uid, filter);
			}
		}

		user.password = prevPass;

		for (IUserHook uh : userHooks) {
			try {
				uh.onUserCreated(bmContext, domainName, item);
			} catch (Exception e) {
				// make hook error proof..
				if (logger.isDebugEnabled()) {
					logger.error("error during executing onUserCreated {}/{} hook {}}", domainName, uid,
							uh.getClass().getName(), e);
				} else {
					logger.error("error during executing onUserCreated {}/{} hook {} : message: {}", domainName, uid,
							uh.getClass().getName(), e.getMessage());
				}
			}
		}

		eventProducer.changed(uid, user);
	}

	ItemValue<User> createItemValue(String uid, User u) {
		Item it = Item.create(uid, null);
		return ItemValue.create(it, u);
	}

	@Override
	public void update(String uid, User user) throws ServerFault {
		ItemValue<User> itemValue = createItemValue(uid, user);
		updateWithItem(itemValue);
	}

	private void updateWithItem(ItemValue<User> userItem) throws ServerFault {
		String uid = userItem.uid;
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER);

		User user = userItem.value;

		ItemValue<User> previous = getFull(uid);
		if (previous == null) {
			throw new ServerFault("user " + uid + " not found in domain " + domainName, ErrorCode.NOT_FOUND);
		}

		if (!StringUtils.equals(user.orgUnitUid, previous.value.orgUnitUid)) {
			rbacManager.forOrgUnit(user.orgUnitUid).check(BasicRoles.ROLE_MANAGE_USER);
		}

		sanitizer.update(previous.value, user);
		sanitizer.update(new DirDomainValue<>(domainName, uid, previous.value),
				new DirDomainValue<>(domainName, uid, user));

		validator.update(previous.value, user);

		for (IUserHook uh : userHooks) {
			uh.beforeUpdate(bmContext, domainName, uid, user, previous.value);
		}

		user.password = previous.value.password;
		user.passwordLastChange = previous.value.passwordLastChange;

		MailFilter filter = null;
		if (!globalVirt && !user.system) {
			filter = transformExternalEmailsToForwards(user, mailboxes.getMailboxFilter(uid));
			mailboxes.validate(uid, mailboxAdapter.asMailbox(domainName, uid, user));
		}
		storeService.update(userItem);

		if (!globalVirt && !user.system) {
			mailboxes.updated(uid, mailboxAdapter.asMailbox(domainName, uid, previous.value),
					mailboxAdapter.asMailbox(domainName, uid, user));
			mailboxes.setMailboxFilter(uid, filter);
		}

		for (IUserHook uh : userHooks) {
			try {
				uh.onUserUpdated(bmContext, domainName, previous, createItemValue(uid, user));
			} catch (Exception e) {
				// make hook error proof..
				if (logger.isDebugEnabled()) {
					logger.error("error during executing onUserUpdated {}/{} hook {}}", domainName, uid,
							uh.getClass().getName(), e);

				} else {
					logger.error("error during executing onUserUpdated {}/{} hook {} : message: {}", domainName, uid,
							uh.getClass().getName(), e.getMessage());

				}
			}
		}

		eventProducer.changed(uid, user);
	}

	public ItemValue<User> getFull(String uid) throws ServerFault {
		ItemValue<DirEntryAndValue<User>> itemValue = storeService.get(uid, null);
		return asFullUser(itemValue);
	}

	private ItemValue<User> asFullUser(ItemValue<DirEntryAndValue<User>> itemValue) throws ServerFault {
		if (itemValue == null) {
			return null;
		} else {
			return ItemValue.create(itemValue, itemValue.value.value);
		}
	}

	@Override
	public ItemValue<User> getComplete(String uid) throws ServerFault {
		logger.debug("[{} @ {}] GET uid: {}", context.getSubject(), context.getContainerUid(), uid);

		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER);

		ItemValue<User> ret = getFull(uid);
		return filterUser(ret);
	}

	private ItemValue<User> filterUser(ItemValue<User> user) {
		if (user != null) {
			user.value.password = null;
		}

		return user;
	}

	@Override
	public ItemValue<User> byEmail(String email) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER);

		ItemValue<DirEntryAndValue<User>> itemValue = storeService.findByEmailFull(email);
		return filterUser(asFullUser(itemValue));
	}

	@Override
	public ItemValue<User> byLogin(String login) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER);

		ItemValue<User> ret = null;

		if (login.contains("@")) {
			login = login.split("@")[0];
		}

		String uid = storeService.findByLogin(login);
		if (uid != null) {
			ret = getComplete(uid);
		}

		return filterUser(ret);
	}

	@Override
	public ItemValue<User> byExtId(String extId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER);
		ParametersValidator.notNullAndNotEmpty(extId);

		return storeService.findByExtId(extId);
	}

	@Override
	public TaskRef delete(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER);

		return bmContext.provider().instance(ITasksManager.class).run(monitor -> performDelete(uid, monitor));

	}

	private void performDelete(String uid, IServerTaskMonitor monitor) {

		monitor.begin(2, "Deleting user " + uid + "@" + domainName);

		ItemValue<User> previousItem = getFull(uid);
		if (previousItem == null) {
			logger.warn("no user for {}@{}", uid, domainName);
			monitor.end(true, "no user for " + uid, JsonUtils.asString(""));
			return;
		}

		if (domainName.equalsIgnoreCase("global.virt") && previousItem.value.login.equals("admin0")) {
			logger.warn("Can't delete Admin0");
			monitor.end(true, "Can't delete admin0", JsonUtils.asString(""));
			return;
		}

		if (uid.equals(context.getSubject()) && domainName.equals(context.getContainerUid())) {
			monitor.end(false, "Cannot delete myself", JsonUtils.asString(""));
			return;
		}

		User previous = previousItem.value;

		for (IUserHook uh : userHooks) {
			uh.beforeDelete(bmContext, domainName, uid, previous);
		}

		List<String> groups = memberOfGroupUid(uid);
		IGroup groupService = bmContext.su().provider().instance(IGroup.class, domainName);
		List<Member> members = new ArrayList<>();
		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = uid;
		members.add(member);
		for (String group : groups) {
			groupService.remove(group, members);
		}

		for (IUserHook uh : userHooks) {
			try {
				uh.onUserDeleted(bmContext, domainName, createItemValue(uid, previous));
			} catch (Exception e) {
				// make hook error proof..
				if (logger.isDebugEnabled()) {
					logger.error("error during executing onUserDeleted {}/{} hook {}}", domainName, uid,
							uh.getClass().getName(), e);

				} else {
					logger.error("error during executing onUserDeleted {}/{} hook {} : message: {}", domainName, uid,
							uh.getClass().getName(), e.getMessage());

				}
			}
		}

		if (!globalVirt && !previous.system) {
			monitor.progress(1, "Deleting user mailbox ...");
			mailboxes.deleted(uid, mailboxAdapter.asMailbox(domainName, uid, previous));
			monitor.progress(2, "User mailbox deleted");

		}

		storeService.delete(uid);

		eventProducer.deleted(uid, previous);
		monitor.end(true, "User deleted", JsonUtils.asString(""));

	}

	private Date addDaysToDate(Date date, int amount) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, amount);

		return c.getTime();
	}

	private Date getToday() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		try {
			return sdf.parse(sdf.format(new Date()));
		} catch (ParseException e) {
			logger.error("Unable to get today date");
			throw new ServerFault("Unable to get today date");
		}
	}

	public boolean passwordUpdateNeeded(String login) {
		ParametersValidator.notNullAndNotEmpty(login);

		ItemValue<User> userItem = getUserFromLogin(login);

		if (userItem.value.passwordMustChange) {
			return true;
		}

		if (userItem.value.passwordNeverExpires) {
			return false;
		}

		Integer passwordLifetimeSetting;
		try {
			passwordLifetimeSetting = Integer
					.valueOf(bmContext.su().provider().instance(IDomainSettings.class, domainName).get()
							.get(DomainSettingsKeys.password_lifetime.name()));
		} catch (NumberFormatException nfe) {
			return false;
		}

		return (passwordLifetimeSetting > 0) && (userItem.value.passwordLastChange == null
				|| addDaysToDate(userItem.value.passwordLastChange, passwordLifetimeSetting)
						.compareTo(getToday()) <= 0);
	}

	public boolean checkPassword(String login, String password) {
		ParametersValidator.notNullAndNotEmpty(login);
		ParametersValidator.notNullAndNotEmpty(password);

		try {
			ItemValue<User> userItem = getUserFromLogin(login);

			// BM-9728
			if (userItem.value.password == null) {
				return false;
			}

			boolean valid = HashFactory.getByPassword(userItem.value.password).validate(password,
					userItem.value.password);
			updatePasswordAlgorithm(userItem, valid, userItem.value.password, password);
			return valid;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	private ItemValue<User> getUserFromLogin(String login) {
		if (login.contains("@")) {
			login = login.split("@")[0];
		}

		String uid = storeService.findByLogin(login);
		if (uid == null) {
			throw new ServerFault(String.format("Unable to get user UID from login %s", login));
		}
		ItemValue<User> userItem = storeService.get(uid);
		if (userItem == null) {
			throw new ServerFault(String.format("Unable to get user from uid %s", uid));
		}

		return userItem;
	}

	private void updatePasswordAlgorithm(ItemValue<User> userItem, boolean valid, String password, String passwordPlain)
			throws ServerFault {
		User user = userItem.value;
		// handle password using older passwordAlgorithm (3.0 -> 3.5, MD5 ->
		// PBKDF2)
		if (valid && !HashFactory.usesDefaultAlgorithm(password)) {
			if (logger.isInfoEnabled()) {
				logger.info("Updating password algorithm of user {} from {} to {}", user.login,
						HashFactory.algorithm(password), HashFactory.DEFAULT.name());
			}
			storeService.setPassword(userItem.uid, HashFactory.getDefault().create(passwordPlain), false);
		}
	}

	public boolean checkApiKey(String userUid, String sid) {
		try {
			return apikeyStore.check(userUid, sid);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public List<ItemValue<Group>> memberOf(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER, BasicRoles.ROLE_MANAGE_GROUP_MEMBERS);

		List<String> groupsUid = memberOfGroupUid(uid);

		ArrayList<ItemValue<Group>> groups = new ArrayList<>();
		IGroup groupService = bmContext.provider().instance(IGroup.class, domainName);

		for (String groupUid : groupsUid) {
			groups.add(groupService.getComplete(groupUid));
		}

		return groups;
	}

	@Override
	public List<String> memberOfGroups(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER, BasicRoles.ROLE_MANAGE_GROUP_MEMBERS);

		return memberOfGroupUid(uid);
	}

	private List<String> memberOfGroupUid(String uid) throws ServerFault {
		Item item = null;

		try {
			item = storeService.getItemStore().get(uid);
		} catch (SQLException sqle) {
			logger.error("Fail to get item {}", uid, sqle);
			throw new ServerFault(sqle);
		}

		if (item == null) {
			throw new ServerFault("Invalid user UID: " + uid);
		}

		try {
			return groupStore.getUserGroups(userContainer, item);
		} catch (SQLException e) {
			logger.error("Unable to get groups for user {}", uid, e);
			throw ServerFault.sqlFault(e);
		}

	}

	@Override
	public ValidationResult validate(String[] usersUids) throws ServerFault {
		boolean valid = storeService.allValid(usersUids);
		if (valid) {
			return new ValidationResult(valid, usersUids);
		} else {
			Map<String, Boolean> validationResults = new HashMap<>();
			for (String uid : usersUids) {
				validationResults.put(uid, storeService.allValid(new String[] { uid }));
			}
			return new ValidationResult(valid, validationResults);
		}
	}

	@Override
	public List<String> allUids() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER);
		return storeService.allUids();
	}

	@Override
	public void setRoles(String uid, Set<String> roles) throws ServerFault {
		RBACManager entryRbac = rbacManager.forEntry(uid);
		entryRbac.check(BasicRoles.ROLE_MANAGE_USER);

		if (roles == null) {
			roles = Collections.emptySet();
		}

		ItemValue<User> userItem = storeService.get(uid);
		if (userItem == null) {
			throw new ServerFault("user " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		if (userItem.value.accountType == AccountType.SIMPLE) {
			throw new ServerFault("Cannot set role for user " + uid, ErrorCode.FORBIDDEN);
		}

		HashSet<String> rolesToCheck = new HashSet<>(roles);
		HashSet<String> selfRolesToCheck = new HashSet<>();

		Set<RoleDescriptor> allRoles = bmContext.provider().instance(IRoles.class).getRoles();
		for (RoleDescriptor role : allRoles) {
			if (role.delegable) {
				rolesToCheck.remove(role.id);
			}

			if (rolesToCheck.contains(role.id) && role.selfPromote && role.parentRoleId != null) {
				rolesToCheck.remove(role.id);
				selfRolesToCheck.add(role.parentRoleId);
			}
		}

		// do not check already assigned roles
		Set<String> previousRoles = storeService.getRoles(uid);
		rolesToCheck.removeAll(previousRoles);

		if (!rbacManager.can(BasicRoles.ROLE_SYSTEM_MANAGER) && //
				!(
				// we can only delegate roles we have
				(rolesToCheck.isEmpty() || rbacManager.roles().containsAll(rolesToCheck)) && //
				// self roles can be delegated if "no self version" is availbe
				// throu this user
						(selfRolesToCheck.isEmpty() || entryRbac.roles().containsAll(selfRolesToCheck)))) {

			Set<String> neededRoles = ImmutableSet.<String>builder()
					.addAll(Sets.difference(rolesToCheck, rbacManager.roles()))
					.addAll(Sets.difference(selfRolesToCheck, entryRbac.roles())).build();
			throw new ServerFault("cannot assign roles which current user doesnt have (needed roles {"
					+ String.join(",", neededRoles) + "} )", ErrorCode.PERMISSION_DENIED);
		}

		storeService.setRoles(uid, roles);
	}

	@Override
	public Set<String> getRoles(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER);

		return storeService.getRoles(uid);
	}

	@Override
	public Set<String> getResolvedRoles(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER);

		if (getFull(uid) == null) {
			throw new ServerFault("user " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		List<String> groupList = memberOfGroupUid(uid);
		return directResolvedRoles(uid, groupList);
	}

	@Override
	public Set<String> directResolvedRoles(String uid, List<String> groups) throws ServerFault {
		ItemValue<User> userItem = getFull(uid);
		User user = userItem.value;
		if (passwordUpdateNeeded(user.login)) {
			return DefaultRoles.USER_PASSWORD_EXPIRED;
		}

		Set<String> roles = UserAccountFactory.get(user.accountType).sanitizeRoles(bmContext,
				storeService.getRoles(uid), domainName, userItem, groups);

		IInternalRoles roleService = bmContext.su().provider().instance(IInternalRoles.class);
		roles = roleService.filter(roles);
		return roleService.resolve(roles);
	}

	@Override
	public Set<String> getUsersWithRoles(List<String> roles) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER);
		return storeService.getItemsWithRoles(roles);
	}

	@Override
	public void setPassword(String uid, ChangePassword password) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(uid);
		ParametersValidator.notNull(password);
		ParametersValidator.notNull(password.newPassword);
		passwordValidator.validate(password.currentPassword, password.newPassword);

		ItemValue<User> userItem = storeService.get(uid);
		if (userItem == null) {
			throw new ServerFault("user uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		for (IPasswordUpdater ipu : userPasswordUpdaters) {
			boolean ret = ipu.update(context, domainName, userItem, password);
			if (ret) {
				break;
			}
		}
	}

	public void updatePassword(String uid, ChangePassword password) throws ServerFault {
		if (StringUtils.isBlank(password.currentPassword)) {
			rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_PASSWORD);
			setPassword(uid, password.newPassword);
		} else {
			changePassword(uid, password.currentPassword, password.newPassword);
		}
	}

	private void changePassword(String uid, String currentPassword, String newPassword) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF_CHANGE_PASSWORD, BasicRoles.ROLE_MANAGE_USER_PASSWORD);

		ParametersValidator.notNull(newPassword);
		passwordValidator.validate(newPassword);

		ItemValue<User> userItem = storeService.get(uid);
		if (userItem == null) {
			throw new ServerFault("user uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		if (!checkPassword(userItem.value.login, currentPassword)) {
			throw new ServerFault("password is not valid " + uid, ErrorCode.AUTHENTICATION_FAIL);
		}

		storeService.setPassword(uid, HashFactory.getDefault().create(newPassword), true);
		eventProducer.passwordUpdated(uid);
		// ysnp cache invalidation
		MQ.getProducer(Topic.CORE_SESSIONS).send(
				new JsonObject().put("latd", userItem.value.login + "@" + domainName).put("operation", "pwchange"));
	}

	private void setPassword(String uid, String newPassword) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_PASSWORD);

		passwordValidator.validate(newPassword);

		ItemValue<User> userItem = storeService.get(uid);
		if (userItem == null) {
			throw new ServerFault("user uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}
		// we support setting the user password as a hash, directly
		// this is used for external user importers
		if (HashFactory.algorithm(newPassword) != HashAlgorithm.UNKNOWN) {
			storeService.setPassword(uid, newPassword, true);
		} else {
			storeService.setPassword(uid, HashFactory.getDefault().create(newPassword), true);
		}

		eventProducer.passwordUpdated(uid);
		// ysnp cache invalidation
		MQ.getProducer(Topic.CORE_SESSIONS).send(
				new JsonObject().put("latd", userItem.value.login + "@" + domainName).put("operation", "pwchange"));
	}

	@Override
	public void setPhoto(String uid, byte[] photo) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_VCARD);

		ItemValue<User> ret = storeService.get(uid);

		if (ret == null) {
			throw new ServerFault("user " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		byte[] asPng = ImageUtils.checkAndSanitize(photo);
		byte[] icon = ImageUtils.resize(asPng, 22, 22);
		storeService.setPhoto(uid, asPng, icon);
		eventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void deletePhoto(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_VCARD);

		if (storeService.hasPhoto(uid)) {
			storeService.deletePhoto(uid);
			eventProducer.changed(uid, storeService.getVersion());
		}
	}

	@Override
	public byte[] getPhoto(String uid) throws ServerFault {
		return storeService.getPhoto(uid);
	}

	@Override
	public byte[] getIcon(String uid) throws ServerFault {
		return storeService.getIcon(uid);
	}

	@Override
	public void updateVCard(String uid, VCard userVCard) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_VCARD);

		ItemValue<User> previous = getFull(uid);
		if (previous == null) {
			throw new ServerFault("user " + uid + " not found in domain " + domainName, ErrorCode.NOT_FOUND);
		}

		sanitizer.create(userVCard);
		validator.create(userVCard);
		previous.value.contactInfos = userVCard;
		storeService.updateVCard(uid, previous.value);
		eventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public VCard getVCard(String uid) throws ServerFault {

		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER);
		ItemValue<DirEntryAndValue<User>> itemValue = storeService.get(uid, null);
		if (itemValue != null) {
			return itemValue.value.vcard;
		} else {
			return null;
		}
	}

	@Override
	public void deleteUserIdentitiesForMailbox(String mailboxUid) throws ServerFault {
		storeService.deleteMailboxIdentities(mailboxUid);
	}

	@Override
	public void deleteUserIdentitiesForMailbox(String userUid, String mailboxUid) throws ServerFault {
		storeService.deleteMailboxIdentities(userUid, mailboxUid);
	}

	@Override
	public void setExtId(String uid, String extId) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER);

		ItemValue<User> previous = getFull(uid);
		if (previous == null) {
			throw new ServerFault("user " + uid + " not found in domain " + domainName, ErrorCode.NOT_FOUND);
		}
		storeService.setExtId(uid, extId);
		eventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void updateAccountType(String uid, AccountType accountType) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER);

		if (accountType != null) {
			DirEntryHandlers.byKind(BaseDirEntry.Kind.USER).updateAccountType(bmContext, domainName, uid, accountType);
			for (IUserHook uh : userHooks) {
				uh.onAccountTypeUpdated(bmContext, domainName, uid, accountType);
			}
			eventProducer.changed(uid, storeService.getVersion());
		}
	}

	private MailFilter transformExternalEmailsToForwards(User user, MailFilter filter) {
		if (filter == null) {
			filter = new MailFilter();
		}
		if (user.routing != Routing.none) {
			return filter;
		}
		user.emails = new ArrayList<>(user.emails);
		user.routing = Routing.internal;
		List<String> domainAndAliases = new ArrayList<>(domain.aliases);
		domainAndAliases.add(domain.name);

		Iterator<Email> userEmailIter = user.emails.iterator();
		while (userEmailIter.hasNext()) {
			Email mail = userEmailIter.next();
			if (isExternalEmail(domainAndAliases, mail)) {
				filter.forwarding.emails.add(mail.address);
				userEmailIter.remove();
			}
		}

		filter.forwarding.enabled = !filter.forwarding.emails.isEmpty();
		return filter;
	}

	private boolean isExternalEmail(List<String> domainAndAliases, Email mail) {
		return !mail.allAliases && !domainAndAliases.contains(mail.domainPart());
	}

	@Override
	public User get(String uid) {
		ItemValue<User> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<User> item, boolean isCreate) {
		if (isCreate) {
			createWithItem(item);
		} else {
			updateWithItem(item);
		}
	}

}
