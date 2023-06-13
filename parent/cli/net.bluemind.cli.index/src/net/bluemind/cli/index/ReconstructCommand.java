package net.bluemind.cli.index;

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
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.DirEntryTargetFilter;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.IMailboxMgmt;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "reconstruct", description = "Reconstruct the given mailspool index")
public class ReconstructCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ReconstructCommand.class;
		}

	}

	@Parameters(paramLabel = "<index_name>", description = "target index (must be a mailspool index)")
	public String index;

	private CliContext ctx;

	@Override
	public void run() {
		if (!index.startsWith("mailspool_") || index.equals("mailspool_pending")) {
			ctx.warn("Only mailspool index allowed");
			return;
		}

		ElasticsearchClient esClient = ESearchActivator.getClient();

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

		Map<String, AliasDefinition> aliasDefinitions = aliases.aliases();
		ctx.info(aliasDefinitions.size() + " aliases found on index: " + index);

		ctx.info("Resetting index: " + index);
		ESearchActivator.resetIndex(index);

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
			return;
		}

		ctx.info("Starting dir entry consolidation");
		Set<String> uids = aliasDefinitions.keySet().stream() //
				.map(name -> name.replace("mailspool_alias_", "")) //
				.collect(Collectors.toSet());
		DirEntryTargetFilter targetFilter = DirEntryTargetFilter.allDomains(ctx, dirEntryKind(), Optional.empty());

		targetFilter.getEntries().stream() //
				.filter(e -> uids.contains(e.dirEntry.uid)) //
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
	}

	private Kind[] dirEntryKind() {
		return new Kind[] { Kind.GROUP, Kind.MAILSHARE, Kind.USER, Kind.RESOURCE };
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
