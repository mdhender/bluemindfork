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

package net.bluemind.core.auditlogs.client.es.datastreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;

import co.elastic.clients.ApiClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.transport.ElasticsearchTransport;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.exception.ElasticIndexException;

public class DataStreamActivator implements BundleActivator, IAuditLogMgmt {

	private static final Map<String, IndexTemplateDefinition> indexTemplates = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger(DataStreamActivator.class);

	@Override
	public void start(BundleContext arg0) throws Exception {

		IExtensionPoint ep = Platform.getExtensionRegistry()
				.getExtensionPoint("net.bluemind.core.auditlogs.client.es.datastreams", "indextemplate");
		for (IExtension ext : ep.getExtensions()) {
			for (IConfigurationElement ce : ext.getConfigurationElements()) {
				String index = ce.getAttribute("name");
				String schema = ce.getAttribute("resource");
				String dataStreamName = ce.getAttribute("datastream_name");
				Bundle bundle = Platform.getBundle(ext.getContributor().getName());
				URL url = bundle.getResource(schema);
				try (InputStream in = url.openStream()) {
					indexTemplates.put(index,
							new IndexTemplateDefinition(index, ByteStreams.toByteArray(in), dataStreamName));
				}
			}
		}
	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		/// Nothing to do

	}

	public static void resetDataStream(String indexName, String dataStreamName) {
		ESearchActivator.waitForElasticsearchHosts();
		ElasticsearchClient esClient = ESearchActivator.getClient();
		deleteDataStream(esClient, dataStreamName);
		deleteIndexTemplate(esClient, indexName);
		initDataStream(esClient, indexName, dataStreamName);
	}

	private static void deleteDataStream(ElasticsearchClient esClient, String dataStreamName) {
		try {
			esClient.indices().deleteDataStream(d -> d.name(Arrays.asList(dataStreamName)));
			logger.info("datastream '{}' deleted.", dataStreamName);
		} catch (ElasticsearchException e) {
			if (e.error() != null && "index_not_found_exception".equals(e.error().type())) {
				logger.warn("dataStream '{}' not found, can't be delete", dataStreamName);
				return;
			}
			throw new ElasticIndexException(dataStreamName, e);
		} catch (IOException e) {
			throw new ElasticIndexException(dataStreamName, e);
		}
	}

	private static void deleteIndexTemplate(ElasticsearchClient esClient, String indexTemplateName) {
		try {
			esClient.indices().deleteIndexTemplate(it -> it.name(Arrays.asList(indexTemplateName)));
			logger.info("index template '{}' deleted.", indexTemplateName);

		} catch (ElasticsearchException e) {
			if (e.error() != null && "index_template_missing_exception".equals(e.error().type())) {
				logger.warn("index template '{}' not found, can't be delete", indexTemplateName);
				return;
			}
			throw new ElasticIndexException(indexTemplateName, e);
		} catch (IOException e) {
			throw new ElasticIndexException(indexTemplateName, e);
		}
	}

	public static void initDataStream(ElasticsearchClient esClient, String indexName, String dataStreamName) {
		indexDefinitionOf(indexName).ifPresentOrElse(definition -> {
			byte[] schema = definition.schema;
			try {
				logger.info("init index template '{}' with settings & schema", indexName);
				esClient.indices()
						.putIndexTemplate(it -> it.name(indexName).withJson(new ByteArrayInputStream(schema)));
				logger.info("index template '{}' created, creating datastream ...", indexName);
				esClient.indices().createDataStream(d -> d.name(dataStreamName));
				logger.info("datastream '{}' created, waiting for green...", dataStreamName);
				esClient.cluster().health(h -> h.index(dataStreamName).waitForStatus(HealthStatus.Green));
			} catch (Exception e) {
				logger.error("Cannot init '{}' datastream: {}", dataStreamName, e.getMessage());
				throw new ElasticIndexException(indexName, e);
			}
		}, () -> {
			logger.warn("no SCHEMA for {}", indexName);
			try {
				esClient.indices().putIndexTemplate(it -> it.name(indexName));
				esClient.indices().createDataStream(d -> d.name(dataStreamName));
			} catch (Exception e) {
				logger.error("Cannot init '{}' datastream: {}", dataStreamName, e.getMessage());
				throw new ElasticIndexException(indexName, e);
			}
		});
	}

	private record IndexTemplateDefinition(String indexTemplateName, byte[] schema, String datastreamName) {
		boolean supportsIndex(String name) {
			return name.equals(indexTemplateName);
		}
	}

	private static Optional<IndexTemplateDefinition> indexDefinitionOf(String index) {
		return indexTemplates.values().stream().filter(item -> item.supportsIndex(index)).findFirst();
	}

	public static <T extends ApiClient<?, ?>> T buildClient(String tag, ElasticsearchTransport transport,
			Function<ElasticsearchTransport, T> builder) {
		if (transport == null) {
			logger.error("no elasticsearch instance found for tag {}", tag);
			return null;
		} else {
			return builder.apply(transport);
		}
	}

	@Override
	public void resetDatastream() {
		indexTemplates.values().forEach(v -> resetDataStream(v.indexTemplateName, v.datastreamName));
	}

	@Override
	public void createDataStreamIfNotExists(String name) {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		indexDefinitionOf(name).map(indexDefinition -> ESearchActivator.indexNames(esClient).stream() //
				.filter(indexDefinition::supportsIndex) //
				.findFirst() //
				.orElseGet(() -> {
					initDataStream(esClient, indexDefinition.indexTemplateName, indexDefinition.datastreamName);
					return indexDefinition.datastreamName;
				}));

	}

	@VisibleForTesting
	@Override
	public void removeDatastream() {
		indexTemplates.values().forEach(v -> {
			ESearchActivator.waitForElasticsearchHosts();
			ElasticsearchClient esClient = ESearchActivator.getClient();
			deleteDataStream(esClient, v.datastreamName);
			deleteIndexTemplate(esClient, v.indexTemplateName);
		});
	}

}
