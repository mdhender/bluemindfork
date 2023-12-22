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
package net.bluemind.index.mail.ring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.index.mail.ring.actions.IndexAction;

public class AliasRingOperations {

	private static final Logger logger = LoggerFactory.getLogger(AliasRingOperations.class);

	private final List<IndexAction> actions;
	private final String operation;
	private final ElasticsearchClient client;

	private AliasRingOperations(ElasticsearchClient client, String operation, List<IndexAction> actions) {
		this.client = client;
		this.operation = operation;
		this.actions = actions;
	}

	private void execute() throws ElasticsearchException, IOException {
		logger.info("Executing index operation: {}", operation);
		for (int i = 0; i < actions.size(); i++) {
			IndexAction action = actions.get(i);
			logger.info("[{}/{}] Executing index action: {}", i + 1, actions.size(), action.info());
			action.execute(client);
		}
	}

	public static class Builder {
		private final List<IndexAction> actions = new ArrayList<>();
		private final String operation;
		private final ElasticsearchClient client;

		Builder(ElasticsearchClient client, String operation) {
			this.client = client;
			this.operation = operation;
		}

		Builder action(IndexAction action) {
			actions.add(action);
			return this;
		}

		void execute() throws ElasticsearchException, IOException {
			new AliasRingOperations(client, operation, actions).execute();
		}

	}

}
