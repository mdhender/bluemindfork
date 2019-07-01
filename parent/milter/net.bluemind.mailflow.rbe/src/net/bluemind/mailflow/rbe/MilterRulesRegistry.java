/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailflowRule;

public class MilterRulesRegistry {
	private static Logger logger = LoggerFactory.getLogger(MilterRulesRegistry.class);

	private static List<MilterRuleFactory> loaded;

	static {
		init();
	}

	public final static Collection<MilterRuleFactory> getFactories() {
		return loaded;
	}

	private static final void init() {
		logger.info("loading net.bluemind.milter.rulefactory extensions");
		RunnableExtensionLoader<MilterRuleFactory> rel = new RunnableExtensionLoader<MilterRuleFactory>();
		loaded = rel.loadExtensionsWithPriority("net.bluemind.milter", "rulefactory", "rule_factory", "impl");
		logger.info("{} implementation found for extensionpoint net.bluemind.milter.rulefactory", loaded.size());

	}

	public static Optional<MailRule> get(String ruleIdentifier, MailRuleActionAssignmentDescriptor ruleAssignment) {
		Optional<MailRule> rootRule = getRule(ruleIdentifier);
		if (!rootRule.isPresent()) {
			logger.info("Trying to load rule {} failed", ruleIdentifier);
			return Optional.empty();
		}
		buildRule(rootRule.get(), ruleAssignment);
		return rootRule;
	}

	private static Optional<MailRule> getRule(String ruleIdentifier) {
		for (MilterRuleFactory factory : loaded) {
			if (factory.identifier().equals(ruleIdentifier)) {
				return Optional.of(factory.create());
			}
		}
		return Optional.empty();
	}

	private static void buildRule(MailRule rule, MailRuleActionAssignmentDescriptor ruleAssignment) {
		RuleVisitor v = new MailRuleVisitor(rule);
		ruleTraversal(ruleAssignment.rules, v);
	}

	private static void ruleTraversal(MailflowRule rule, RuleVisitor visitor) {
		boolean handled = visitor.visit(rule);
		if (handled) {
			return;
		}
		for (MailflowRule r : rule.children) {
			ruleTraversal(r, visitor);
		}
	}

	private static interface RuleVisitor {
		boolean visit(MailflowRule rule);
	}

	private static class MailRuleVisitor implements RuleVisitor {

		private final MailRule mailRule;

		public MailRuleVisitor(MailRule mailRule) {
			this.mailRule = mailRule;
		}

		@Override
		public boolean visit(MailflowRule rule) {
			if (rule.ruleIdentifier.equals(mailRule.identifier())) {
				mailRule.receiveConfiguration(rule.configuration);
				List<MailflowRule> children = rule.children;
				List<MailRule> materializedChildren = new ArrayList<>(children.size());
				for (MailflowRule mailflowRule : children) {
					MailRuleActionAssignmentDescriptor ruleAssignment = new MailRuleActionAssignmentDescriptor();
					ruleAssignment.rules = mailflowRule;
					Optional<MailRule> childRule = MilterRulesRegistry.get(mailflowRule.ruleIdentifier, ruleAssignment);
					if (childRule.isPresent()) {
						materializedChildren.add(childRule.get());
					} else {
						logger.warn("Unable to find registered sub-rule {}", mailflowRule.ruleIdentifier);
					}
				}
				mailRule.receiveChildren(materializedChildren);
				return true;
			} else {
				return false;
			}
		}

	}

}
