/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.indices.AliasDefinition;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import io.vertx.core.Vertx;
import jakarta.json.JsonObject;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.VertxEsTaskMonitor;
import net.bluemind.lib.elasticsearch.exception.ElasticTaskException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 *
 */
@Command(name = "reindexmails", description = "Reindex mail indexes")
public class ReindexMailIndexesCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ReindexMailIndexesCommand.class;
		}

	}

	@Option(names = "--indexes", description = "comma separated list of indexes (e.q. 2,5,6)")
	public String indexes;
	@Option(names = "--script", description = "path to a script file")
	public String script;
	@Option(names = "--slices", description = "number of elasticsearch worker slices", defaultValue = "5")
	public Integer slices = 5;
	@Option(names = "--batchSize", description = "document batch size", defaultValue = "100")
	public Integer batchSize = 100;
	private CliContext ctx;

	@Override
	public void run() {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		Map<String, IndexAliases> indexAliases = null;
		try {
			indexAliases = esClient.indices().getAlias().result();
		} catch (ElasticsearchException | IOException e) {
			ctx.error("Failed to list indices alias", e);
			return;
		}

		Optional<Script> code = loadScript();
		int maxIndex = getMaxIndex(indexAliases.keySet());
		for (String index : getIndexList(indexAliases.keySet())) {
			String targetIndex = "mailspool_" + ++maxIndex;
			try {
				reindex(esClient, index, indexAliases.get(index), targetIndex, code);
			} catch (ElasticsearchException | IOException | ElasticTaskException e) {
				ctx.error("Failed to reindex index {} to {}", index, targetIndex, e);
				return;
			}
			swicthIndex(esClient, index, indexAliases.get(index), targetIndex);
		}
	}

	private Set<String> getIndexList(Set<String> indexNames) {
		if (indexes != null) {
			return Arrays.asList(indexes.split(",")).stream().map(i -> "mailspool_" + i.trim())
					.collect(Collectors.toSet());
		} else {
			return indexNames.stream()
					.filter(indexName -> indexName.startsWith("mailspool") && !indexName.contains("pending"))
					.collect(Collectors.toSet());
		}
	}

	private int getMaxIndex(Set<String> indexNames) {
		return indexNames.stream() //
				.filter(indexName -> indexName.startsWith("mailspool") && !indexName.contains("pending"))
				.mapToInt(indexName -> Integer.valueOf(indexName.substring("mailspool_".length()))).max().orElse(0);
	}

	private void reindex(ElasticsearchClient esClient, String index, IndexAliases aliases, String targetIndex,
			Optional<Script> code) throws ElasticsearchException, IOException, ElasticTaskException {
		ctx.info("Reindexing records from {} to {}", index, targetIndex);
		ESearchActivator.initIndex(esClient, targetIndex);
		Query bodiesQuery = TermQuery.of(t -> t.field("body_msg_link").value("body"))._toQuery();
		moveAndReindex(esClient, Optional.empty(), index, targetIndex, bodiesQuery, "bodies");

		for (String alias : aliases.aliases().keySet()) {
			String entityId = getEntityIdByAlias(alias);
			ctx.info("Reindexing records of alias {} owned by {}", alias, entityId);
			Query ownerQuery = TermQuery.of(t -> t.field("owner").value(entityId))._toQuery();
			moveAndReindex(esClient, code, index, targetIndex, ownerQuery, "records");
		}
	}

	private void swicthIndex(ElasticsearchClient esClient, String index, IndexAliases aliases, String targetIndex) {
		for (Entry<String, AliasDefinition> alias : aliases.aliases().entrySet()) {
			String entityId = getEntityIdByAlias(alias.getKey());
			ctx.info("Moving alias for {}", entityId);
			try {
				esClient.indices().updateAliases(u -> u //
						.actions(a -> a.add(add -> add //
								.index(targetIndex).alias(alias.getKey()).filter(alias.getValue().filter())))
						.actions(a -> a.remove(r -> r.index(index).alias(alias.getKey()))));
			} catch (ElasticsearchException | IOException e) {
				ctx.warn("Failed to move alias for {}", entityId);
			}
		}

		ctx.info("Deleting index {}", index);
		try {
			esClient.indices().delete(d -> d.index(index));
		} catch (ElasticsearchException | IOException e) {
			ctx.warn("Failed to delete index {}", index);
		}
	}

	private Optional<Script> loadScript() {
		if (script == null) {
			return Optional.empty();
		}
		try {
			String code = new String(Files.readAllBytes(new File(script).toPath()));
			ctx.info("Applying following code to reindex action:\r\n" + code);
			return Optional.of(Script.of(s -> s.inline(i -> i.lang("painless").source(code))));
		} catch (IOException e) {
			throw new CliException("Cannot read code from script file " + script + ": " + e.getMessage());
		}
	}

	private String getEntityIdByAlias(String aliasValue) {
		return aliasValue.substring("mailspool_alias_".length());
	}

	private void moveAndReindex(ElasticsearchClient esClient, Optional<Script> script, String srcIndex,
			String targetIndex, Query query, String type)
			throws ElasticsearchException, IOException, ElasticTaskException {
		ctx.info("Reindexing index {} to {}", srcIndex, targetIndex);
		ReindexResponse response = esClient.reindex(r -> {
			r //
					.waitForCompletion(false) //
					.source(s -> s.index(srcIndex).size(batchSize).query(query)) //
					.dest(d -> d.index(targetIndex).opType(OpType.Index)) //
					.slices(s -> s.value(slices)) //
					.conflicts(Conflicts.Proceed) //
					.scroll(s -> s.time("1d"));
			script.ifPresent(r::script);
			return r;
		});
		JsonObject status = new VertxEsTaskMonitor(Vertx.vertx(), esClient).waitForCompletion(response.task()).toJson()
				.asJsonObject();
		ctx.info("Reindexing {} from index {} to {}: {}", type, srcIndex, targetIndex,
				Matcher.quoteReplacement(status.toString()));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}