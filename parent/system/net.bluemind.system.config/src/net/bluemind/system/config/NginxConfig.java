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
package net.bluemind.system.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NginxConfig {

	private static Logger logger = LoggerFactory.getLogger(NginxConfig.class);

	public NginxConfig(Map<String, String> values) {
		this.values = values;
		for (String key : values.keySet()) {
			logger.info("Setting NGINX config {}:{}", key, values.get(key));
		}
	}

	public String get(String key) {
		return values.get(key);
	}

	private final Map<String, String> values;

	public static class NginxConfigBuilder {
		private Map<String, String> values;

		private NginxConfigBuilder() {
			this.values = new HashMap<>();
		}

		public static NginxConfigBuilder init(String key, String value) {
			NginxConfigBuilder builder = new NginxConfigBuilder();
			builder.values.put(key, value);
			return builder;
		}

		public NginxConfigBuilder add(String key, String value) {
			values.put(key, value);
			return this;
		}

		public NginxConfig build() {
			return new NginxConfig(values);
		}
	}
}
