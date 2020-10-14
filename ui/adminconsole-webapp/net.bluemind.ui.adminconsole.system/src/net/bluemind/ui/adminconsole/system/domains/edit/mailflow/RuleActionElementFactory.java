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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow;

import java.util.List;

import com.google.gwt.user.client.ui.Panel;

import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.actions.AddSignatureConfig;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.actions.MailflowActionConfig;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.actions.UpdateSubjectConfig;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.CompositeTreeItem;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.DateBasedRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.RuleTreeItem;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.SenderInGroupRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.SenderInOuRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.SenderIsRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.SimpleRule;

public class RuleActionElementFactory {

	public static RuleTreeItem getRuleByIdentifier(RuleAssignmentWidget parent, String identifier,
			List<MailRuleDescriptor> ruleIdentifiers, Panel config, String domainUid) {
		MailRuleDescriptor descriptor = getDescriptorByIdentifier(ruleIdentifiers, identifier);
		switch (descriptor.ruleIdentifier) {
		case "MatchAlwaysRule":
		case "RecipientIsInternalRule":
		case "RecipientIsExternalRule":
			return new SimpleRule(parent, descriptor, ruleIdentifiers, config, domainUid);
		case "AndRule":
		case "OrRule":
		case "NotRule":
		case "XorRule":
			return new CompositeTreeItem(parent, descriptor, ruleIdentifiers, config, domainUid);
		case "SenderInOuRule":
			return new SenderInOuRule(parent, descriptor, ruleIdentifiers, config, domainUid);
		case "SenderInGroupRule":
			return new SenderInGroupRule(parent, descriptor, ruleIdentifiers, config, domainUid);
		case "SenderIsRule":
			return new SenderIsRule(parent, descriptor, ruleIdentifiers, config, domainUid);
		case "SendDateIsBefore":
		case "SendDateIsAfter":
			return new DateBasedRule(parent, descriptor, ruleIdentifiers, config, domainUid);
		default:
			throw new IllegalArgumentException("Unknown identifier " + identifier);
		}

	}

	private static MailRuleDescriptor getDescriptorByIdentifier(List<MailRuleDescriptor> ruleIdentifiers,
			String identifier) {
		return ruleIdentifiers.stream().filter(r -> r.ruleIdentifier.equals(identifier)).findFirst().get();
	}

	public static MailflowActionConfig getActionByIdentifier(String identifier) {
		switch (identifier) {
		case "AddSignatureAction":
			return new AddSignatureConfig();

		case "UpdateSubjectAction":
			return new UpdateSubjectConfig();

		default:
			throw new IllegalArgumentException("Unknown identifier " + identifier);
		}

	}

}
