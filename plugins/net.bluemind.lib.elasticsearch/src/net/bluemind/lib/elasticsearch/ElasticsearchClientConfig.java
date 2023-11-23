/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.lib.elasticsearch;

import static net.bluemind.configfile.elastic.ElasticsearchConfig.OVERRIDE_PATH;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import net.bluemind.configfile.ConfigChangeListener;
import net.bluemind.configfile.ReloadableConfig;
import net.bluemind.configfile.elastic.ElasticsearchConfig;
import net.bluemind.lib.vertx.VertxPlatform;

public class ElasticsearchClientConfig {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClientConfig.class);
	private static ReloadableConfig instance = load();

	private ElasticsearchClientConfig() {
	}

	private static ReloadableConfig load() {
		return new ReloadableConfig(VertxPlatform.getVertx(), ElasticsearchClientConfig::setupConfig);
	}

	private static Config setupConfig() {
		Config conf = ConfigFactory.load(ElasticsearchClientConfig.class.getClassLoader(),
				"resources/application.conf");
		File local = new File(OVERRIDE_PATH);
		if (local.exists()) {
			try {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			} catch (ConfigException e) {
				logger.error("Invalid Elasticsearch config file in '{}', ignored: {}", OVERRIDE_PATH, e.getMessage());
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

	public static int getMaxAliasMultiplier() {
		return Integer
				.highestOneBit(get().getInt(ElasticsearchConfig.Indexation.ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLICATOR));
	}
}
