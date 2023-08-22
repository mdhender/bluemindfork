/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.milter.action.eventforwarding;

import java.util.Map;

import net.bluemind.mailflow.api.ExecutionMode;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailflowRouting;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.milter.action.MilterRuleActionsFactory;

public class EventForwardingFactory implements MilterRuleActionsFactory {

	@Override
	public MailRuleActionAssignment create() {
		MailRuleActionAssignment assignment = new MailRuleActionAssignment();
		assignment.actionIdentifier = EventForwardingAction.identifier;
		assignment.isActive = true;
		assignment.mode = ExecutionMode.CONTINUE;
		assignment.routing = MailflowRouting.OUTGOING;
		MailflowRule mailflowRule = new MailflowRule();
		mailflowRule.ruleIdentifier = "MatchAlwaysRule";
		mailflowRule.configuration = Map.of();
		assignment.rules = mailflowRule;
		return assignment;
	}

}
