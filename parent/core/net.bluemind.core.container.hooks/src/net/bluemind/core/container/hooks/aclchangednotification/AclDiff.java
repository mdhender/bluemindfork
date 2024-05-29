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

import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;

public record AclDiff(String subject, Verb oldVerb, Verb newVerb, AclStatus status) {

	@Override
	public String toString() {
		return "AclDiff [ subject = " + subject + ", oldVerb = " + (oldVerb != null ? oldVerb.name() : "None")
				+ ", new = " + (newVerb != null ? newVerb.name() : "None") + ", status= " + status.name() + "]";
	}

	public static AclDiff createAclDiffForUpdate(AccessControlEntry oldAcl, AccessControlEntry newAcl) {
		return new AclDiff(oldAcl.subject, oldAcl.verb, newAcl.verb, AclStatus.UPDATED);
	}

	public static AclDiff createAclDiff(AccessControlEntry aws, AclStatus status) {
		switch (status) {
		case ADDED: {
			return new AclDiff(aws.subject, null, aws.verb, AclStatus.ADDED);
		}
		case REMOVED: {
			return new AclDiff(aws.subject, aws.verb, null, AclStatus.REMOVED);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + status);
		}
	}

	public enum AclStatus {
		ADDED, REMOVED, UPDATED;
	}
}
