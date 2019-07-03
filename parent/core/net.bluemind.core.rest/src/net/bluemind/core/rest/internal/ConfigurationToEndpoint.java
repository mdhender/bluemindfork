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
package net.bluemind.core.rest.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.model.Endpoint;

public abstract class ConfigurationToEndpoint {
	protected static final Logger logger = LoggerFactory.getLogger(ConfigurationToEndpoint.class);
	protected final IConfigurationElement configElement;
	protected final IExtension extension;

	protected ConfigurationToEndpoint(IConfigurationElement configElement, IExtension extension) {
		this.configElement = configElement;
		this.extension = extension;
	}

	public abstract Endpoint get();

	public static ConfigurationToEndpoint getInstance(IExtension extension, IConfigurationElement configElement) {
		if (configElement.getAttribute("api") != null) {
			if (configElement.getAttribute("inline") != null
					&& Boolean.parseBoolean(configElement.getAttribute("inline"))) {
				return new InlinedApiToEndpoint(configElement, extension);
			} else {
				return new ApiToEndpoint(configElement, extension);
			}
		}
		throw new IllegalArgumentException();
	}

	public static class ApiToEndpoint extends ConfigurationToEndpoint {

		protected ApiToEndpoint(IConfigurationElement configElement, IExtension extension) {
			super(configElement, extension);
		}

		@Override
		public Endpoint get() {
			String className = configElement.getAttribute("api");
			try {
				Class<?> apiClass = Platform.getBundle(extension.getContributor().getName()).loadClass(className);
				return new ApiClassEndpoint(apiClass);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

		}

	}

	public static class InlinedApiToEndpoint extends ConfigurationToEndpoint {

		protected InlinedApiToEndpoint(IConfigurationElement configElement, IExtension extension) {
			super(configElement, extension);
		}

		@Override
		public Endpoint get() {
			String className = configElement.getAttribute("api");
			try {
				Class<?> apiClass = Platform.getBundle(extension.getContributor().getName()).loadClass(className);
				return new InlinedApiClassEndpoint(apiClass);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

		}

	}

}
