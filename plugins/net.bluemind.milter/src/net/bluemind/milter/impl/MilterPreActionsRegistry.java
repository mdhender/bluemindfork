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
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.milter.action.MilterPreAction;
import net.bluemind.milter.action.MilterPreActionsFactory;

public class MilterPreActionsRegistry {
	private static Logger logger = LoggerFactory.getLogger(MilterPreActionsRegistry.class);

	private static List<MilterPreAction> loaded;

	static {
		init();
	}

	public final static Collection<MilterPreAction> get() {
		return loaded;
	}

	private static final void init() {
		logger.info("loading net.bluemind.milter.preactionfactory extensions");
		RunnableExtensionLoader<MilterPreActionsFactory> rel = new RunnableExtensionLoader<MilterPreActionsFactory>();
		loaded = rel.loadExtensionsWithPriority("net.bluemind.milter", "preactionfactory", "pre_action_factory", "impl")
				.stream().map(f -> f.create()).filter(Objects::nonNull).collect(Collectors.toList());
		logger.info("{} implementation found for extensionpoint net.bluemind.milter.preactionfactory", loaded.size());
	}
}
