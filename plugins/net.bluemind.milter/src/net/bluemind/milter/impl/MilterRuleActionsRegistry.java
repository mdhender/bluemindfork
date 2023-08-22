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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.milter.impl;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.milter.action.MilterRuleActionsFactory;

public class MilterRuleActionsRegistry {
	private static Logger logger = LoggerFactory.getLogger(MilterRuleActionsRegistry.class);

	private static List<MailRuleActionAssignment> loaded;

	static {
		init();
	}

	public final static Collection<MailRuleActionAssignment> get() {
		return loaded;
	}

	private static final void init() {
		logger.info("loading net.bluemind.milter.ruleactionfactory extensions");
		RunnableExtensionLoader<MilterRuleActionsFactory> rel = new RunnableExtensionLoader<>();
		loaded = rel
				.loadExtensionsWithPriority("net.bluemind.milter", "ruleactionfactory", "rule_action_factory", "impl")
				.stream().map(f -> f.create()).toList();
		logger.info("{} implementations found for extensionpoint net.bluemind.milter.ruleactionfactory", loaded.size());
	}
}
