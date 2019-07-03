/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.node.shared;

import java.util.Objects;

public class ActiveExecQuery {

	public final String group;
	public final String name;

	private ActiveExecQuery(String group, String name) {
		this.group = group;
		this.name = name;
	}

	public static ActiveExecQuery byGroup(String group) {
		Objects.requireNonNull("group must not be null", group);
		return new ActiveExecQuery(group, null);
	}

	public static ActiveExecQuery byName(String group, String name) {
		Objects.requireNonNull("group must not be null", group);
		Objects.requireNonNull("name must not be null", name);
		return new ActiveExecQuery(group, name);
	}

	public static ActiveExecQuery all() {
		return new ActiveExecQuery(null, null);
	}

}
