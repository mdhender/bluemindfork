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

import com.google.common.base.MoreObjects;

public class ExecDescriptor {

	public String group;
	public String name;
	public String command;
	public String taskRefId;

	public static ExecDescriptor forTask(String taskref) {
		ExecDescriptor ed = new ExecDescriptor();
		ed.taskRefId = taskref;
		return ed;
	}

	public String toString() {
		return MoreObjects.toStringHelper(ExecDescriptor.class).add("group", group).add("name", name)
				.add("cmd", command).add("pid", taskRefId).toString();
	}

}
