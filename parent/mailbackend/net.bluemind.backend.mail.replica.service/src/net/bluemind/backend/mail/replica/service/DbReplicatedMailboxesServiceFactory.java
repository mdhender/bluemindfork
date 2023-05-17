/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.internal.DbReplicatedMailboxesService;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;

public class DbReplicatedMailboxesServiceFactory
		extends AbstractReplicatedMailboxesServiceFactory<IDbReplicatedMailboxes> {

	public DbReplicatedMailboxesServiceFactory() {
	}

	@Override
	public Class<IDbReplicatedMailboxes> factoryClass() {
		return IDbReplicatedMailboxes.class;
	}

	@Override
	protected IDbReplicatedMailboxes create(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore mboxReplicaStore, ContainerStoreService<MailboxReplica> storeService,
			ContainerStore containerStore) {
		return new DbReplicatedMailboxesService(root, cont, context, mboxReplicaStore, storeService, containerStore);
	}

}
