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
package net.bluemind.core.api;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonsActivator implements BundleActivator {

	private static Logger logger = LoggerFactory.getLogger(CommonsActivator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		BMVersion.version = context.getBundle().getHeaders().get("Bundle-Version");
		BMVersion.versionName = context.getBundle().getHeaders().get("X-BM-Version");
		logger.info("CommonsActivator started version {} name {}", BMVersion.version, BMVersion.versionName);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		logger.info("CommonsActivator stopped");
	}

	public static String getVersionName() {
		return BMVersion.versionName;
	}

	public static String getVersion() {
		return BMVersion.version;
	}
}
