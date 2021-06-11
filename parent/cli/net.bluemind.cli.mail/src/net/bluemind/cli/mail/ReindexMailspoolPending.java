/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.mail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexAction;
import org.elasticsearch.index.reindex.ReindexRequestBuilder;
import org.osgi.framework.Bundle;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.PublicInfos;
import picocli.CommandLine.Command;

@Command(name = "reindexpending", description = "Reindex mailspool_pending in a new index")
public class ReindexMailspoolPending implements ICmdLet, Runnable {

	@SuppressWarnings("serial")
	private class ReindexException extends RuntimeException {
		ReindexException(String message) {
			super(message);
		}
	}

	private CliContext ctx;

	public static final String INDEX_SCHEMA_NAME = "mailspool_pending";
	public static final String INDEX_ALIAS = "mailspool_pending_alias";

	@Override
	public void run() {
		PublicInfos infos = CliContext.get().adminApi().instance(IInstallation.class).getInfos();
		ctx.info("infos: " + infos.softwareVersion + " " + infos.releaseName);

		try {
			Client client = ESearchActivator.getClient();
			String fromIndexName = aliasIndexName(client, INDEX_ALIAS)
					.orElseThrow(() -> new ReindexException("No index found for alias " + INDEX_ALIAS));
			getSchema().ifPresent(schema -> doReindex(client, fromIndexName, schema));
		} catch (Exception e) {
			ctx.error("Fail to reindex:\r\n" + ctx.toStack(e));
		}
	}

	private void doReindex(Client client, String fromIndexName, Map<String, Object> schema) {
		String datePart = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String toIndexName = String.format("mailspool_pending_v%s", datePart);
		if (toIndexName.equals(fromIndexName)) {
			toIndexName = toIndexName.concat("-1");
		}

		boolean success = createIndex(client, toIndexName, schema);
		if (!success) {
			throw new ReindexException("Fail to create index " + toIndexName);
		}

		success = reindexAndMonitor(client, fromIndexName, toIndexName).map(this::reportReindexStatus).orElse(false);
		if (!success) {
			deleteIndex(client, toIndexName);
			throw new ReindexException("Fail to reindex document from " + INDEX_ALIAS);
		}

		success = moveAlias(client, fromIndexName, toIndexName);
		if (!success) {
			deleteIndex(client, toIndexName);
			throw new ReindexException("Fail to move alias " + INDEX_ALIAS + " to " + toIndexName);
		}

		success = deleteIndex(client, fromIndexName);
		if (!success) {
			throw new ReindexException("Fail to delete index " + fromIndexName);
		}
		ctx.info("Reindexation process complete");
	}

	private Optional<String> aliasIndexName(Client client, String aliasName) {
		GetAliasesResponse response = client.admin().indices().prepareGetAliases(aliasName).get();
		Iterator<String> aliasIndexNameIterator = response.getAliases().keysIt();
		return aliasIndexNameIterator.hasNext() ? Optional.of(aliasIndexNameIterator.next()) : Optional.empty();
	}

	private boolean createIndex(Client client, String indexName, Map<String, Object> indexSchema) {
		AcknowledgedResponse response = client.admin().indices().prepareCreate(indexName).setSource(indexSchema).get();

		if (response.isAcknowledged()) {
			ctx.info("Index " + indexName + " created");
		}
		return response.isAcknowledged();
	}

	private Optional<BulkByScrollResponse> reindexAndMonitor(Client client, String fromIndexName, String toIndexName) {
		ReindexRequestBuilder builder = ReindexAction.INSTANCE.newRequestBuilder(client).source(INDEX_ALIAS)
				.destination(toIndexName).setSlices(5).abortOnVersionConflict(false);
		builder.destination().setOpType(OpType.INDEX);

		ActionFuture<BulkByScrollResponse> futurResponse = builder.execute();
		long total = client.admin().indices().prepareStats(fromIndexName).get().getTotal().docs.getCount();
		try {
			while (!futurResponse.isDone() && !Thread.currentThread().isInterrupted()) {
				long docs = client.admin().indices().prepareStats(toIndexName).get().getTotal().docs.getCount();
				ctx.progress((int) total, (int) docs);
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			ctx.error("Command interrupted while reindex in progress.");
			Thread.currentThread().interrupt();
		}
		return (!Thread.currentThread().isInterrupted()) ? Optional.of(futurResponse.actionGet()) : Optional.empty();
	}

	private boolean reportReindexStatus(BulkByScrollResponse response) {
		if (!response.getBulkFailures().isEmpty()) {
			ctx.warn(response.getBulkFailures().size() + " failures occurred during reindexation:");
			response.getBulkFailures().forEach(failure -> ctx.warn("- " + failure.getMessage()));
		}
		return response.getBulkFailures().isEmpty();
	}

	private boolean moveAlias(Client client, String fromIndexName, String toIndexName) {
		AliasActions removeAlias = AliasActions.remove().index(fromIndexName).alias(INDEX_ALIAS);
		AliasActions addAlias = AliasActions.add().index(toIndexName).alias(INDEX_ALIAS).writeIndex(true);
		IndicesAliasesRequest req = Requests.indexAliasesRequest().addAliasAction(removeAlias).addAliasAction(addAlias);
		AcknowledgedResponse response = client.admin().indices().aliases(req).actionGet();

		if (response.isAcknowledged()) {
			ctx.info("Index " + toIndexName + " now with alias " + INDEX_ALIAS);
		}
		return response.isAcknowledged();
	}

	private boolean deleteIndex(Client client, String indexName) {
		AcknowledgedResponse response = client.admin().indices().prepareDelete(indexName).get();

		if (response.isAcknowledged()) {
			ctx.info("Index " + indexName + " deleted");
		}
		return response.isAcknowledged();
	}

	private Optional<Map<String, Object>> getSchema() {
		IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.elasticsearch.schema");
		return Arrays.asList(ep.getExtensions()).stream()
				.flatMap(extension -> Arrays.asList(extension.getConfigurationElements()).stream())
				.filter(configurationElement -> configurationElement.getAttribute("index").equals(INDEX_SCHEMA_NAME))
				.findFirst().flatMap(this::readSchema);
	}

	private Optional<Map<String, Object>> readSchema(IConfigurationElement configurationElement) {
		Bundle bundle = Platform.getBundle(configurationElement.getContributor().getName());
		String schema = configurationElement.getAttribute("schema");
		Map<String, Object> jsonMap = null;
		try {
			URLConnection conn = bundle.getResource(schema).openConnection();
			try (InputStream schemaInputStream = conn.getInputStream()) {
				jsonMap = new ObjectMapper().readValue(schemaInputStream, Map.class);
				jsonMap.remove("aliases");
			}
		} catch (IOException e) {
			ctx.error("Fail to read schema from bundle " + bundle.getSymbolicName());
		}
		return Optional.ofNullable(jsonMap);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ReindexMailspoolPending.class;
		}
	}

}
