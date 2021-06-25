/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.backend.mail.replica.service.names;

import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.ContainerStoreService;

public class NameSanitizers {

	private NameSanitizers() {
	}

	public static INameSanitizer create(MailboxReplicaRootDescriptor root, MailboxReplicaStore store,
			ContainerStoreService<MailboxReplica> contStore) {
		switch (root.ns) {
		case shared:
			return new SharedMailboxNameSanitizer(root, store, contStore);
		case users:
			return new UserMailboxNameSanitizer(root, store, contStore);
		case deleted:
		case deletedShared:
		default:
			throw new ServerFault("We can't deal with namespace " + root.ns + "right now");
		}
	}

}
