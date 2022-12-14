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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import jakarta.ws.rs.PathParam;
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
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.directory.api.ReservedIds;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.MailboxConfig;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.MailFilterRuleForwardingMapper;
import net.bluemind.mailbox.api.rules.MailFilterRuleVacationMapper;
import net.bluemind.mailbox.hook.IMailboxHook;
import net.bluemind.mailbox.persistence.DomainMailFilterStore;
import net.bluemind.mailbox.persistence.MailboxStore;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation.DiagnosticReportCheckId;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class MailboxesService implements IMailboxes, IInCoreMailboxes {
	private static final Logger logger = LoggerFactory.getLogger(MailboxesService.class);
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
	private static final IMailboxesStorage mailboxStorage = getMailStorage();
	private static final List<IMailboxHook> hooks = getHooks();
	private final MailFilterRuleVacationMapper vacationMapper = new MailFilterRuleVacationMapper();
	private final MailFilterRuleForwardingMapper forwardingMapper = new MailFilterRuleForwardingMapper();

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
		try {
			context.su().provider().instance(IContainers.class).get(mailboxAclsContainerUid);
		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("no mailboxacl found for mailbox {}@{}", uid, domainUid);
				return;
			} else {
				throw e;
			}
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

		mailboxStorage().changeDomainFilter(context, domainUid, filter);
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

		MailFilter filter = getMailboxFilter(mailboxUid);
		filter.vacation = vacation;
		setMailboxFilter(mailboxUid, filter);
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

		MailFilter previous = storeService.getFilter(mailboxUid);
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

		mailboxStorage().changeFilter(context, domain, mailbox, filter);
		for (IMailboxHook hook : hooks) {
			hook.onMailFilterChanged(context, domainUid, mailbox, filter);
		}
	}

	@Override
	public List<MailFilterRule> getMailboxRules(@PathParam("mailboxUid") String mailboxUid) throws ServerFault {
		rbacManager.forEntry(mailboxUid).check(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER);
		return storeService.getFilter(mailboxUid).rules;
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
	public TaskRef checkAndRepairAll() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		String tuid = String.format("mbox_checkAndRepairAll-%s", domain.uid);
		return context.provider().instance(ITasksManager.class).run(tuid, new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				checkAndRepairAllTask(monitor);
				monitor.end(true, null, null);
			}

		});
	}

	@Override
	public TaskRef checkAll() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX);

		String tuid = String.format("mbox_checkAll-%s", domain.uid);
		return context.provider().instance(ITasksManager.class).run(tuid, new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {

				try {
					checkAllTask(monitor);
					monitor.end(true, null, null);
				} catch (Exception e) {
					monitor.end(false, e.getMessage(), null);
				}
			}

		});
	}

	@Override
	public TaskRef checkAndRepair(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILBOX);

		String tuid = String.format("mbox_checkAndRepair-%s-%s", domain.uid, uid);
		return context.provider().instance(ITasksManager.class).run(tuid, new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				try {

					checkAndRepairTask(uid, monitor, true);
					monitor.end(true, null, null);
				} catch (Exception e) {
					logger.error("error during check and repair of {}", uid, e);
					monitor.end(false, e.getMessage(), null);
				}
			}

		});
	}

	@Override
	public TaskRef check(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILBOX);
		String tuid = String.format("mbox_check-%s-%s", domain.uid, uid);
		return context.provider().instance(ITasksManager.class).run(tuid, new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				checkAndRepairTask(uid, monitor, false);
				monitor.end(true, null, null);
			}

		});
	}

	/**
	 * Use {@link IDirEntryMaintenance#repair(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * for (String entryUid: IDirectory.search(
	 * 			DirEntryQuery.filterKind(Kind.GROUP, Kind.MAILSHARE, Kind.RESOURCE, Kind.USER))
	 * 		.values.stream()
	 * 		.map(deiv -> deiv.uid)
	 * 		.collect()Collectors.toSet()) {
	 * 	IDirEntryMaintenance.repair(opsIds);
	 * }
	 * </code>
	 * </pre>
	 */
	@Deprecated
	public void checkAndRepairAllTask(IServerTaskMonitor monitor) throws ServerFault {
		List<String> uids = storeService.allUids();

		monitor.begin(uids.size(), "checking and repair mailboxes of " + domainUid);

		for (String uid : storeService.allUids()) {
			checkAndRepairTask(uid, monitor.subWork(1), true);
		}
	}

	/**
	 * Use {@link IDirEntryMaintenance#check(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * for (String entryUid: IDirectory.search(
	 * 			DirEntryQuery.filterKind(Kind.GROUP, Kind.MAILSHARE, Kind.RESOURCE, Kind.USER))
	 * 		.values.stream()
	 * 		.map(deiv -> deiv.uid)
	 * 		.collect()Collectors.toSet()) {
	 * 	IDirEntryMaintenance.check(opsIds);
	 * }
	 * </code>
	 * </pre>
	 */
	@Deprecated
	private void checkAllTask(IServerTaskMonitor monitor) throws ServerFault {
		List<String> uids = storeService.allUids();
		monitor.begin(uids.size(), "checking and repair mailboxes of " + domainUid);

		for (String uid : storeService.allUids()) {
			checkAndRepairTask(uid, monitor, false);
		}
	}

	/**
	 * Use {@link IDirEntryMaintenance#repair(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * IDirEntryMaintenance.repair(opsIds);
	 * </code>
	 * </pre>
	 */
	@Deprecated
	public void checkAndRepairTask(String uid, IServerTaskMonitor monitor) throws ServerFault {
		checkAndRepairTask(uid, monitor, true);
	}

	/**
	 * If not <code>repair</code>, use {@link IDirEntryMaintenance#check(Set)} If
	 * <code>repair</code>, use {@link IDirEntryMaintenance#repair(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * if (repair) {
	 * 	IDirEntryMaintenance.repair(opsIds);
	 * } else {
	 * 	IDirEntryMaintenance.check(opsIds);
	 * }
	 * </code>
	 * </pre>
	 */
	@Deprecated
	public void checkAndRepairTask(String uid, IServerTaskMonitor monitor, boolean repair) throws ServerFault {
		monitor.begin(1, String.format("Check and repair mailbox %s@%s", uid, domainUid));
		Set<String> opsIds = new HashSet<>(Arrays.asList(DiagnosticReportCheckId.mailboxExists.name(),
				DiagnosticReportCheckId.mailboxIndexExists.name(), DiagnosticReportCheckId.mailboxAclsContainer.name(),
				DiagnosticReportCheckId.mailboxAcls.name(), DiagnosticReportCheckId.mailboxHsm.name(),
				DiagnosticReportCheckId.mailboxFilters.name(), DiagnosticReportCheckId.mailboxPostfixMaps.name(),
				DiagnosticReportCheckId.mailboxAcls.name(), DiagnosticReportCheckId.mailboxImapHierarchy.name(),
				DiagnosticReportCheckId.mailboxFilters.name(), DiagnosticReportCheckId.mailboxPostfixMaps.name(),
				DiagnosticReportCheckId.mailboxSubscription.name()));

		TaskRef tr = context.su().provider().instance(IDirEntryMaintenance.class, domainUid, uid)
				.repair(RepairConfig.create(opsIds, !repair, true, true));
		ITask itask = context.su().provider().instance(ITask.class, tr.id);
		TaskUtils.forwardProgress(itask, monitor);
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
		if (current.dataLocation == null
				|| !mailboxStorage().mailboxRequiresCreationInCyrus(context, domainUid, previous, current)) {
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
		return reservedIds;
	}

	private ReservedIds doReserveDefaultFolderIds(IOfflineMgmt offlineMgmtApi, String uid, String subtreeUid,
			Set<String> defaultFolderNames) {
		IdRange idRange = offlineMgmtApi.allocateOfflineIds(defaultFolderNames.size());
		ReservedIds reservedIds = new ReservedIds();
		long id = idRange.globalCounter;
		for (String folderName : defaultFolderNames) {
			String folderKey = SubtreeContainerItemIdsCache.key(subtreeUid, folderName);
			long cachedId = SubtreeContainerItemIdsCache.putFolderIdIfMissing(folderKey, id);
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

}
