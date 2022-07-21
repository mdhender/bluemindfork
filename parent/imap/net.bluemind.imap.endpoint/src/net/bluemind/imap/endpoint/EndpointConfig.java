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
package net.bluemind.imap.endpoint;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class EndpointConfig {

	private static final Logger logger = LoggerFactory.getLogger(EndpointConfig.class);
	private static final Config INSTANCE = loadConfig();

	private EndpointConfig() {

	}

	private static Config loadConfig() {
		Config conf = ConfigFactory.load(EndpointConfig.class.getClassLoader(), "resources/application.conf");
		File local = new File("/etc/bm/imap.conf"); // NOSONAR
		if (local.exists()) {
			Config parsed = ConfigFactory.parseFile(local);
			conf = parsed.withFallback(conf);
		}
		return conf;
	}

	public static Config get() {
		return INSTANCE;
	}

}
