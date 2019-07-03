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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;

/**
 * load net.bluemind.core.rest.serviceFactory extensions
 *
 */
public class ServerSideServiceFactories {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerSideServiceFactories.class);

	private static final ServerSideServiceFactories INSTANCE = new ServerSideServiceFactories();

	private Map<Class<?>, IServerSideServiceFactory<?>> factories = new HashMap<>();

	private ServerSideServiceFactories() {
		IExtensionRegistry er = RegistryFactory.getRegistry();
		IExtensionPoint point = er.getExtensionPoint("net.bluemind.core.rest", "serviceFactory");
		if (point == null) {
			LOGGER.error("extensionPoint net.bluemind.core.rest.serviceFactory not found");
			return;
		}

		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {

				ServerSideServiceProvider.IServerSideServiceFactory<?> serviceFactory = null;
				LOGGER.debug("try to register service {}", e.getAttribute("class"));
				try {
					serviceFactory = (ServerSideServiceProvider.IServerSideServiceFactory<?>) e
							.createExecutableExtension("class");

					LOGGER.debug("registred service {} for {}", e.getAttribute("class"), serviceFactory.factoryClass());
				} catch (CoreException ce) {
					LOGGER.error("error during loading extension " + ie.getExtensionPointUniqueIdentifier(), ce);
					continue;
				}

				factories.put(serviceFactory.factoryClass(), serviceFactory);
			}
		}
	}

	public static ServerSideServiceFactories getInstance() {
		return INSTANCE;
	}

	public Map<Class<?>, IServerSideServiceFactory<?>> getFactories() {
		return factories;
	}
}
