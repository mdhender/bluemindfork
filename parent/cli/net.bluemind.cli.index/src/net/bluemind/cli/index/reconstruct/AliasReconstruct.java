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
package net.bluemind.cli.index.reconstruct;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.AliasDefinition;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.directory.common.DirEntryTargetFilter;
import net.bluemind.cli.directory.common.DirEntryTargetFilter.DirEntryWithDomain;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.lib.elasticsearch.IndexAliasMapping.RingIndexAliasMapping;
import net.bluemind.lib.elasticsearch.IndexAliasMode;
import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;
import net.bluemind.mailbox.api.IMailboxMgmt;

public abstract class AliasReconstruct {

	protected final CliContext ctx;
	protected final String index;
	protected final ElasticsearchClient esClient;
	protected Map<String, AliasDefinition> aliasDefinitions;

	protected AliasReconstruct(CliContext ctx, String index, ElasticsearchClient esClient) {
		this.ctx = ctx;
		this.index = index;
		this.esClient = esClient;
	}

	public static AliasReconstruct get(CliContext ctx, String index, ElasticsearchClient esClient) {
		return IndexAliasMode.getMode() == Mode.ONE_TO_ONE ? new OneToOneAliasReconstruct(ctx, index, esClient)
				: new RingAliasReconstruct(ctx, index, esClient);
	}

	public abstract void reassignAliases();

	public abstract boolean filter(DirEntryWithDomain entry);

	public void getLinkedAliases() {
		ctx.info("Checking existing alias on index: " + index);
		IndexAliases aliases;
		try {
			aliases = esClient.indices().getAlias(a -> a.index(index)).get(index);
		} catch (IOException | ElasticsearchException e) {
			ctx.error("Failed to list aliases on index '" + index + "'", e);
			aliases = null;
		}

		if (aliases == null || aliases.aliases().isEmpty()) {
			ctx.warn("No aliases registred on index '" + index + "'");
			return;
		}

		aliasDefinitions = aliases.aliases();
		ctx.info(aliasDefinitions.size() + " aliases found on index: " + index);
	}

	public void consolidateMailboxes() {
		new CliUtils(ctx).getDomainUids().forEach(domain -> {
			DirEntryTargetFilter targetFilter = DirEntryTargetFilter.allDomains(ctx, dirEntryKind(), Optional.empty());

			targetFilter.getEntries().stream() //
					.filter(this::filter) //
					.collect(Collectors.groupingBy(e -> e.domainUid)) //
					.forEach((domainUid, entries) -> {
						IMailboxMgmt mailboxMgmtApi = ctx.adminApi().instance(IMailboxMgmt.class, domainUid);
						entries.stream().map(e -> e.dirEntry).forEach(entry -> {
							String dirEntryName = (entry.value.email != null && !entry.value.email.isEmpty())
									? (entry.value.email + " (" + entry.uid + ")")
									: entry.uid;
							ctx.info("Consolidation of dir entry: " + dirEntryName);
							TaskRef ref = mailboxMgmtApi.consolidateMailbox(entry.uid);
							Tasks.follow(ctx, ref, dirEntryName,
									String.format("Fail to consolidate mailbox index for entry %s", entry));
						});
					});
		});
	}

	protected Kind[] dirEntryKind() {
		return new Kind[] { Kind.GROUP, Kind.MAILSHARE, Kind.USER, Kind.RESOURCE };
	}

	public static class OneToOneAliasReconstruct extends AliasReconstruct {

		private Set<String> uids;

		public OneToOneAliasReconstruct(CliContext ctx, String index, ElasticsearchClient esClient) {
			super(ctx, index, esClient);
		}

		@Override
		public void getLinkedAliases() {
			super.getLinkedAliases();
			uids = aliasDefinitions.keySet().stream() //
					.map(name -> name.replace("mailspool_alias_", "")) //
					.collect(Collectors.toSet());
		}

		@Override
		public void reassignAliases() {
			ctx.info("Adding previous alias to index: " + index);
			try {
				esClient.indices().updateAliases(u -> {
					aliasDefinitions.entrySet().forEach(entry -> u.actions(a -> a //
							.add(add -> add.index(index).alias(entry.getKey()).filter(entry.getValue().filter()))));
					return u;
				});
			} catch (IOException | ElasticsearchException e) {
				String missing = aliasDefinitions.keySet().stream().collect(Collectors.joining(","));
				ctx.error("Failed to add aliases on index '" + index + "': " + missing, e);
			}
		}

		@Override
		public boolean filter(DirEntryWithDomain entry) {
			return uids.contains(entry.dirEntry.uid);
		}

	}

	public static class RingAliasReconstruct extends AliasReconstruct {

		public RingAliasReconstruct(CliContext ctx, String index, ElasticsearchClient esClient) {
			super(ctx, index, esClient);
		}

		@Override
		public void reassignAliases() {
			// in ring mode, aliases are created during reset
		}

		@Override
		public boolean filter(DirEntryWithDomain entry) {
			return aliasDefinitions.keySet()
					.contains(new RingIndexAliasMapping().getReadAliasByMailboxUid(entry.dirEntry.uid));
		}

	}

}
