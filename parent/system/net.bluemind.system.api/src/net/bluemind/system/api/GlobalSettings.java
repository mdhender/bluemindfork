/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.api;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobalSettings {
	public Map<String, String> settings;

	public GlobalSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public static GlobalSettings build(Map<String, String> settings) {
		return new GlobalSettings(settings);
	}

	public GlobalSettings update(Map<String, String> updates) {
		Map<String, String> updated = new HashMap<>(settings);
		updated.putAll(updates);

		return new GlobalSettings(updated);
	}

	public GlobalSettings remove(String key) {
		return new GlobalSettings(settings.entrySet().stream().filter(entry -> !entry.getKey().equals(key))
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
	}
}
