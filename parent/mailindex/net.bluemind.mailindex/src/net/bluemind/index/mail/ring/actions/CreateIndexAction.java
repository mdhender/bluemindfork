/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.index.mail.ring.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class CreateIndexAction implements IndexAction {

	private final String indexName;

	public CreateIndexAction(String indexName) {
		this.indexName = indexName;
	}

	@Override
	public void execute(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		esClient.indices().create(c -> c.index(indexName)
				.withJson(new ByteArrayInputStream(ESearchActivator.getIndexSchema("mailspool"))));
		esClient.cluster().health(h -> h.index(indexName).waitForStatus(HealthStatus.Green));

	}

	@Override
	public String info() {
		return "Creating index " + indexName;
	}

}
