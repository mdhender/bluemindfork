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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.restore;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class SysconfOverride {

	private Map<String, String> overrides;

	public SysconfOverride(Map<String, String> over) {
		this.overrides = ImmutableMap.copyOf(over);
	}

	public Map<String, String> getOverrides() {
		return overrides;
	}

}
