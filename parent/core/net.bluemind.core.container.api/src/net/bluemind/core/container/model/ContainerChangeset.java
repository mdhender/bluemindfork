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
package net.bluemind.core.container.model;

import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * Set of changes for a container since a version
 *
 */
@BMApi(version = "3")
public class ContainerChangeset<T> {
	/**
	 * created items uid
	 */
	public List<T> created;

	/**
	 * updated items uid
	 */
	public List<T> updated;

	/**
	 * deleted items uid
	 */
	public List<T> deleted;

	/**
	 * changeset max version
	 */
	public long version;

	public static <T> ContainerChangeset<T> create(List<T> created, List<T> updated, List<T> deleted, long version) {
		ContainerChangeset<T> set = new ContainerChangeset<>();
		set.created = created;
		set.updated = updated;
		set.deleted = deleted;
		set.version = version;
		return set;
	}

	public String toString() {
		return "created: " + created + " updated: " + updated + " deleted: " + deleted + " version: " + version;
	}
}
