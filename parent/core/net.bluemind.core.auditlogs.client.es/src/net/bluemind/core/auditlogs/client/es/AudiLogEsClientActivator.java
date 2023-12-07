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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */

package net.bluemind.core.auditlogs.client.es;

import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import net.bluemind.core.auditlogs.client.es.config.AuditLogElasticStoreConfig;
import net.bluemind.core.auditlogs.client.es.config.AuditLogElasticStoreConfig.ExternalESConfig;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class AudiLogEsClientActivator {

	private AudiLogEsClientActivator() {

	}

	public static ElasticsearchClient get() {
		ExternalESConfig externalESConfig = AuditLogElasticStoreConfig.getExternalEsConfig();
		if (externalESConfig == null) {
			return ESearchActivator.getClient();
		}

		return ESearchActivator.getClient(List.of(externalESConfig.ip()),
				AuditLogElasticStoreConfig.getAuthenticationMethod());
	}
}
