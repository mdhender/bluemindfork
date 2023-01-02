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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.user.persistence.security;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class HashConfig {
	private static final Config INSTANCE = loadConfig();
	private static final String CONFIG_PATH = "/etc/bm/bluemind.conf";

	private HashConfig() {

	}

	private static Config loadConfig() {
		Config referenceConfig = ConfigFactory.parseResourcesAnySyntax(HashConfig.class.getClassLoader(),
				"resources/reference.conf");
		Config resolvedConfig = referenceConfig;
		File local = new File(CONFIG_PATH);
		if (local.exists()) {
			resolvedConfig = ConfigFactory.parseFile(local);
			resolvedConfig = resolvedConfig.withFallback(referenceConfig).resolve();
		}
		return resolvedConfig.withOnlyPath("bm.security.hash");
	}

	public static Config get() {
		return INSTANCE;
	}
}