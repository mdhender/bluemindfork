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
package net.bluemind.mailflow.service.validators;

import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.validator.IValidator;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.mailflow.hook.IMailflowConfigValidator;
import net.bluemind.mailflow.service.MailFlowRegistry;

public class MailflowAssignmentValidator implements IValidator<MailRuleActionAssignmentDescriptor> {

	private static final List<IMailflowConfigValidator> hooks = getHooks();

	private static List<IMailflowConfigValidator> getHooks() {
		RunnableExtensionLoader<IMailflowConfigValidator> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.mailflow", "mailflowConfig", "hook", "impl");
	}

	@Override
	public void create(MailRuleActionAssignmentDescriptor assignment) throws ServerFault {
		List<String> actionIdentifiers = MailFlowRegistry.getActions().stream().map(a -> a.actionIdentifier)
				.collect(Collectors.toList());
		List<String> ruleIdentifiers = MailFlowRegistry.getRules().stream().map(r -> r.ruleIdentifier)
				.collect(Collectors.toList());

		if (!actionIdentifiers.contains(assignment.actionIdentifier)) {
			throw new ServerFault("Mailflow action identifier " + assignment.actionIdentifier + " not found");
		}

		validateRules(assignment.rules, ruleIdentifiers);
		validateConfiguration(assignment);
	}

	private void validateConfiguration(MailRuleActionAssignmentDescriptor assignment) {
		hooks.forEach(hook -> {
			if (hook.getAction().equals(assignment.actionIdentifier)) {
				hook.validate(assignment.actionConfiguration);
			}
		});
	}

	private void validateRules(MailflowRule rule, List<String> ruleIdentifiers) {
		if (!ruleIdentifiers.contains(rule.ruleIdentifier)) {
			throw new ServerFault("Mailflow rule identifier " + rule.ruleIdentifier + " not found");
		}

		for (MailflowRule child : rule.children) {
			validateRules(child, ruleIdentifiers);
		}

	}

	@Override
	public void update(MailRuleActionAssignmentDescriptor oldValue, MailRuleActionAssignmentDescriptor newValue)
			throws ServerFault {
		create(newValue);
	}

}