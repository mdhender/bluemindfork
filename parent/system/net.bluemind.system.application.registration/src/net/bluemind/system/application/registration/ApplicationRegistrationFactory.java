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
package net.bluemind.system.application.registration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Verticle;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.system.application.registration.hook.DummyAppStatusInfoHook;
import net.bluemind.system.application.registration.hook.IAppStatusInfoHook;

public class ApplicationRegistrationFactory implements IVerticleFactory, IUniqueVerticleFactory {
	static final Logger logger = LoggerFactory.getLogger(ApplicationRegistrationFactory.class);

	private static IAppStatusInfoHook getHook() {
		RunnableExtensionLoader<IAppStatusInfoHook> loader = new RunnableExtensionLoader<>();
		IAppStatusInfoHook provider = null;
		List<IAppStatusInfoHook> providers = loader.loadExtensions("net.bluemind.system.application.registration",
				"appHook", "hook", "class");
		if (providers.isEmpty()) {
			logger.warn("no hook found for Application Status Infos");
			provider = new DummyAppStatusInfoHook();
		} else {
			if (providers.size() > 1) {

				logger.warn("too many hooks found for Application Status Infos ({})", providers.size());
			}
			provider = providers.get(0);
		}
		return provider;
	}

	@Override
	public boolean isWorker() {
		return true;
	}

	@Override
	public Verticle newInstance() {
		return new ApplicationRegistration(new Store(System.getProperty("net.bluemind.property.product", "unknown")),
				getHook());
	}

}
