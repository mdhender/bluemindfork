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

import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.client.loader.AuditLogClientLoader;
import net.bluemind.core.container.api.internal.IChangeLogService;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemChangelog;

public class ChangeLogService implements IChangeLogService {

	private AuditLogClientLoader auditLogClientProvider = new AuditLogClientLoader();

	private BaseContainerDescriptor container;
	private IAuditLogClient auditLogClient;

	public ChangeLogService(BaseContainerDescriptor container) {
		this.container = container;
		auditLogClient = auditLogClientProvider.get();
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) {
		return auditLogClient.getItemChangeLog(container.uid, itemUid, since);
	}
}
