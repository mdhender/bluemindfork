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
import java.util.List;
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
import net.bluemind.core.auditlogs.client.es.AudiLogEsClientActivator;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogStoreConfig;
import net.bluemind.core.auditlogs.exception.AuditLogCreationException;
import net.bluemind.core.auditlogs.exception.AuditLogRemovalException;

public class DataStreamActivator implements BundleActivator, IAuditLogMgmt {

	private static final IndexTemplateDefinition indexTemplateDefinition = loadIndexTemplateDefinition();
	private static final ILMPolicyDefinition ilmPolicyDefinition = loadILMPolicyDefinition();

	private static Logger logger = LoggerFactory.getLogger(DataStreamActivator.class);

	@Override
	public void start(BundleContext arg0) throws Exception {
		/// Nothing to do
	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		/// Nothing to do
	}

	private static IndexTemplateDefinition loadIndexTemplateDefinition() {
		IExtensionPoint ep = Platform.getExtensionRegistry()
				.getExtensionPoint("net.bluemind.core.auditlogs.client.es.datastreams", "indextemplate");
		for (IExtension ext : ep.getExtensions()) {
			for (IConfigurationElement ce : ext.getConfigurationElements()) {
				String indexTemplateName = ce.getAttribute("indextemplatename");
				String schema = ce.getAttribute("resource");
				Bundle bundle = Platform.getBundle(ext.getContributor().getName());
				URL url = bundle.getResource(schema);
				try (InputStream in = url.openStream()) {
					return new IndexTemplateDefinition(indexTemplateName, ByteStreams.toByteArray(in));
				} catch (IOException e) {
					logger.error("Cannot open audit log configuration file '{}'", url.getFile());
					return null;
				}
			}
		}
		return null;
	}

	private static ILMPolicyDefinition loadILMPolicyDefinition() {
		IExtensionPoint ep = Platform.getExtensionRegistry()
				.getExtensionPoint("net.bluemind.core.auditlogs.client.es.datastreams", "ilmpolicy");
		for (IExtension ext : ep.getExtensions()) {
			for (IConfigurationElement ce : ext.getConfigurationElements()) {
				String ilmPolicyName = ce.getAttribute("name");
				String schema = ce.getAttribute("resource");
				Bundle bundle = Platform.getBundle(ext.getContributor().getName());
				URL url = bundle.getResource(schema);
				try (InputStream in = url.openStream()) {
					return new ILMPolicyDefinition(ilmPolicyName, ByteStreams.toByteArray(in));
				} catch (IOException e) {
					logger.error("Cannot open audit log ILM policy file '{}'", url.getFile());
					return null;
				}
			}
		}
		return null;
	}

	private record IndexTemplateDefinition(String indexTemplateName, byte[] schema) {

	}

	private record ILMPolicyDefinition(String name, byte[] schema) {

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

	/*
	 * Creates datastream and corresponding index template using audit log pattern
	 * name defined in configuration file auditlog-store.conf
	 */
	@Override
	public void setupAuditLogBackingStore(String domainUid) throws AuditLogCreationException {
		String dataStreamFullName = AuditLogStoreConfig.resolveDataStreamName(domainUid);
		try {
			createDataStream(dataStreamFullName);
		} catch (ElasticsearchException | IOException e) {
			throw new AuditLogCreationException(e);
		}
	}

	public static List<String> dataStreamNames(ElasticsearchClient esClient) {
		try {
			return esClient.indices().resolveIndex(r -> r.name("*")).dataStreams() //
					.stream().map(ResolveIndexDataStreamsItem::name).toList();
		} catch (ElasticsearchException | IOException e) {
			logger.error("Failed to list datastreams", e);
			return Collections.emptyList();
		}
	}

	private static void initDataStream(ElasticsearchClient esClient, String dataStreamName)
			throws ElasticsearchException, IOException {
		esClient.indices().createDataStream(d -> d.name(dataStreamName));
		logger.info("datastream '{}' created, waiting for green...", dataStreamName);
		esClient.cluster().health(h -> h.index(dataStreamName).waitForStatus(HealthStatus.Green));
	}

	private static void initOrUpdateIndexTemplate(ElasticsearchClient esClient,
			IndexTemplateDefinition indexTemplateDefinition, String indexPattern)
			throws ElasticsearchException, IOException {
		final String indexPatternField = "index_patterns";
		JsonObject jsonSchema = new JsonObject(new String(indexTemplateDefinition.schema));

		// We need to change the index_patterns for the index_template definition, to
		// allow datastream association
		JsonArray indexPatternsSchemaArray = (!jsonSchema.containsKey(indexPatternField)) ? new JsonArray()
				: jsonSchema.getJsonArray(indexPatternField);
		logger.info("Update index_patterns field with '{}' for audit log index template", indexPattern);
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

		jsonSchema.put(indexPatternField, indexPatternsSchemaArray);
		byte[] enhancedSchema = jsonSchema.toString().getBytes();
		esClient.indices().putIndexTemplate(it -> it.name(indexTemplateDefinition.indexTemplateName)
				.withJson(new ByteArrayInputStream(enhancedSchema)));
	}

	private static void updateILMPolicy(ElasticsearchClient esClient, ILMPolicyDefinition ilmPolicyDefinition)
			throws ElasticsearchException, IOException {
		if (ilmPolicyDefinition != null) {
			esClient.ilm().putLifecycle(r -> r.name(ilmPolicyDefinition.name)
					.withJson(new ByteArrayInputStream(ilmPolicyDefinition.schema)));
		}
	}

	/*
	 * Removes all datastreams and index templates
	 */
	@VisibleForTesting
	@Override
	public void removeAuditLogBackingStores() {
		ElasticsearchClient esClient = AudiLogEsClientActivator.get();
		Optional<IndexTemplateDefinition> optSchema = Optional.ofNullable(indexTemplateDefinition);
		// domainUId is not present, removes datastream root (e.g. audit_log*)
		String dataStreamName = "*";
		if (optSchema.isPresent()) {
			try {
				removeDataStream(esClient, dataStreamName);
				removeIndexTemplate(esClient, optSchema.get().indexTemplateName);
			} catch (AuditLogRemovalException e) {
				logger.error("Error on audit log store removal: {}", e.getMessage());
			}
		}
	}

	@Override
	public void removeAuditLogBackingStore(String domainUid) {
		String dataStreamFullName = AuditLogStoreConfig.resolveDataStreamName(domainUid);
		try {
			removeDataStream(dataStreamFullName);
		} catch (AuditLogRemovalException e) {
			logger.error("Failed to delete audit log store '{}': {}", dataStreamFullName, e.getMessage());
		}
	}

	@Override
	public boolean hasAuditLogBackingStore(String domainUid) {
		String dataStreamName = AuditLogStoreConfig.resolveDataStreamName(domainUid);
		try {
			return hasDataStream(dataStreamName);
		} catch (ElasticsearchException | IOException e) {
			logger.error(e.getMessage());
			return false;
		}
	}

	/*
	 * Creates datastream and index template using datastream full name defined as
	 * method argument
	 */
	public void createDataStream(String dataStreamFullName) throws ElasticsearchException, IOException {
		ElasticsearchClient esClient = AudiLogEsClientActivator.get();
		List<String> currentDataStreams = dataStreamNames(esClient);
		Optional<IndexTemplateDefinition> optSchema = Optional.ofNullable(indexTemplateDefinition);
		if (!currentDataStreams.contains(dataStreamFullName) && optSchema.isPresent()) {
			updateILMPolicy(esClient, ilmPolicyDefinition);
			initOrUpdateIndexTemplate(esClient, optSchema.get(), dataStreamFullName);
			initDataStream(esClient, dataStreamFullName);
		}
	}

	@VisibleForTesting
	public void removeDataStream(String dataStreamFullName) throws AuditLogRemovalException {
		ElasticsearchClient esClient = AudiLogEsClientActivator.get();
		Optional<IndexTemplateDefinition> optSchema = Optional.ofNullable(indexTemplateDefinition);
		// domainUId is not present, removes datastream root (e.g. audit_log*)
		String dataStreamName = String.format(AuditLogStoreConfig.getDataStreamName(), "*");
		if (optSchema.isPresent()) {
			removeDataStream(esClient, dataStreamName);
			removeIndexTemplate(esClient, optSchema.get().indexTemplateName);
		}
	}

	public boolean hasDataStream(String dataStreamFullName) throws ElasticsearchException, IOException {
		ElasticsearchClient esClient = AudiLogEsClientActivator.get();
		return !esClient.indices().resolveIndex(i -> i.name(dataStreamFullName)).dataStreams().isEmpty();
	}

	private static void removeDataStream(ElasticsearchClient esClient, String dataStreamName)
			throws AuditLogRemovalException {
		Optional<IndexTemplateDefinition> optSchema = Optional.ofNullable(indexTemplateDefinition);
		if (optSchema.isPresent()) {
			try {
				esClient.indices().deleteDataStream(d -> d.name(Arrays.asList(dataStreamName)));
				logger.info("datastream '{}' deleted.", dataStreamName);
			} catch (ElasticsearchException e) {
				if (e.error() != null && "index_not_found_exception".equals(e.error().type())) {
					logger.warn("dataStream '{}' not found, can't be delete", dataStreamName);
					return;
				}
				throw new AuditLogRemovalException(e);
			} catch (IOException e) {
				throw new AuditLogRemovalException(e);
			}
		}
	}

	private static void removeIndexTemplate(ElasticsearchClient esClient, String indexTemplateName)
			throws AuditLogRemovalException {
		try {
			esClient.indices().deleteIndexTemplate(it -> it.name(Arrays.asList(indexTemplateName)));
			logger.info("index template '{}' deleted.", indexTemplateName);
		} catch (ElasticsearchException e) {
			if (e.error() != null && "index_template_missing_exception".equals(e.error().type())) {
				logger.warn("index template '{}' not found, can't be delete", indexTemplateName);
				return;
			}
			throw new AuditLogRemovalException(e);
		} catch (IOException e) {
			throw new AuditLogRemovalException(e);
		}
	}

}