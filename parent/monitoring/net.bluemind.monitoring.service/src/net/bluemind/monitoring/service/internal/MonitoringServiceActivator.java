/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.service.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.monitoring.service.IServiceInfoProvider;

public class MonitoringServiceActivator implements BundleActivator {

	private static BundleContext context;
	private static Map<String, ServiceInfoProvider> providers;
	private static final Logger logger = LoggerFactory.getLogger(MonitoringServiceActivator.class);

	public static Optional<ServiceInfoProvider> getProvider(String service) {
		return Optional.ofNullable(providers.get(service));
	}

	public static Map<String, ServiceInfoProvider> getProviders() {
		return Collections.unmodifiableMap(providers);
	}

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		MonitoringServiceActivator.context = context;
		loadProviders();
	}

	private void loadProviders() throws CoreException {
		providers = new HashMap<>();
		logger.debug("loading monitoring providers");
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.monitoring",
				"provider");

		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("provider")) {
					String serviceName = e.getAttribute("service-name");
					IServiceInfoProvider impl = (IServiceInfoProvider) e.createExecutableExtension("impl");
					IConfigurationElement[] children = e.getChildren("tags");
					List<String> tags = new ArrayList<>();
					if (null != children) {
						for (IConfigurationElement tag : children) {
							tags.add(tag.getAttribute("tag"));
						}
					}
					ServiceInfoProvider provider = new ServiceInfoProvider(impl, serviceName, tags);
					MonitoringServiceActivator.providers.put(serviceName, provider);
				}
			}

		}

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		MonitoringServiceActivator.context = null;
	}

}
