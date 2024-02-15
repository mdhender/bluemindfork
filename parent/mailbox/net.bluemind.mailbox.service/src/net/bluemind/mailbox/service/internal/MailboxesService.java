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
package net.bluemind.mailbox.service.internal;

import static java.util.stream.Collectors.toSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import net.bluemind.backend.mail.api.IUserInbox;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.utils.SubtreeContainerItemIdsCache;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.ReservedIds;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.MailboxConfig;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.api.rules.DelegationRule;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.MailFilterRule.Type;
import net.bluemind.mailbox.api.rules.MailFilterRuleForwardingMapper;
import net.bluemind.mailbox.api.rules.MailFilterRuleVacationMapper;
import net.bluemind.mailbox.api.rules.RuleMoveDirection;
import net.bluemind.mailbox.api.rules.RuleMoveRelativePosition;
import net.bluemind.mailbox.hook.IMailboxHook;
import net.bluemind.mailbox.persistence.DomainMailFilterStore;
import net.bluemind.mailbox.persistence.MailboxStore;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MailboxesService implements IMailboxes, IInCoreMailboxes {
	private static final Logger logger = LoggerFactory.getLogger(MailboxesService.class);
	private static final IMailboxesStorage mailboxStorage = getMailStorage();
	private static final List<IMailboxHook> hooks = getHooks();

	private MailboxStoreService storeService;
	private MailboxSanitizer sanitizer;
	private MailboxValidator validator;
	private MailboxesEventProducer eventProducer;
	private SecurityContext securityContext;
	private MailboxStore mailboxStore;
	private String domainUid;
	private BmContext context;
	private DomainMailFilterStore domainMailFilterStore;
	private Validator objectValidator;
	private Sanitizer objectSanitizer;
	private RBACManager rbacManager;
	private ItemValue<Domain> domain;
	private Container container;
	private final MailFilterRuleVacationMapper vacationMapper;
	private final MailFilterRuleForwardingMapper forwardingMapper;

	public MailboxesService(BmContext context, Container container, ItemValue<Domain> domain) {
		this.context = context;
		this.domain = domain;
		this.container = container;
		this.objectSanitizer = new Sanitizer(context);
		this.objectValidator = new Validator(context);
		this.domainUid = domain.uid;
		mailboxStore = new MailboxStore(context.getDataSource(), container);

		storeService = new MailboxStoreService(context.getDataSource(), context.getSecurityContext(), this.container);

		eventProducer = new MailboxesEventProducer(container.uid, context.getSecurityContext(),
				VertxPlatform.eventBus());

		sanitizer = new MailboxSanitizer(domain);

		validator = new MailboxValidator(context, domainUid, mailboxStore, storeService.getItemStore());

		this.securityContext = context.getSecurityContext();

		this.domainMailFilterStore = new DomainMailFilterStore(context.getDataSource(), container);
		this.vacationMapper = new MailFilterRuleVacationMapper();
		this.forwardingMapper = new MailFilterRuleForwardingMapper();

		rbacManager = new RBACManager(context).forDomain(domainUid);
	}

	@Override
	@VisibleForTesting
	public void create(String uid, Mailbox value) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		logger.info("[{} @ {}] CREATE uid: {}", securityContext.getSubject(), securityContext.getContainerUid(), uid);
		sanitizer.sanitize(value);
		validator.validate(value, uid);
		// FIXME juste attach
		storeService.attach(uid, null, value);
		created(uid, value, null);
	}

	@Override
	public void update(String uid, Mailbox value) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILBOX);

		logger.info("[{} @ {}] UPDATE uid: {}", securityContext.getSubject(), securityContext.getContainerUid(), uid);

		sanitizer.sanitize(value);
		validator.validate(value, uid);

		ItemValue<Mailbox> previousItemValue = storeService.get(uid, null);
		if (previousItemValue == null) {
			throw new ServerFault("mailbox " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		storeService.update(uid, null, value);
		updated(uid, previousItemValue.value, value, null);
	}

	@Override
	public ItemValue<Mailbox> getComplete(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILBOX);

		if (logger.isDebugEnabled()) {
			logger.debug("[{} @ {}] GET uid: {}", securityContext.getSubject(), securityContext.getContainerUid(), uid);
		}
		ItemValue<Mailbox> ret = storeService.get(uid, null);
		if (ret != null && ret.value == null) {
			return null;
		} else {
			return ret;
		}
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILBOX);

		ItemValue<Mailbox> itemValue = storeService.get(uid, null);

		if (itemValue != null && itemValue.value != null) {
			storeService.detach(uid);
			deleted(uid, itemValue.value);
		}
		// FIXME should throw if itemValue doesnt exists!
	}

	private void deleteMailboxesAclsContainer(String uid) throws ServerFault {
		String mailboxAclsContainerUid = IMailboxAclUids.uidForMailbox(uid);
		if (context.su().provider().instance(IContainers.class).getLightIfPresent(mailboxAclsContainerUid) == null) {
			logger.warn("no mailboxacl found for mailbox {}@{}", uid, domainUid);
			return;
		}
		context.su().provider().instance(IContainers.class).delete(mailboxAclsContainerUid);

	}

	@Override
	public ItemValue<Mailbox> byEmail(String email) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		String[] split = email.split("@");
		if (split.length != 2) {
			throw new ServerFault("email is not valid", ErrorCode.INVALID_PARAMETER);
		}
		String domainName = split[1];
		// email not in domain
		if (!domainName.equals(domainUid) && !domain.value.aliases.contains(domainName)) {
			return null;
		}
		String uid;
		try {
			uid = mailboxStore.emailSearch(email);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (uid != null) {
			return storeService.get(uid, null);
		} else {
			return null;
		}
	}

	@Override
	public List<String> byType(Mailbox.Type type) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		try {
			return mailboxStore.typeSearch(type);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public ItemValue<Mailbox> byName(String name) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		String uid;
		try {
			uid = mailboxStore.nameSearch(name);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (uid != null) {
			return storeService.get(uid, null);
		} else {
			return null;
		}
	}

	@Override
	public MailFilter getDomainFilter() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_READ_DOMAIN_FILTER, BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		try {
			return domainMailFilterStore.get();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void setDomainFilter(MailFilter filter) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		objectValidator.create(filter);
		try {
			domainMailFilterStore.set(filter);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		for (IMailboxHook hook : hooks) {
			hook.onDomainMailFilterChanged(context, domainUid, filter);
		}

	}

	@Override
	public List<MailFilterRule> getDomainRules() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_READ_DOMAIN_FILTER, BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		try {
			return domainMailFilterStore.get().rules;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public MailFilterRule getDomainRule(long id) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_READ_DOMAIN_FILTER, BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		MailFilterRule rule;
		try {
			rule = domainMailFilterStore.getRule(id);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (rule == null) {
			throw new ServerFault("Rule with id " + id + " not found", ErrorCode.NOT_FOUND);
		}
		return rule;
	}

	@Override
	public Long addDomainRule(MailFilterRule rule) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_READ_DOMAIN_FILTER, BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		try {
			validateDomainMailFilterRule(rule);
			long id = domainMailFilterStore.addRule(rule);
			onDomainMailFilterRuleChanged();
			return id;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void updateDomainRule(long id, MailFilterRule rule) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_READ_DOMAIN_FILTER, BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		try {
			MailFilterRule previousRule = domainMailFilterStore.getRule(id);
			if (previousRule == null) {
				throw new ServerFault("Rule with id " + id + " not found", ErrorCode.NOT_FOUND);
			}

			validateDomainMailFilterRule(rule);
			domainMailFilterStore.updateRule(id, rule);
			onDomainMailFilterRuleChanged();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteDomainRule(long id) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_READ_DOMAIN_FILTER, BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		try {
			domainMailFilterStore.deleteRule(id);
			onDomainMailFilterRuleChanged();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private void validateDomainMailFilterRule(MailFilterRule rule) {
		MailFilter filter = new MailFilter();
		filter.rules = Arrays.asList(rule);
		objectValidator.create(filter);
	}

	private void onDomainMailFilterRuleChanged() {
		MailFilter filter = getDomainFilter();
		for (IMailboxHook hook : hooks) {
			hook.onDomainMailFilterChanged(context, domainUid, filter);
		}
	}

	@Override
	public MailFilter.Vacation getMailboxVacation(String mailboxUid) throws ServerFault {
		MailFilter filter = getMailboxFilter(mailboxUid);
		return filter.vacation;
	}

	@Override
	public void setMailboxVacation(String mailboxUid, MailFilter.Vacation vacation) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		validateMailFilterVacation(mailboxUid, vacation);
		vacationMapper.map(vacation)
				.ifPresent(rule -> getMailboxRules(mailboxUid).stream().filter(r -> r.type == Type.VACATION).findFirst()
						.ifPresentOrElse(existingRule -> updateMailboxRule(mailboxUid, existingRule.id, rule),
								() -> addMailboxRule(mailboxUid, rule)));
	}

	@Override
	public MailFilter.Forwarding getMailboxForwarding(String mailboxUid) {
		MailFilter filter = getMailboxFilter(mailboxUid);
		return filter.forwarding;
	}

	@Override
	public void setMailboxForwarding(String mailboxUid, MailFilter.Forwarding forwarding) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		validateMailFilterForwarding(mailboxUid, forwarding);
		forwardingMapper.map(forwarding)
				.ifPresent(rule -> getMailboxRules(mailboxUid).stream().filter(r -> r.type == Type.FORWARD).findFirst()
						.ifPresentOrElse(existingRule -> updateMailboxRule(mailboxUid, existingRule.id, rule),
								() -> addMailboxRule(mailboxUid, rule)));
	}

	@Override
	public MailFilter getMailboxFilter(String mailboxUid) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		MailFilter filter = storeService.getFilter(mailboxUid);

		filter.rules.stream() //
				.filter(rule -> MailFilterRule.Type.VACATION.equals(rule.type)) //
				.findFirst() //
				.ifPresentOrElse(rule -> {
					filter.rules.remove(rule);
					filter.vacation = vacationMapper.map(rule);
				}, () -> filter.vacation = new MailFilter.Vacation());

		filter.rules.stream() //
				.filter(rule -> MailFilterRule.Type.FORWARD.equals(rule.type)) //
				.findFirst() //
				.ifPresentOrElse(rule -> {
					filter.forwarding = forwardingMapper.map(rule);
					filter.rules.remove(rule);
				}, () -> filter.forwarding = new MailFilter.Forwarding());

		return filter;
	}

	@Override
	public void setMailboxFilter(String mailboxUid, MailFilter filter) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		ItemValue<Mailbox> mailbox = storeService.get(mailboxUid, null);

		if (mailbox == null) {
			throw new ServerFault("Mailbox " + mailboxUid + " not found", ErrorCode.NOT_FOUND);
		}

		MailFilter previous = getMailboxFilter(mailboxUid);
		objectSanitizer.update(previous, filter);
		objectValidator.update(previous, filter);
		MailFilterForwardRoleValidator specificMailFilterValidator = new MailFilterForwardRoleValidator(context, domain,
				mailboxUid);
		specificMailFilterValidator.update(previous, filter);

		vacationMapper.map(filter.vacation).ifPresent(rule -> {
			filter.rules = new ArrayList<>(filter.rules);
			filter.rules.add(rule);
		});
		forwardingMapper.map(filter.forwarding).ifPresent(rule -> {
			filter.rules = new ArrayList<>(filter.rules);
			filter.rules.add(rule);
		});

		storeService.setFilter(mailboxUid, filter);

		for (IMailboxHook hook : hooks) {
			hook.onMailFilterChanged(context, domainUid, mailbox, filter);
		}
	}

	@Override
	public List<MailFilterRule> getMailboxRules(String mailboxUid) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		return storeService.getFilter(mailboxUid).rules;
	}

	@Override
	public MailFilterRule getMailboxRule(String mailboxUid, long id) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		MailFilterRule rule = storeService.getFilterRule(mailboxUid, id);
		if (rule == null) {
			throw new ServerFault("Rule with id " + id + " not found", ErrorCode.NOT_FOUND);
		}
		return rule;
	}

	@Override
	public Long addMailboxRule(String mailboxUid, MailFilterRule rule) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		validateMailFilterRule(mailboxUid, rule);
		long id = storeService.addFilterRule(mailboxUid, rule);
		onMailFilterRuleChanged(mailboxUid);
		return id;
	}

	@Override
	public Long addMailboxRuleRelative(String mailboxUid, RuleMoveRelativePosition movePosition, long anchorId,
			MailFilterRule rule) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		MailFilterRule anchorRule = storeService.getFilterRule(mailboxUid, anchorId);
		if (anchorRule == null) {
			throw new ServerFault("Rule with id " + anchorId + " not found", ErrorCode.NOT_FOUND);
		}

		if (!rule.client.equals(anchorRule.client)) {
			throw new ServerFault(
					"New rule can't be added relative to rule id=" + anchorId + " as they don't share the same client",
					ErrorCode.INVALID_PARAMETER);
		}

		validateMailFilterRule(mailboxUid, rule);
		long newId = storeService.addFilterRule(mailboxUid, movePosition, anchorId, rule);
		onMailFilterRuleChanged(mailboxUid);
		return newId;
	}

	@Override
	public void updateMailboxRule(String mailboxUid, long id, MailFilterRule rule) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		MailFilterRule previousRule = storeService.getFilterRule(mailboxUid, id);
		if (previousRule == null) {
			throw new ServerFault("Rule with id " + id + " not found", ErrorCode.NOT_FOUND);
		}

		validateMailFilterRule(mailboxUid, rule);
		storeService.updateFilterRule(mailboxUid, id, rule);
		onMailFilterRuleChanged(mailboxUid);
	}

	@Override
	public void deleteMailboxRule(String mailboxUid, long id) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		storeService.deleteFilterRule(mailboxUid, id);
		onMailFilterRuleChanged(mailboxUid);
	}

	@Override
	public void moveMailboxRule(String mailboxUid, long id, RuleMoveDirection moveDirection) {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		MailFilterRule previousRule = storeService.getFilterRule(mailboxUid, id);
		if (previousRule == null) {
			throw new ServerFault("Rule with id " + id + " not found", ErrorCode.NOT_FOUND);
		}

		storeService.moveFilterRule(mailboxUid, id, moveDirection);
		onMailFilterRuleChanged(mailboxUid);
	}

	@Override
	public void moveMailboxRuleRelative(String mailboxUid, long id, RuleMoveRelativePosition movePosition,
			long anchorId) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		MailFilterRule previousRule = storeService.getFilterRule(mailboxUid, id);
		if (previousRule == null) {
			throw new ServerFault("Rule with id " + id + " not found", ErrorCode.NOT_FOUND);
		}

		MailFilterRule anchorRule = storeService.getFilterRule(mailboxUid, anchorId);
		if (anchorRule == null) {
			throw new ServerFault("Rule with id " + id + " not found", ErrorCode.NOT_FOUND);
		}

		if (!previousRule.client.equals(anchorRule.client)) {
			throw new ServerFault("Rule with id " + id + " can't be ordered relative to rule id=" + anchorId
					+ " as they don't share the same client", ErrorCode.INVALID_PARAMETER);
		}

		storeService.moveFilterRule(mailboxUid, id, movePosition, anchorId);
		onMailFilterRuleChanged(mailboxUid);
	}

	private void validateMailFilterVacation(String mailboxUid, Vacation vacation) {
		validateMailFilterRule(mailboxUid, Collections.emptyList(), vacation, new Forwarding());
	}

	private void validateMailFilterForwarding(String mailboxUid, Forwarding forwarding) {
		validateMailFilterRule(mailboxUid, Collections.emptyList(), new Vacation(), forwarding);
	}

	private void validateMailFilterRule(String mailboxUid, MailFilterRule rule) {
		validateMailFilterRule(mailboxUid, Arrays.asList(rule), new Vacation(), new Forwarding());
	}

	private void validateMailFilterRule(String mailboxUid, List<MailFilterRule> rules, Vacation vacation,
			Forwarding forwarding) {
		MailFilter filter = new MailFilter();
		filter.rules = rules;
		objectSanitizer.update(null, filter);
		objectValidator.update(null, filter);
		var specificMailFilterValidator = new MailFilterForwardRoleValidator(context, domain, mailboxUid);
		specificMailFilterValidator.update(null, filter);
	}

	private void onMailFilterRuleChanged(String mailboxUid) {
		MailFilter updatedFilter = getMailboxFilter(mailboxUid);
		ItemValue<Mailbox> mailbox = storeService.get(mailboxUid, null);
		for (IMailboxHook hook : hooks) {
			hook.onMailFilterChanged(context, domainUid, mailbox, updatedFilter);
		}
	}

	@Override
	public List<MailFilterRule> getMailboxRulesByClient(String mailboxUid, String client) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		return storeService.getFilter(mailboxUid).rules.stream().filter(r -> client.equals(r.client)).toList();
	}

	private IMailboxesStorage mailboxStorage() {
		return mailboxStorage;
	}

	private static List<IMailboxHook> getHooks() {
		RunnableExtensionLoader<IMailboxHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.mailbox", "hook", "hook", "class");
	}

	@Override
	public List<AccessControlEntry> getMailboxAccessControlList(String mailboxUid) throws ServerFault {
		rbacManager.forContainer(IMailboxAclUids.uidForMailbox(mailboxUid)).check(Verb.Manage.name());

		IContainerManagement cmgmt = context.provider().instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(mailboxUid));

		return cmgmt.getAccessControlList();
	}

	@Override
	public void setMailboxAccessControlList(String mailboxUid, List<AccessControlEntry> accessControlEntries)
			throws ServerFault {
		rbacManager.forContainer(IMailboxAclUids.uidForMailbox(mailboxUid)).check(Verb.Manage.name());

		IContainerManagement cmgmt = context.provider().instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(mailboxUid));

		cmgmt.setAccessControlList(accessControlEntries);
	}

	@Override
	public Integer getUnreadMessagesCount() throws ServerFault {
		String userUid = context.getSecurityContext().getSubject();
		IUserInbox userInboxApi = context.provider().instance(IUserInbox.class, domainUid, userUid);
		return userInboxApi.unseen();
	}

	@Override
	public List<ItemValue<Mailbox>> list() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		return storeService.all().stream().filter(m -> m.value != null).collect(Collectors.toList());
	}

	@Override
	public List<String> listUids() {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		return storeService.allUids();
	}

	private static IMailboxesStorage getMailStorage() {
		return MailboxesStorageFactory.getMailStorage();
	}

	@Override
	public List<String> byRouting(Routing routing) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		try {
			return mailboxStore.routingSearch(routing);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void checkAvailabilty(Mailbox mailbox) throws ServerFault {
		try {
			if (mailboxStore.nameAlreadyUsed(null, mailbox)) {
				throw new ServerFault("Mail name: " + mailbox.name + " already used", ErrorCode.ALREADY_EXISTS);
			}
		} catch (SQLException sqle) {
			throw ServerFault.sqlFault(sqle);
		}

		try {
			if (mailboxStore.emailAlreadyUsed(null, mailbox.emails)) {
				// TODO: list addresses already used?
				// FIXME: update mailbox?
				throw new ServerFault("At least one email is already used", ErrorCode.ALREADY_EXISTS);
			}
		} catch (SQLException sqle) {
			throw ServerFault.sqlFault(sqle);
		}

	}

	@Override
	public MailboxQuota getMailboxQuota(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(Verb.Read.name(), BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_MAILBOX);

		if (logger.isDebugEnabled()) {
			logger.debug("[{} @ {}] GET uid: {}", securityContext.getSubject(), securityContext.getContainerUid(), uid);
		}
		ItemValue<Mailbox> mailbox = storeService.get(uid, null);
		// FIXME quota should be stored in database ( t_mailbox)
		if (mailbox.value != null) {
			return mailboxStorage.getQuota(context, domainUid, mailbox);
		} else {
			return new MailboxQuota();
		}

	}

	@Override
	public MailboxConfig getMailboxConfig(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(Verb.Read.name(), BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_MAILBOX);

		ItemValue<Mailbox> mailbox = storeService.get(uid, null);

		if (mailbox == null) {
			throw new ServerFault("mailbox " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		MailboxQuota quota = mailboxStorage.getQuota(context, domainUid, mailbox);
		SystemConf sysConf = context.su().provider().instance(ISystemConfiguration.class).getValues();

		MailboxConfig ret = new MailboxConfig();
		ret.messageMaxSize = sysConf.integerValue(SysConfKeys.message_size_limit.name());
		ret.quota = quota.quota;
		return ret;
	}

	@Override
	public void validate(String uid, Mailbox mailbox) throws ServerFault {
		this.validator.validate(mailbox, uid);
	}

	@Override
	public void sanitize(Mailbox mailbox) throws ServerFault {
		this.sanitizer.sanitize(mailbox);
	}

	@Override
	public void created(String uid, Mailbox mailbox, Consumer<ReservedIds> reservedIdsConsumer) throws ServerFault {
		if (reservedIdsConsumer != null) {
			reservedIdsConsumer.accept(reserveDefaultFolderIds(uid, null, mailbox));
		}

		Helper.createMailboxesAclsContainer(context, domainUid, uid, mailbox);
		ItemValue<Mailbox> itemValue = storeService.get(uid, null);

		for (IMailboxHook hook : hooks) {
			try {
				hook.preMailboxCreated(context, domainUid, itemValue);
			} catch (Exception e) {
				logger.error("error during call to hook (preMailboxCreated) {} : {} ", hook.getClass(), e.getMessage(),
						e);
			}
		}

		mailboxStorage().create(context, domainUid, itemValue);

		for (IMailboxHook hook : hooks) {
			try {
				hook.onMailboxCreated(context, domainUid, itemValue);
			} catch (Exception e) {
				logger.error("error during call to hook (onMailboxCreated) {} : {} ", hook.getClass(), e.getMessage(),
						e);
			}
		}
		eventProducer.created(uid);
	}

	private ReservedIds reserveDefaultFolderIds(String uid, Mailbox previous, Mailbox current) {
		if (current.dataLocation == null || !mailboxRequiresIdsReservations(context, domainUid, previous, current)) {
			logger.debug("IDRES {} Mailbox {} does not require ids reservations for folders.", uid, current);
			return null;
		}

		IOfflineMgmt offlineMgmtApi = context.provider().instance(IOfflineMgmt.class, domainUid, uid);
		String subtreeUid = IMailReplicaUids.subtreeUid(domainUid, current.type, uid);
		ReservedIds reservedIds;
		Set<String> defaultFolderNames;
		switch (current.type) {
		case user:
			defaultFolderNames = defaultFolderNames(current, DefaultFolder.USER_FOLDERS);
			reservedIds = doReserveDefaultFolderIds(offlineMgmtApi, uid, subtreeUid, defaultFolderNames);
			break;
		case group:
		case resource:
		case mailshare:
			defaultFolderNames = defaultFolderNames(current, DefaultFolder.MAILSHARE_FOLDERS);
			reservedIds = doReserveDefaultFolderIds(offlineMgmtApi, uid, subtreeUid, defaultFolderNames);
			break;
		default:
			reservedIds = null;
		}
		logger.info("IDRES returning reservations {}", reservedIds);
		return reservedIds;
	}

	private boolean mailboxRequiresIdsReservations(BmContext context, String domainUid, Mailbox previous,
			Mailbox current) {
		return previous == null //
				// Ensure managed mailbox exist in storage
				|| notCreated(context, domainUid, previous, current);
	}

	private boolean notCreated(BmContext context, String domainUid, Mailbox previous, Mailbox current) {
		return !previous.equals(current) && previous.name.equals(current.name)
				&& !mailboxStorage().mailboxExist(context, domainUid, ItemValue.create("", current));
	}

	private ReservedIds doReserveDefaultFolderIds(IOfflineMgmt offlineMgmtApi, String uid, String subtreeUid,
			Set<String> defaultFolderNames) {
		IdRange idRange = offlineMgmtApi.allocateOfflineIds(defaultFolderNames.size());
		ReservedIds reservedIds = new ReservedIds();
		long id = idRange.globalCounter;
		for (String folderName : defaultFolderNames) {
			String folderKey = SubtreeContainerItemIdsCache.key(subtreeUid, folderName);
			long cachedId = SubtreeContainerItemIdsCache.putFolderIdIfMissing(folderKey, id);
			logger.info("IDRES [{}] pre-alloc {} {}", subtreeUid, folderKey, cachedId);
			reservedIds.add(folderKey, cachedId);
			id++;
		}
		return reservedIds;
	}

	private Set<String> defaultFolderNames(Mailbox mailbox, Set<DefaultFolder> folders) {
		Set<String> defaultFolderNames = folders.stream()
				.map(folder -> (mailbox.type.sharedNs) ? mailbox.name + "/" + folder.name : folder.name)
				.collect(toSet());
		String receiveFolderName = (mailbox.type.sharedNs) ? mailbox.name : "INBOX";
		defaultFolderNames.add(receiveFolderName);
		return defaultFolderNames;
	}

	@Override
	public void updated(String uid, Mailbox previous, Mailbox value, Consumer<ReservedIds> reservedIdsConsumer)
			throws ServerFault {
		ItemValue<Mailbox> previousItemValue = ItemValue.create(uid, previous);
		ItemValue<Mailbox> itemValue = storeService.get(uid, null);
		if (reservedIdsConsumer != null) {
			reservedIdsConsumer.accept(reserveDefaultFolderIds(uid, previousItemValue.value, itemValue.value));
		}

		for (IMailboxHook hook : hooks) {
			try {
				hook.preMailboxUpdate(context, domainUid, previousItemValue, itemValue);
			} catch (Exception e) {
				logger.error("error during call to hook (preMailboxUpdate) {} : {} ", hook.getClass(), e.getMessage(),
						e);
			}
		}
		IMailboxesStorage storage = mailboxStorage();
		logger.info("[{}] Update to {}", storage, itemValue);
		storage.update(context, domainUid, previousItemValue, itemValue);

		for (IMailboxHook hook : hooks) {
			try {
				hook.onMailboxUpdated(context, domainUid, previousItemValue, itemValue);
			} catch (Exception e) {
				logger.error("error during call to hook (onMailboxUpdated) {} : {} ", hook.getClass(), e.getMessage(),
						e);
			}
		}

		eventProducer.updated(uid);
	}

	@Override
	public void deleted(String uid, Mailbox mailbox) throws ServerFault {
		ItemValue<Mailbox> itemValue = ItemValue.create(uid, mailbox);

		for (IMailboxHook hook : hooks) {
			try {
				hook.preMailboxDeleted(context, domainUid, itemValue);
			} catch (Exception e) {
				logger.error("error during call to hook (preMailboxDeleted) {}: {} ", hook.getClass(), e.getMessage(),
						e);
			}
		}

		try {
			setMailboxAccessControlList(uid, Collections.emptyList());
			deleteMailboxesAclsContainer(uid);
		} catch (ServerFault sf) {
			if (ErrorCode.NOT_FOUND.equals(sf.getCode())) {
				logger.error("unable to remove access control list: {}", sf.getMessage());
			}
		}

		try {
			mailboxStorage().delete(context, domainUid, itemValue);
		} catch (Exception e) {
			logger.error("Unable to remove mailbox storage: {}", e.getMessage(), e);
		}

		for (IMailboxHook hook : hooks) {
			try {
				hook.onMailboxDeleted(context, domainUid, itemValue);
			} catch (Exception e) {
				logger.error("error during call to hook (onMailboxDeleted) {}: {} ", hook.getClass(), e.getMessage(),
						e);
			}
		}

		eventProducer.deleted(uid);
	}

	@Override
	public List<ItemValue<Mailbox>> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);
		return storeService.getMultiple(uids);
	}

	public static class Helper {

		private Helper() {

		}

		public static void createMailboxesAclsContainer(BmContext context, String domainUid, String uid, Mailbox box)
				throws ServerFault {
			if (null == box.name) {
				Email email = box.defaultEmail();
				if (null != email) {
					box.name = email.address;
				}
			}
			IContainers containers = context.su().provider().instance(IContainers.class);
			containers.create(IMailboxAclUids.uidForMailbox(uid), ContainerDescriptor
					.create(IMailboxAclUids.uidForMailbox(uid), box.name, uid, IMailboxAclUids.TYPE, domainUid, true));

			logger.info("initialized folders for {}", uid);
		}
	}

	@Override
	public void deleteEmailByAlias(String alias) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);
		if (domainUid.equals(alias)) {
			logger.error("No, won't delete email alias {} (it's the domainUid)", alias);
			throw new ServerFault("Can't delete the alias for @domainUid");
		} else {
			logger.info("Deleting emails for alias {}", alias);
			storeService.deleteEmailByAlias(alias);
		}
	}

	@Override
	public DelegationRule getMailboxDelegationRule(String mailboxUid) throws ServerFault {
		List<MailFilterRule> mailboxRulesByClient = getMailboxRulesByClient(mailboxUid, "system");
		return DelegationFilter.getDelegationFilterRule(mailboxRulesByClient, mailboxUid);
	}

	@Override
	public void setMailboxDelegationRule(String mailboxUid, DelegationRule delegationRule) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);

		ItemValue<Mailbox> mailbox = storeService.get(mailboxUid, null);
		if (mailbox == null) {
			throw new ServerFault("Mailbox " + mailboxUid + " not found", ErrorCode.NOT_FOUND);
		}

		MailFilter mailboxFilter = getMailboxFilter(mailboxUid);
		List<MailFilterRule> imipFilterRules = mailboxFilter.rules.stream()
				.filter(r -> DelegationFilter.isDelegationRule(r)).toList();
		if (imipFilterRules.size() > 1) {
			throw new ServerFault("Too many 'Copy iMIP to Delegates' rules found for mailbox " + mailboxUid);
		}

		DelegationFilter rule = DelegationFilter.createDelegateFilterWithConditions(delegationRule);

		delegationRule.delegateUids.forEach(uid -> {
			User delegateUser = context.su().provider().instance(IUser.class, domain.uid).get(uid);
			rule.addDelegateFilterRedirectAction(uid, delegateUser.emails, delegationRule.keepCopy);
		});

		if (delegationRule.readOnly) {
			rule.addDelegateFilterSetFlagAction();
		}

		if (imipFilterRules.size() == 1) {
			mailboxFilter.rules.remove(imipFilterRules.get(0));
		}
		mailboxFilter.rules.add(rule);
		setMailboxFilter(mailboxUid, mailboxFilter);
	}

}
