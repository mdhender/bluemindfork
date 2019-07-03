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
import net.bluemind.directory.api.DirBaseValue;

@BMApi(version = "3")
public final class Group extends DirBaseValue {

	public String name;
	public String description;

	public Map<String, String> properties = new HashMap<>();

	public boolean hiddenMembers;

	/**
	 * Does mail send to group must be store in linked mailshare
	 */
	public boolean mailArchived;

	public Integer memberCount;

	@Override
	public String toString() {
		return "Group [name=" + name + ", hidden=" + hidden + ", hiddenMembers=" + hiddenMembers + ", mailArchived="
				+ mailArchived + ", dataLocation=" + dataLocation + "]";
	}

	public boolean profile() {
		return "true".equals(properties.get("is_profile"));
	}
}
