/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.core.container.hooks.aclchangednotification;

import net.bluemind.core.container.hooks.aclchangednotification.AclWithStatus.AclStatus;
import net.bluemind.core.container.model.acl.AccessControlEntry;

public record AclWithStatus(AccessControlEntry entry, AclStatus status) {

	public enum AclStatus {
		ADDED, REMOVED;
	}

	@Override
	public String toString() {
		return "AclWithStatus [ entry = " + entry.toString() + ", status = " + status + "]";
	}
}
