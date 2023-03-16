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

package net.bluemind.core.auditlogs.client.loader;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.IAuditLogClientFactory;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class AuditLogClientLoader {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogClientLoader.class);

	private static final IAuditLogClientFactory auditLogClient = loadAuditLogClient();

	private static IAuditLogClientFactory loadAuditLogClient() {
		RunnableExtensionLoader<IAuditLogClientFactory> rel = new RunnableExtensionLoader<>();
		List<IAuditLogClientFactory> plugins = rel.loadExtensions("net.bluemind.core", "auditlogs", "store", "factory");
		if (plugins != null && plugins.size() == 1) {
			return plugins.get(0);
		}
		logger.warn("Cannot find plugin 'net.bluemind.core.auditlogs', load NoopAuditLogClient");
		return () -> NoopAuditLogClient.INSTANCE;
	}

	public IAuditLogClient get() {
		return auditLogClient.load();
	}
}
