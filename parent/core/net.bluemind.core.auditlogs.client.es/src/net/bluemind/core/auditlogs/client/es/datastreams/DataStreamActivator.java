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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import co.elastic.clients.elasticsearch.indices.get_index_template.IndexTemplateItem;
import co.elastic.clients.elasticsearch.indices.resolve_index.ResolveIndexDataStreamsItem;
import co.elastic.clients.transport.ElasticsearchTransport;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.exception.ElasticIndexException;

public class DataStreamActivator implements BundleActivator, IAuditLogMgmt {

	private static final Map<String, IndexTemplateDefinition> indexTemplates = new HashMap<>();
	private static final String SEPARATOR = "_";
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

	private static void deleteDataStream(ElasticsearchClient esClient, String dataStreamName, String domainUid) {
		String fullDataStreamName = (domainUid != null) ? dataStreamName + SEPARATOR + domainUid : dataStreamName + "*";
		try {

			esClient.indices().deleteDataStream(d -> d.name(Arrays.asList(fullDataStreamName)));
			logger.info("datastream '{}' deleted.", fullDataStreamName);
		} catch (ElasticsearchException e) {
			if (e.error() != null && "index_not_found_exception".equals(e.error().type())) {
				logger.warn("dataStream '{}' not found, can't be delete", fullDataStreamName);
				return;
			}
			throw new ElasticIndexException(fullDataStreamName, e);
		} catch (IOException e) {
			throw new ElasticIndexException(fullDataStreamName, e);
		}
	}

	private static void deleteIndexTemplate(ElasticsearchClient esClient, String indexTemplateName, String domainUid) {
		String fullIndexTemplateName = (domainUid != null) ? indexTemplateName + SEPARATOR + domainUid
				: indexTemplateName + "*";
		try {
			esClient.indices().deleteIndexTemplate(it -> it.name(Arrays.asList(fullIndexTemplateName)));
			logger.info("index template '{}' deleted.", fullIndexTemplateName);

		} catch (ElasticsearchException e) {
			if (e.error() != null && "index_template_missing_exception".equals(e.error().type())) {
				logger.warn("index template '{}' not found, can't be delete", fullIndexTemplateName);
				return;
			}
			throw new ElasticIndexException(fullIndexTemplateName, e);
		} catch (IOException e) {
			throw new ElasticIndexException(fullIndexTemplateName, e);
		}
	}

	private record IndexTemplateDefinition(String indexTemplateName, byte[] schema, String datastreamName) {
		boolean supportsIndex(String name) {
			return name.equals(indexTemplateName);
		}

		boolean supportsDataStream(String name) {
			return name.equals(datastreamName);
		}
	}

	private static Optional<IndexTemplateDefinition> indexDefinitionOf(String index) {
		return indexTemplates.values().stream().filter(item -> item.supportsIndex(index)).findFirst();
	}

	private static boolean isDataStream(ElasticsearchClient esClient, String dataStream)
			throws ElasticsearchException, IOException {
		return !esClient.indices().resolveIndex(i -> i.name(dataStream)).dataStreams().isEmpty();
	}

	private static Optional<IndexTemplateItem> indexTemplateDefinitionOf(ElasticsearchClient esClient,
			String indexTemplateName) throws ElasticsearchException, IOException {
		return esClient.indices().getIndexTemplate().indexTemplates().stream()
				.filter(i -> i.name().equals(indexTemplateName)).findFirst();
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
		// TODO SCL : review
//		indexTemplates.values().forEach(v -> resetAllDataStreams(v.indexTemplateName, v.datastreamName));
	}

	@Override
	public void createDataStreamIfNotExists(String dataStreamName, String domainUid) {
		String dataStreamFullName = dataStreamName + SEPARATOR + domainUid;
		ElasticsearchClient esClient = ESearchActivator.getClient();
		List<String> currentDataStreams = dataStreamNames(esClient);
		Optional<IndexTemplateDefinition> optSchema = indexTemplates.values().stream()
				.filter(d -> d.datastreamName.equals(dataStreamName)).findFirst();

		try {
			initOrUpdateIndexTemplate(esClient, optSchema.get(), dataStreamFullName);
			if (!currentDataStreams.contains(dataStreamFullName) && optSchema.isPresent()) {
				IndexTemplateDefinition schema = optSchema.get();
				if (!isDataStream(esClient, dataStreamFullName)) {
					initDataStream(esClient, dataStreamFullName);
				}

			}
		} catch (ElasticsearchException | IOException e) {
			// TODO: handle exception
		}
	}

	public static List<String> dataStreamNames(ElasticsearchClient esClient) {
		try {
			return esClient.indices().resolveIndex(r -> r.name("*")).dataStreams() //
					.stream().map(ResolveIndexDataStreamsItem::name).toList();
		} catch (ElasticsearchException | IOException e) {
			logger.error("[es][indices] Failed to list indices", e);
			return Collections.emptyList();
		}
	}

	private static void initDataStream(ElasticsearchClient esClient, String dataStreamName) {
		try {
			esClient.indices().createDataStream(d -> d.name(dataStreamName));
			logger.info("datastream '{}' created, waiting for green...", dataStreamName);
			esClient.cluster().health(h -> h.index(dataStreamName).waitForStatus(HealthStatus.Green));
		} catch (Exception e) {
			logger.error("Cannot init '{}' datastream: {}", dataStreamName, e.getMessage());
		}

	}

	private static void initOrUpdateIndexTemplate(ElasticsearchClient esClient,
			IndexTemplateDefinition indexTemplateDefinition, String indexPattern)
			throws ElasticsearchException, IOException {
		JsonObject jsonSchema = new JsonObject(new String(indexTemplateDefinition.schema));
		JsonArray indexPatternsSchemaArray = (!jsonSchema.containsKey("index_patterns")) ? new JsonArray()
				: jsonSchema.getJsonArray("index_patterns");
		logger.info("Update index_patterns with '{}' for index template '{}' ", indexPattern,
				indexTemplateDefinition.indexTemplateName);
		indexPatternsSchemaArray.add(indexPattern + "*");

		indexTemplateDefinitionOf(esClient, indexTemplateDefinition.indexTemplateName).ifPresent(indexTemplate -> {
			// index template is present -> add indexPattern to index_patterns and updates
			List<String> indexPatterns = indexTemplate.indexTemplate().indexPatterns();
			indexPatterns.forEach(a -> {
				if (!indexPatternsSchemaArray.contains(a)) {
					indexPatternsSchemaArray.add(a);
				}
			});
		});

		jsonSchema.put("index_patterns", indexPatternsSchemaArray);
		byte[] enhancedSchema = jsonSchema.toString().getBytes();
		esClient.indices().putIndexTemplate(it -> it.name(indexTemplateDefinition.indexTemplateName)
				.withJson(new ByteArrayInputStream(enhancedSchema)));
	}

	@VisibleForTesting
	@Override
	public void removeDatastreamForPrefix(String dataStreamPrefix) {
		indexTemplates.values().stream().filter(d -> d.datastreamName.equals(dataStreamPrefix)).forEach(v -> {
			ESearchActivator.waitForElasticsearchHosts();
			ElasticsearchClient esClient = ESearchActivator.getClient();
			deleteDataStream(esClient, v.datastreamName, null);
			deleteIndexTemplate(esClient, v.indexTemplateName, null);
		});
	}

	@VisibleForTesting
	public void removeAllDatastream() {
		indexTemplates.values().forEach(v -> {
			ESearchActivator.waitForElasticsearchHosts();
			ElasticsearchClient esClient = ESearchActivator.getClient();
			deleteDataStream(esClient, v.datastreamName, null);
			deleteIndexTemplate(esClient, v.indexTemplateName, null);
		});
	}

	@Override
	public void removeDatastreamForPrefixAndDomain(String dataStreamPrefix, String domainUid) {
		indexTemplates.values().stream().filter(d -> d.datastreamName.equals(dataStreamPrefix)).forEach(v -> {
			ESearchActivator.waitForElasticsearchHosts();
			ElasticsearchClient esClient = ESearchActivator.getClient();
			deleteDataStream(esClient, v.datastreamName, domainUid);
			deleteIndexTemplate(esClient, v.indexTemplateName, domainUid);
		});

	}

}