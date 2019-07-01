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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import javax.sql.DataSource;

import net.bluemind.backend.mail.replica.api.IReplicatedDataExpiration;
import net.bluemind.backend.mail.replica.service.internal.ReplicatedDataExpirationService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ReplicatedDataExpirationServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IReplicatedDataExpiration> {

	@Override
	public Class<IReplicatedDataExpiration> factoryClass() {
		return IReplicatedDataExpiration.class;
	}

	@Override
	public IReplicatedDataExpiration instance(BmContext context, String... params) {

		String serverUid = params[0];
		DataSource ds = context.getMailboxDataSource(serverUid);

		return new ReplicatedDataExpirationService(context, ds, serverUid);
	}

}
