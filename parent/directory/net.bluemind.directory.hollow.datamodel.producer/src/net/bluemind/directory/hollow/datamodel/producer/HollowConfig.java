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
package net.bluemind.directory.hollow.datamodel.producer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class HollowConfig {

	private static final Logger logger = LoggerFactory.getLogger(HollowConfig.class);
	private static final Config INSTANCE = loadConfig();

	private HollowConfig() {

	}

	private static Config loadConfig() {
		Config conf = ConfigFactory.load(HollowConfig.class.getClassLoader(), "resources/hollow.conf");
		try {
			File local = new File("/etc/bm/hollow.conf"); // NOSONAR
			if (local.exists()) {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		Config systemPropertyConfig = ConfigFactory.defaultApplication();
		return systemPropertyConfig.withFallback(conf);
	}

	public static Config get() {
		return INSTANCE;
	}

}
