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

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSchemaInitializer implements ISchemaInitializer {

	private static Logger logger = LoggerFactory.getLogger(AbstractSchemaInitializer.class);

	@Override
	public void initializeSchema(Client esearchClient) {
		String name = getIndexName();
		IndicesExistsResponse resp = esearchClient.admin().indices().prepareExists(name).execute().actionGet();

		// create index if not already exist
		if (!resp.isExists()) {

			String alias = getIndexName();
			String index = getIndexName() + "-initial";
			logger.info("creating index {} alias to {} ", index, alias);

			// first we create the real index
			esearchClient.admin().indices().prepareCreate(index).addMapping(getType(), getSchemaAsString()).execute()
					.actionGet();

			// we create the index alias
			esearchClient.admin().indices().prepareAliases().addAlias(index, alias).execute().actionGet();

		} else {
			logger.info("schema for index {} already exist ", getIndexName());
		}
	}

	public abstract String getSchemaAsString();

	public abstract String getType();

	abstract protected String getIndexName();

}
