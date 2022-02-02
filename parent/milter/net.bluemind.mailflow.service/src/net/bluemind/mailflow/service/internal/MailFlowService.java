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
package net.bluemind.mailflow.service.internal;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailflow.api.IMailflowRules;
import net.bluemind.mailflow.api.MailActionDescriptor;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.hook.IMailflowHook;
import net.bluemind.mailflow.persistence.MailFlowStore;
import net.bluemind.mailflow.rbe.CoreClientContext;
import net.bluemind.mailflow.rbe.MailflowRuleEngine;
import net.bluemind.mailflow.service.MailFlowRegistry;
import net.bluemind.role.api.BasicRoles;

public class MailFlowService implements IMailflowRules {

	private static final Logger logger = LoggerFactory.getLogger(MailFlowService.class);
	private final Validator validator;
	private final Sanitizer sanitizer;
	private final MailFlowStore store;
	private final RBACManager rbacManager;
	private String domainUid;

	private static List<IMailflowHook> hooks = getHooks();

	public MailFlowService(BmContext context, String domainUid) {
		this.validator = new Validator(context);
		this.sanitizer = new Sanitizer(context);
		this.store = new MailFlowStore(context.getDataSource(), domainUid);
		rbacManager = new RBACManager(context).forDomain(domainUid);
		this.domainUid = domainUid;
	}

	private static List<IMailflowHook> getHooks() {
		RunnableExtensionLoader<IMailflowHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.mailflow", "mailflowHook", "hook", "class");
	}

	@Override
	public void create(String uid, MailRuleActionAssignmentDescriptor assignment) {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		logger.debug("Creating mailflow rule {}:{}", uid, assignment.description);
		sanitizer.create(assignment);
		validator.create(assignment);

		store.create(uid, assignment);
		hooks.forEach(hook -> hook.onCreate(domainUid, uid, assignment));
		EmitMailflowEvent.invalidateConfig(domainUid);
	}

	@Override
	public void update(String uid, MailRuleActionAssignmentDescriptor assignment) {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		logger.debug("Updating mailflow rule {}:{}", uid, assignment.description);
		sanitizer.create(assignment);
		validator.create(assignment);

		store.reCreate(uid, assignment);
		hooks.forEach(hook -> hook.onUpdate(domainUid, uid, assignment));
		EmitMailflowEvent.invalidateConfig(domainUid);
	}

	@Override
	public void delete(String uid) {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		logger.debug("Deleting mailflow rule {}", uid);
		MailRuleActionAssignmentDescriptor assignment = store.get(uid);
		store.delete(uid);
		hooks.forEach(hook -> hook.onDelete(domainUid, uid, assignment));
		EmitMailflowEvent.invalidateConfig(domainUid);
	}

	@Override
	public List<MailActionDescriptor> listActions() {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		return MailFlowRegistry.getActions();
	}

	@Override
	public List<MailRuleDescriptor> listRules() {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		return MailFlowRegistry.getRules();
	}

	@Override
	public List<MailRuleActionAssignment> listAssignments() {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		return store.getAll();
	}

	@Override
	public MailRuleActionAssignment getAssignment(String uid) throws ServerFault {
		return store.get(uid);
	}

	@Override
	public List<MailRuleActionAssignment> evaluate(Message message) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_ADMIN);
		return new MailflowRuleEngine(new CoreClientContext(detectSenderDomain(message)))
				.evaluate(listAssignments(), message).stream().map(ra -> ra.assignment).collect(Collectors.toList());
	}

	private ItemValue<Domain> detectSenderDomain(Message message) {
		String from = message.sendingAs.from;
		String domainPart = from.substring(from.indexOf("@") + 1);
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.findByNameOrAliases(domainPart);
	}

	@Override
	public MailRuleActionAssignmentDescriptor get(String uid) {
		return getAssignment(uid);
	}

	@Override
	public void restore(ItemValue<MailRuleActionAssignmentDescriptor> item, boolean isCreate) {
		if (isCreate) {
			create(item.uid, item.value);
		} else {
			update(item.uid, item.value);
		}

	}

}
