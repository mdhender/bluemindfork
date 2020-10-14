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

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.service.internal.DbMessageBodiesService;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class DbMessageBodiesServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IDbMessageBodies> {

	private static final Logger logger = LoggerFactory.getLogger(DbMessageBodiesService.class);

	private IDbMessageBodies getService(BmContext context, CyrusPartition partition) {
		logger.debug("For partition {}...", partition);
		MessageBodyStore bodyStore = new MessageBodyStore(context.getMailboxDataSource(partition.serverUid));
		Supplier<MessageBodyObjectStore> bodyObjectStore = Suppliers
				.memoize(() -> new MessageBodyObjectStore(context, partition));
		return new DbMessageBodiesService(bodyStore, bodyObjectStore);
	}

	@Override
	public IDbMessageBodies instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		CyrusPartition partition = CyrusPartition.forName(params[0]);
		return getService(context, partition);
	}

	@Override
	public Class<IDbMessageBodies> factoryClass() {
		return IDbMessageBodies.class;
	}

}
