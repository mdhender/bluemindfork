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

package net.bluemind.core.container.service.internal;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.AuditLogQuery;
import net.bluemind.core.auditlogs.IItemChangeLogClient;
import net.bluemind.core.auditlogs.api.ILogRequestService;

public class LogRequestService implements ILogRequestService {

	private final IItemChangeLogClient client;

	public LogRequestService(IItemChangeLogClient client) {
		this.client = client;
	}

	@Override
	public List<AuditLogEntry> queryAuditLog(AuditLogQuery query) throws ServerFault {
		return client.queryAuditLog(query);
	}

}
