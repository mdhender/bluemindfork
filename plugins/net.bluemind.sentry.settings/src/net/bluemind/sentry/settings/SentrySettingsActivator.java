/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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

package net.bluemind.sentry.settings;

import java.io.IOException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sentry.Sentry;

public class SentrySettingsActivator implements BundleActivator {
	private static final Logger logger = LoggerFactory.getLogger(SentrySettingsActivator.class);

	public void start(BundleContext bundleContext) throws Exception {
		logger.info("Sentry settings activator launched");
		try {
			SentryProperties.checkOrCreateFolders();
		} catch (IOException ioe) {
			logger.error("Unable to setup sentry folders", ioe);
		}

		System.setProperty("sentry.properties.file",
				SentryProperties.getConfigurationPath().toAbsolutePath().toString());
		System.setProperty("in-app-includes", "net.bluemind");
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Sentry.close();
	}
}