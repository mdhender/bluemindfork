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
package net.bluemind.system.service.hooks;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class SystemConfigurationHooks {

	private static final SystemConfigurationHooks INSTANCE = new SystemConfigurationHooks();

	public static SystemConfigurationHooks getInstance() {
		return INSTANCE;
	}

	private List<ISystemConfigurationObserver> observers;
	private List<ISystemConfigurationSanitizor> sanitizors;
	private List<ISystemConfigurationValidator> validators;

	private SystemConfigurationHooks() {
		RunnableExtensionLoader<ISystemConfigurationObserver> observerLoader = new RunnableExtensionLoader<>();
		this.observers = observerLoader.loadExtensionsWithPriority("net.bluemind.system", "hook", "observer", "class");

		RunnableExtensionLoader<ISystemConfigurationSanitizor> sanitizorLoader = new RunnableExtensionLoader<>();
		this.sanitizors = sanitizorLoader.loadExtensionsWithPriority("net.bluemind.system", "hook", "sanitizor",
				"class");

		RunnableExtensionLoader<ISystemConfigurationValidator> validatorLoader = new RunnableExtensionLoader<>();
		this.validators = validatorLoader.loadExtensionsWithPriority("net.bluemind.system", "hook", "validator",
				"class");

	}

	public void sanitize(SystemConf previous, Map<String, String> values) throws ServerFault {
		for (ISystemConfigurationSanitizor sanitizor : sanitizors) {
			sanitizor.sanitize(previous, values);
		}
	}

	public void validate(SystemConf previous, Map<String, String> values) throws ServerFault {
		for (ISystemConfigurationValidator validator : validators) {
			validator.validate(previous, values);
		}
	}

	public void fireUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		Exception lastFault = null;
		for (ISystemConfigurationObserver hook : observers) {
			try {
				hook.onUpdated(context, previous, conf);
			} catch (Exception e) {
				logger.error("hook {} onUpdated failed: {}", hook, e.getMessage(), e);
				lastFault = e;
			}
		}
		if (lastFault != null) {
			if (lastFault instanceof ServerFault serverfault) {
				throw serverfault;
			} else {
				throw new ServerFault(lastFault);
			}
		}
	}
}
