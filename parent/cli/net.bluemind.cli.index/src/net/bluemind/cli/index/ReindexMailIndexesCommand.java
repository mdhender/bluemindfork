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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesAction;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexAction;
import org.elasticsearch.index.reindex.ReindexRequestBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.lib.elasticsearch.ESearchActivator;
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
		Client client = ESearchActivator.getClient();
		ImmutableOpenMap<String, List<AliasMetadata>> getAliasesResponse = new GetAliasesRequestBuilder(client,
				GetAliasesAction.INSTANCE, new String[0]).get().getAliases();

		Optional<Script> code = loadScript();
		int maxIndex = getMaxIndex(getAliasesResponse);
		for (String index : getIndexList(getAliasesResponse)) {
			reindex(client, index, getAliasesResponse.get(index), ++maxIndex, code);
		}
	}

	private Set<String> getIndexList(ImmutableOpenMap<String, List<AliasMetadata>> getAliasesResponse) {
		if (indexes != null) {
			return Arrays.asList(indexes.split(",")).stream().map(i -> "mailspool_" + i.trim())
					.collect(Collectors.toSet());
		} else {
			Set<String> toIndex = new HashSet<>();
			for (Iterator<String> keysIt = getAliasesResponse.keysIt(); keysIt.hasNext();) {
				String indexName = keysIt.next();
				if (indexName.startsWith("mailspool") && !indexName.contains("pending")) {
					toIndex.add(indexName);
				}
			}
			return toIndex;
		}

	}

	private int getMaxIndex(ImmutableOpenMap<String, List<AliasMetadata>> getAliasesResponse) {
		int max = 0;
		for (Iterator<String> keysIt = getAliasesResponse.keysIt(); keysIt.hasNext();) {
			String indexName = keysIt.next();
			if (indexName.startsWith("mailspool") && !indexName.contains("pending")) {
				max = Math.max(max, Integer.valueOf(indexName.substring("mailspool_".length())));
			}
		}
		return max;
	}

	private void reindex(Client client, String index, List<AliasMetadata> aliases, int targetIndexNumber,
			Optional<Script> code) {
		String targetIndex = "mailspool_" + targetIndexNumber;
		ctx.info("Reindexing records from {} to {}", index, targetIndex);
		ESearchActivator.initIndex(client, targetIndex);
		TermQueryBuilder bodiesQuery = QueryBuilders.termQuery("body_msg_link", "body");
		moveAndReindex(client, Optional.empty(), index, targetIndex, bodiesQuery, "bodies");

		for (AliasMetadata alias : aliases) {
			String entityId = getEntityIdByAlias(alias.alias());
			ctx.info("Reindexing records of alias {} owned by {}", alias.alias(), entityId);
			TermQueryBuilder ownerQuery = QueryBuilders.termQuery("owner", entityId);
			moveAndReindex(client, code, index, targetIndex, ownerQuery, "records");
			ctx.info("Adding alias of {}", entityId);
			client.admin().indices().prepareAliases().addAlias(targetIndex, alias.alias(), ownerQuery)
					.removeAlias(index, alias.alias()).get();
		}
		ctx.info("Deleting index {}", index);
		client.admin().indices().prepareDelete(index).get();
	}

	private Optional<Script> loadScript() {
		if (script == null) {
			return Optional.empty();
		}
		try {
			String code = new String(Files.readAllBytes(new File(script).toPath()));
			ctx.info("Applying following code to reindex action:\r\n" + code);
			return Optional.of(new Script(ScriptType.INLINE, "painless", code, Collections.emptyMap()));
		} catch (IOException e) {
			throw new CliException("Cannot read code from script file " + script + ": " + e.getMessage());
		}
	}

	private String getEntityIdByAlias(String aliasValue) {
		return aliasValue.substring("mailspool_alias_".length());
	}

	private void moveAndReindex(Client client, Optional<Script> script, String srcIndex, String targetIndex,
			TermQueryBuilder ownerQuery, String type) {
		ctx.info("Reindexing index {} to {}", srcIndex, targetIndex);

		ReindexRequestBuilder builder = new ReindexRequestBuilder(client, ReindexAction.INSTANCE).source(srcIndex)
				.destination(targetIndex).setSlices(slices).abortOnVersionConflict(false).filter(ownerQuery);
		builder.destination().setOpType(OpType.INDEX);
		builder.source().setSize(batchSize);
		script.ifPresent(builder::script);
		BulkByScrollResponse ret = builder.get();
		ctx.info("Reindexing {} from index {} to {}: {}", type, srcIndex, targetIndex,
				Matcher.quoteReplacement(ret.toString()));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}