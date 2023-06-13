/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.elasticsearch.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public abstract class AbstractSchemaInitializer implements ISchemaInitializer {

	private static Logger logger = LoggerFactory.getLogger(AbstractSchemaInitializer.class);

	@Override
	public void initializeSchema(ElasticsearchClient esClient) {
		String name = getIndexName();
		try {
			boolean exists = esClient.indices().exists(e -> e.index(name)).value();
			// init index if not already exist
			if (!exists) {
				ESearchActivator.initIndex(esClient, name);
			}
		} catch (Exception e) {
			logger.error("[es][server hook] Failed to initialize index {}", name, e);
		}
	}

	public abstract String getSchemaAsString();

	public abstract String getType();

	protected abstract String getIndexName();

}
