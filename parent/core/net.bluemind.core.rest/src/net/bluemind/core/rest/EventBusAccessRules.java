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
package net.bluemind.core.rest;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

/**
 * load net.bluemind.core.rest.serviceFactory extensions
 *
 */
public class EventBusAccessRules {

	private static final EventBusAccessRules INSTANCE = new EventBusAccessRules();

	private List<IEventBusAccessRule> rules = null;

	private EventBusAccessRules() {
		rules = new RunnableExtensionLoader<IEventBusAccessRule>().loadExtensions("net.bluemind.core.rest",
				"eventBusAccessRule", "access-rule", "class");
	}

	public static EventBusAccessRules getInstance() {
		return INSTANCE;
	}

	public List<IEventBusAccessRule> getEventBusRules() {
		return rules;
	}

}
