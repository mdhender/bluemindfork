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
package net.bluemind.milter.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterActionsFactory;

public class MilterActionsRegistry {
	private static Logger logger = LoggerFactory.getLogger(MilterActionsRegistry.class);

	private static List<MilterActionsFactory> loaded;

	static {
		init();
	}

	public final static Collection<MilterActionsFactory> getFactories() {
		return loaded;
	}

	private static final void init() {
		logger.info("loading net.bluemind.milter.actionfactory extensions");
		RunnableExtensionLoader<MilterActionsFactory> rel = new RunnableExtensionLoader<MilterActionsFactory>();
		loaded = rel.loadExtensionsWithPriority("net.bluemind.milter", "actionfactory", "action_factory", "impl");
		logger.info("{} implementation found for extensionpoint net.bluemind.milter.actionfactory", loaded.size());
	}

	public static Optional<MilterAction> get(String actionIdentifier) {
		List<MilterActionsFactory> milterActionFactory = loaded.stream()
				.filter(r -> r.create().identifier().equals(actionIdentifier)).collect(Collectors.toList());
		if (milterActionFactory.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(milterActionFactory.get(0).create());
	}

}
