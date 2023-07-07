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

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import net.bluemind.configfile.ConfigChangeListener;
import net.bluemind.configfile.ReloadableConfig;
import net.bluemind.configfile.imap.ImapConfig;
import net.bluemind.lib.vertx.VertxPlatform;

public class EndpointConfig {

	private static final Logger logger = LoggerFactory.getLogger(EndpointConfig.class);
	private static ReloadableConfig instance = load();

	private EndpointConfig() {
	}

	private static ReloadableConfig load() {
		return new ReloadableConfig(VertxPlatform.getVertx(), EndpointConfig::setupConfig);
	}

	private static Config setupConfig() {
		Config conf = ConfigFactory.load(EndpointConfig.class.getClassLoader(), "resources/application.conf");
		File local = new File(ImapConfig.OVERRIDE_PATH); // NOSONAR
		if (local.exists()) {
			try {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			} catch (ConfigException e) {
				logger.error("Invalid IMAP config file in '/etc/bm/imap.conf', ignored: {}", e.getMessage());
			}
		}
		return conf;
	}

	public static void addListener(ConfigChangeListener listener) {
		instance.addListener(listener);
	}

	public static Config get() {
		return instance.config();
	}

	@VisibleForTesting
	public static void reload() {
		ConfigFactory.invalidateCaches();
		instance = load();
	}

}
