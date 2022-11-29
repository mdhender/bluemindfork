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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.driver.mailapi;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class DriverConfig {

	private static final Logger logger = LoggerFactory.getLogger(DriverConfig.class);
	private static final Config INSTANCE = loadConfig();

	public static final String USER_VIRTUAL_ROOT = "driver.virtual-root.user";
	public static final String SHARED_VIRTUAL_ROOT = "driver.virtual-root.shared";

	private DriverConfig() {

	}

	private static Config loadConfig() {
		Config conf = ConfigFactory.load(DriverConfig.class.getClassLoader(), "resources/application.conf");
		try {
			File local = new File("/etc/bm/imap-driver.conf"); // NOSONAR
			if (local.exists()) {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return conf;
	}

	public static Config get() {
		return INSTANCE;
	}

}
