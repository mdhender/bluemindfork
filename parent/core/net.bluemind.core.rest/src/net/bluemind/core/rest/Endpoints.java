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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.internal.ConfigurationToEndpoint;
import net.bluemind.core.rest.model.Endpoint;
import net.bluemind.core.rest.model.RestService;
import net.bluemind.core.rest.model.RestServiceApiDescriptor;

public class Endpoints {

	private static final Logger logger = LoggerFactory.getLogger(Endpoints.class);

	private static List<RestService> endpoints;

	public static List<RestService> getEndpoints() {
		if (endpoints == null) {
			endpoints = loadEndpoints();
		}

		return endpoints;
	}

	private static List<RestService> loadEndpoints() {
		IExtensionRegistry er = RegistryFactory.getRegistry();
		IExtensionPoint point = er.getExtensionPoint("net.bluemind.core.rest", "apiEndpoint");
		if (point == null) {
			logger.error("extensionPoint net.bluemind.core.rest.apiEndpoint not found");
			return Collections.emptyList();
		}

		IExtension[] extensions = point.getExtensions();
		List<RestService> restServices = new ArrayList<>();
		for (IExtension extension : extensions) {
			for (IConfigurationElement configElement : extension.getConfigurationElements()) {
				try {
					RestService restService = createRestService(extension, configElement);
					restServices.add(restService);
				} catch (RuntimeException e) {
					logger.error("error while creating REST-Service from extension point {}: {}",
							extension.getExtensionPointUniqueIdentifier(), e);
				}
			}
		}

		return restServices;
	}

	private static RestService createRestService(IExtension extension, IConfigurationElement configElement) {
		Endpoint ep = ConfigurationToEndpoint.getInstance(extension, configElement).get();
		RestServiceApiDescriptor descriptor = new RestServiceApiDescriptorBuilder().build(ep.getInterface());

		RestService restService = new RestService(descriptor, ep);
		if (logger.isDebugEnabled()) {
			logger.debug("available endpoint {}@{}", restService.descriptor.getApiInterfaceName(),
					restService.descriptor.rootPath);
		}

		return restService;

	}
}
