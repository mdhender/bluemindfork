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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.Panel;

import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;

public class SimpleRule extends RuleTreeItem {

	public SimpleRule(RuleAssignmentWidget parent, MailRuleDescriptor descriptor,
			List<MailRuleDescriptor> ruleIdentifiers, Panel config, String domainUid) {
		super(parent, descriptor, config, domainUid);
	}

	@Override
	public MailflowRule toRule() {
		MailflowRule rule = new MailflowRule();
		rule.ruleIdentifier = super.ruleIdentifier;
		rule.configuration = Collections.emptyMap();
		return rule;
	}

	@Override
	public void set(Map<String, String> configuration) {
	}

}
