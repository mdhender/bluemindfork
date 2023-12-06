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
import net.bluemind.core.auditlogs.IAuditLogFactory;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.IItemChangeLogClient;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.network.topology.Topology;

public class AuditLogLoader {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogLoader.class);

	private static final IAuditLogFactory auditLog = loadAuditLog();

	private static IAuditLogFactory loadAuditLog() {
		RunnableExtensionLoader<IAuditLogFactory> rel = new RunnableExtensionLoader<>();
		List<IAuditLogFactory> plugins = rel.loadExtensions("net.bluemind.core", "auditlogs", "store", "factory");

		if (!AuditLogConfig.isActivated()) {
			logger.warn("Audit log has been deactivated: no audit log data will be stored");
			return noopAuditLogFactory();
		}
		if (plugins != null && plugins.size() == 1) {
			return plugins.get(0);
		}
		logger.warn("Cannot find plugin 'net.bluemind.core.auditlogs', load NoopAuditLogClient");
		return noopAuditLogFactory();
	}

	private static IAuditLogFactory noopAuditLogFactory() {
		return new IAuditLogFactory() {

			@Override
			public IAuditLogClient createClient() {
				return NoopAuditLogClient.INSTANCE;
			}

			@Override
			public IAuditLogMgmt createManager() {
				return NoopAuditLogManager.INSTANCE;
			}

			@Override
			public IItemChangeLogClient createItemChangelogClient() {
				return NoopItemChangeLogClient.INSTANCE;
			}

		};
	}

	public IAuditLogClient getClient() {
		return Topology.getIfAvailable().map(t -> auditLog.createClient()).orElse(NoopAuditLogClient.INSTANCE);
	}

	public IAuditLogMgmt getManager() {
		return Topology.getIfAvailable().map(t -> auditLog.createManager()).orElse(NoopAuditLogManager.INSTANCE);
	}

	public IItemChangeLogClient getItemChangelogClient() {
		return Topology.getIfAvailable().map(t -> auditLog.createItemChangelogClient())
				.orElse(NoopItemChangeLogClient.INSTANCE);
	}

}
