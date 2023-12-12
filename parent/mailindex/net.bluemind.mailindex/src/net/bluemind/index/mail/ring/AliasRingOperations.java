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
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.index.mail.ring.AliasRing.RingIndex;
import net.bluemind.index.mail.ring.actions.CopyDocumentsAction;
import net.bluemind.index.mail.ring.actions.CreateIndexAction;
import net.bluemind.index.mail.ring.actions.DeleteIndexAction;
import net.bluemind.index.mail.ring.actions.IndexAction;
import net.bluemind.index.mail.ring.actions.MoveAliasAction;
import net.bluemind.lib.elasticsearch.IndexAliasCreator.RingIndexAliasCreator;

public class AliasRingOperations {

	private final ElasticsearchClient esClient;
	private final MailIndexService service;

	public AliasRingOperations(ElasticsearchClient esClient, MailIndexService service) {
		this.esClient = esClient;
		this.service = service;
	}

	public void createIndex(String indexName) throws ElasticsearchException, IOException {
		new Executioner.Builder(esClient, "Adding index " + indexName) //
				.action(new CreateIndexAction(indexName)) //
				.execute();
	}

	public void deleteIndex(String indexName) throws ElasticsearchException, IOException {
		new Executioner.Builder(esClient, "Deleting index " + indexName) //
				.action(new DeleteIndexAction(indexName)) //
				.execute();
	}

	public void rebalance(RingIndex sourceIndex, int targetPosition) throws ElasticsearchException, IOException {
		var targetIndex = RingIndexAliasCreator.getIndexRingName("mailspool", targetPosition);

		var concernedReadAliases = new TreeSet<>(
				sourceIndex.readAliases().stream().filter(alias -> alias.position() <= targetPosition).toList());
		var concerncedwriteAliases = new TreeSet<>(
				sourceIndex.writeAliases().stream().filter(alias -> alias.position() <= targetPosition).toList());

		new Executioner.Builder(esClient,
				String.format("Rebalancing source index %s with target index %s", sourceIndex.name(), targetIndex)) //
				.action(new MoveAliasAction(sourceIndex, concernedReadAliases, targetIndex)) //
				.action(new CopyDocumentsAction(service, sourceIndex, concernedReadAliases, targetIndex)) //
				.action(new MoveAliasAction(sourceIndex, concerncedwriteAliases, targetIndex)) //
				.execute();
	}

	private static class Executioner {

		private static final Logger logger = LoggerFactory.getLogger(Executioner.class);

		private final List<IndexAction> actions;
		private final String operation;
		private final ElasticsearchClient client;

		public Executioner(ElasticsearchClient client, String operation, List<IndexAction> actions) {
			this.client = client;
			this.operation = operation;
			this.actions = actions;
		}

		public void execute() throws ElasticsearchException, IOException {
			logger.info("Executing index operation: {}", operation);
			for (int i = 0; i < actions.size(); i++) {
				IndexAction action = actions.get(i);
				logger.info("[{}/{}] Executing index action: {}", i + 1, actions.size(), action.info());
				action.execute(client);
			}
		}

		private static class Builder {
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
				new Executioner(client, operation, actions).execute();
			}

		}

	}

}
