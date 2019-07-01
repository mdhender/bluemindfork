/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.service.internal;

import java.util.List;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;

public class AccessControlEntryValidator {
	private final String domainUid;

	public AccessControlEntryValidator(String domainUid) {
		this.domainUid = domainUid;
	}

	public void validate(Container container, List<AccessControlEntry> accessControlEntries) {
		if ("mailboxacl".equals(container.type)
				&& accessControlEntries.stream().anyMatch(ace -> domainUid.equals(ace.subject))) {
			throw new ServerFault("Public sharing for user mailbox is forbidden", ErrorCode.FORBIDDEN);
		}
	}

}
