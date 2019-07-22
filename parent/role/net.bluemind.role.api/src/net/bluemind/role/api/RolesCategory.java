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
package net.bluemind.role.api;

import net.bluemind.core.api.BMApi;

/**
 * Role category declaration.
 *
 */
@BMApi(version = "3")
public class RolesCategory {

	/**
	 * Unique identifier.
	 */
	public String id;
	/**
	 * Category label.
	 */
	public String label;
	/**
	 * Priority (order).
	 */
	public int priority;

	public static RolesCategory create(String id, String label) {
		return create(id, label, 0);
	}

	public static RolesCategory create(String id, String label, int priority) {
		RolesCategory ret = new RolesCategory();
		ret.id = id;
		ret.label = label;
		ret.priority = priority;
		return ret;
	}
}
