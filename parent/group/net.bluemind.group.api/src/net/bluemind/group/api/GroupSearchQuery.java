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
package net.bluemind.group.api;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class GroupSearchQuery {

	public String name;
	public Map<String, String> properties = new HashMap<String, String>();

	public boolean matches(Group group) {

		if (null != name && name.trim().length() > 0) {
			if (!group.name.toUpperCase().contains(name.toUpperCase())) {
				return false;
			}
		}

		Map<String, String> groupProperties = group.properties;
		for (String key : properties.keySet()) {
			if (!groupProperties.containsKey(key) || !groupProperties.get(key).equals(properties.get(key))) {
				return false;
			}
		}

		return true;

	}

	public static GroupSearchQuery matchProperty(String key, String value) {
		GroupSearchQuery ret = new GroupSearchQuery();
		ret.properties.put(key, value);
		return ret;
	}

}
