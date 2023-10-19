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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.retry.support.RetryRequester;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class ElasticSearchAuditLogClient implements IAuditLogClient {
	private static final Logger logger = LoggerFactory.getLogger(ElasticSearchAuditLogClient.class);

	private RetryRequester requester;

	public ElasticSearchAuditLogClient() {
		this.requester = new RetryRequester(VertxPlatform.eventBus(), "audit");
	}

	@Override
	public void storeAuditLog(AuditLogEntry document) {
		if (StateContext.getState() != SystemState.CORE_STATE_RUNNING) {
			return;
		}
		try {
			JsonObject js = JsonObject.mapFrom(document);
			requester.request(js);
		} catch (TopologyException e) {
			logger.warn("ElasticClient is not available: {}", e.getMessage());
		}

	}

}
