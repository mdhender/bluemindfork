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
package net.bluemind.core.auditlogs.client.es.datastreams;

import java.io.ByteArrayInputStream;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.auditlogs.client.es.AudiLogEsClientActivator;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogStoreConfig;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.retry.support.RetryQueueVerticle;
import net.bluemind.retry.support.RetryQueueVerticle.RetryProcessor;

public class AuditQueueFactory implements IVerticleFactory, IUniqueVerticleFactory {
	private static final Logger logger = LoggerFactory.getLogger(AuditQueueFactory.class);

	private static class RetryIndexing implements RetryProcessor {

		private Supplier<ElasticsearchClient> esClientHandler;

		public RetryIndexing(Supplier<ElasticsearchClient> handler) {
			esClientHandler = handler;
		}

		@Override
		public void retry(JsonObject js) throws Exception {
			ElasticsearchClient esClient = esClientHandler.get();
			if (!js.containsKey("domainUid")) {
				return;
			}
			if (esClient == null) {
				return;
			}
			String domainUid = js.getString("domainUid");
			byte[] jsBytes = js.encode().getBytes();
			if (domainUid != null) {
				String dataStreamName = AuditLogStoreConfig.resolveDataStreamName(domainUid);
				try {
					esClient.index(i -> i.index(dataStreamName).withJson(new ByteArrayInputStream(jsBytes)));
				} catch (ElasticsearchException e) {
					if (e.error() != null && "index_not_found_exception".equals(e.error().type())) {
						logger.warn("datastream '{}' not found", dataStreamName);
						return;
					}
					throw new Exception(e);
				}
			}
		}
	}

	public static class AuditQueue extends RetryQueueVerticle {

		protected AuditQueue(RetryProcessor rp) {
			super("audit", rp);
		}

	}

	private final AuditQueue auditQueue;

	public AuditQueueFactory() {
		RetryIndexing ri = new RetryIndexing(AudiLogEsClientActivator::get);
		this.auditQueue = new AuditQueue(ri);
	}

	@Override
	public boolean isWorker() {
		return true;
	}

	@Override
	public Verticle newInstance() {
		return auditQueue;
	}

}
