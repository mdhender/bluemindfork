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
package net.bluemind.mailflow.rbe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.mailflow.common.api.Message;

public class MailflowRuleEngine {

	private static final Logger logger = LoggerFactory.getLogger(MailflowRuleEngine.class);
	private final IClientContext context;

	public MailflowRuleEngine(IClientContext context) {
		this.context = context;
	}

	public List<RuleAction> evaluate(List<MailRuleActionAssignment> assignments, Message message) {

		Map<String, RuleAction> groupMatches = new HashMap<>();
		List<RuleAction> matches = new ArrayList<>();

		for (MailRuleActionAssignment ruleAssignment : assignments) {
			if (ruleAssignment.isActive) {
				MailflowRule rules = ruleAssignment.rules;
				Optional<MailRule> rule = MilterRulesRegistry.get(rules.ruleIdentifier, ruleAssignment);
				if (!rule.isPresent()) {
					logger.warn("Unable to find registered rule {}", rules.ruleIdentifier);
				} else {
					MailRuleEvaluation evaluatedRule;
					try {
						evaluatedRule = rule.get().evaluate(message, context);
					} catch (Exception e) {
						logger.warn("Rule {} failed by throwing an exception", rules.ruleIdentifier, e);
						evaluatedRule = MailRuleEvaluation.rejected();
					}
					if (evaluatedRule.matches) {
						RuleAction ruleAction = new RuleAction(ruleAssignment.position, evaluatedRule, ruleAssignment);
						String group = ruleAssignment.group;
						if (group != null && group.length() > 0) {
							if (groupMatches.containsKey(group)) {
								RuleAction current = groupMatches.get(group);
								if (ruleAction.priority < current.priority) {
									groupMatches.put(group, ruleAction);
								}
							} else {
								groupMatches.put(group, ruleAction);
							}
						} else {
							matches.add(new RuleAction(ruleAssignment.position, evaluatedRule, ruleAssignment));
						}
					}
				}
			}
		}

		matches.addAll(groupMatches.values());
		Collections.sort(matches, (a, b) -> Integer.compare(a.priority, b.priority));
		return matches;
	}

}
