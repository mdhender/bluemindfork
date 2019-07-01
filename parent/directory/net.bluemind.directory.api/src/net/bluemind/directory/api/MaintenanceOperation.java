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
package net.bluemind.directory.api;

import net.bluemind.core.api.BMApi;

/**
 * MaintenanceOperation defines a unique identifier of a cleanup and repair
 * operation applicable to {@link DirEntry}s
 */
@BMApi(version = "3")
public class MaintenanceOperation {

	/**
	 * Unique operation identifier
	 */
	public String identifier;
	/**
	 * Description
	 */
	public String description;

	public static MaintenanceOperation create(String id, String desc) {
		MaintenanceOperation op = new MaintenanceOperation();
		op.identifier = id;
		op.description = desc;
		return op;
	}

	@Override
	public String toString() {
		return "MaintenanceOperation{id: " + identifier + ", desc: " + description + "}";
	}
}
