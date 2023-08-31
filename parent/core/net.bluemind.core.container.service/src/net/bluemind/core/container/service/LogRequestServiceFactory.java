/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.core.container.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.api.ILogRequestService;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.container.service.internal.LogRequestService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class LogRequestServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<ILogRequestService> {

	@Override
	public Class<ILogRequestService> factoryClass() {
		return ILogRequestService.class;
	}

	@Override
	public ILogRequestService instance(BmContext context, String... params) throws ServerFault {
		if (params.length > 0) {
			throw new ServerFault("wrong number of instance parameters");
		}

		AuditLogLoader auditLogProvider = new AuditLogLoader();
		IAuditLogClient client = auditLogProvider.getClient();
		if (client == null) {
			throw new ServerFault("AuditLog client cannot be found");
		}
		return new LogRequestService(client);
	}

}
