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

package net.bluemind.core.auditlogs.client.es;

import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.IAuditLogFactory;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.IItemChangeLogClient;
import net.bluemind.core.auditlogs.client.es.datastreams.DataStreamActivator;

public class ElasticSearchAuditLogFactory implements IAuditLogFactory {

	@Override
	public IAuditLogClient createClient() {
		return new ElasticSearchAuditLogClient();
	}

	@Override
	public IItemChangeLogClient createItemChangelogClient() {
		return new ElasticSearchItemChangeLogClient();
	}

	@Override
	public IAuditLogMgmt createManager() {
		return new DataStreamActivator();
	}

	@Override
	public int priority() {
		return 1;
	}

}
