package net.bluemind.cli.index;

import java.util.Optional;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.index.reconstruct.AliasReconstruct;
import net.bluemind.lib.elasticsearch.ESearchActivator;
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

		AliasReconstruct reconstruct = AliasReconstruct.get(ctx, index, esClient);

		reconstruct.getLinkedAliases();

		ctx.info("Resetting index: " + index);
		ESearchActivator.resetIndex(index);

		reconstruct.reassignAliases();

		ctx.info("Starting dir entry consolidation");
		reconstruct.consolidateMailboxes();
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
