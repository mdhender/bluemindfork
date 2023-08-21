/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.milter.action.delegation;

import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailflowRouting;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.milter.action.MilterRuleActionsFactory;

public class DelegationRuleAction {

	public static class DelegationRuleActionFactory implements MilterRuleActionsFactory {

		@Override
		public MailRuleActionAssignment create() {
			MailRuleActionAssignment assignment = new MailRuleActionAssignment();
			assignment.actionIdentifier = "milter.delegation";
			assignment.routing = MailflowRouting.OUTGOING;
			assignment.isActive = true;
			assignment.position = 1;

			MailflowRule rule = new MailflowRule();
			rule.ruleIdentifier = "MatchAlwaysRule";
			assignment.rules = rule;

			return assignment;
		}

	}
}
