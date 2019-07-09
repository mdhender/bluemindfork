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
package net.bluemind.system.service.internal;

import java.util.List;
import java.util.Map;

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
		RunnableExtensionLoader<ISystemConfigurationObserver> observerLoader = new RunnableExtensionLoader<ISystemConfigurationObserver>();
		this.observers = observerLoader.loadExtensions("net.bluemind.system", "hook", "observer", "class");

		RunnableExtensionLoader<ISystemConfigurationSanitizor> sanitizorLoader = new RunnableExtensionLoader<ISystemConfigurationSanitizor>();
		this.sanitizors = sanitizorLoader.loadExtensions("net.bluemind.system", "hook", "sanitizor", "class");

		RunnableExtensionLoader<ISystemConfigurationValidator> validatorLoader = new RunnableExtensionLoader<ISystemConfigurationValidator>();
		this.validators = validatorLoader.loadExtensions("net.bluemind.system", "hook", "validator", "class");

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
		for (ISystemConfigurationObserver hook : observers) {
			hook.onUpdated(context, previous, conf);
		}
	}
}
