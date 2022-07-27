package net.bluemind.cli.index;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesAction;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.AliasMetadata;

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

		ctx.info("Checking existing alias on index: " + index);
		Client client = ESearchActivator.getClient();
		List<AliasMetadata> aliasMetadatas = new GetAliasesRequestBuilder(client, GetAliasesAction.INSTANCE).get()
				.getAliases().get(index);
		if (aliasMetadatas == null || aliasMetadatas.isEmpty()) {
			ctx.warn("No aliases registred on index '" + index + "'");
			return;
		}

		ctx.info(aliasMetadatas.size() + " aliases found on index: " + index);

		ctx.info("Resetting index: " + index);
		ESearchActivator.resetIndex(index);

		ctx.info("Adding previous alias to index: " + index);
		IndicesAliasesRequest addAliasRequest = Requests.indexAliasesRequest();
		aliasMetadatas.stream() //
				.map(metadata -> addAliasAction(index, metadata)) //
				.forEach(addAliasRequest::addAliasAction);
		client.admin().indices().aliases(addAliasRequest).actionGet();

		ctx.info("Starting dir entry consolidation");
		Set<String> uids = aliasMetadatas.stream() //
				.map(metadata -> metadata.alias().replace("mailspool_alias_", "")) //
				.collect(Collectors.toSet());
		DirEntryTargetFilter targetFilter = new DirEntryTargetFilter(ctx, "all", dirEntryKind(), "");
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

	private AliasActions addAliasAction(String index, AliasMetadata metadata) {
		return AliasActions.add().alias(metadata.alias()).index(index).filter(metadata.filter().string())
				.writeIndex(true);
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
