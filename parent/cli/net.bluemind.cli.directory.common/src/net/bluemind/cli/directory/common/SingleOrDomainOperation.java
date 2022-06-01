package net.bluemind.cli.directory.common;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.directory.common.DirEntryTargetFilter.DirEntryWithDomain;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class SingleOrDomainOperation implements ICmdLet, Runnable {
	private class OperationResult {
		public final DirEntryWithDomain dirEntryWithDomain;
		public final Exception exception;
		public final boolean noop;

		public OperationResult(DirEntryWithDomain dirEntryWithDomain) {
			this.dirEntryWithDomain = dirEntryWithDomain;
			this.exception = null;
			this.noop = false;
		}

		public OperationResult(DirEntryWithDomain dirEntryWithDomain, Exception exception) {
			this.dirEntryWithDomain = dirEntryWithDomain;
			this.exception = exception;
			this.noop = false;
		}

		public OperationResult(DirEntryWithDomain dirEntryWithDomain, boolean noop) {
			this.dirEntryWithDomain = dirEntryWithDomain;
			this.exception = null;
			this.noop = noop;
		}

		public boolean isError() {
			return exception != null;
		}
	}

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Parameters(paramLabel = "<target>", description = "email address, domain name or 'all' for all domains")
	public String target;

	@Option(names = "--workers", description = "run with X workers")
	public int workers = 1;

	@Option(names = "--match", description = "regex that entity must match, for example : [a-c].*")
	public String match = "";

	@Option(names = "--no-progress", description = "don't display progress messages")
	public boolean noProgress = false;

	public abstract void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws Exception;

	public abstract Kind[] getDirEntryKind();

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	public void run() {
		DirEntryTargetFilter targetFilter = new DirEntryTargetFilter(ctx, target, getDirEntryKind(), match);

		List<DirEntryWithDomain> entriesWithDomainUid = targetFilter.getEntries();
		if (entriesWithDomainUid.isEmpty()) {
			throw new CliException(String.format("Your search for '%s', filtered by '%s' did not match anything",
					target, match.isEmpty() ? "" : match));
		}
		Set<String> domains = entriesWithDomainUid.stream().map(e -> e.domainUid).collect(Collectors.toSet());
		preIterate(domains);

		// create executor & completion service with workers thread
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		CompletionService<OperationResult> opsWatcher = new ExecutorCompletionService<>(pool);

		entriesWithDomainUid.forEach(de -> opsWatcher.submit(() -> {
			try {
				synchronousDirOperation(de.domainUid, de.dirEntry);
			} catch (NoopException no) {
				return new OperationResult(de, true);
			} catch (Exception e) {
				return new OperationResult(de, e);
			}

			return new OperationResult(de);
		}));

		int ended = 0;
		int noops = 0;
		for (int i = 0; i < entriesWithDomainUid.size(); i++) {
			OperationResult operationResult = null;
			try {
				operationResult = opsWatcher.take().get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}

			if (operationResult.noop) {
				noops++;
			}

			if (operationResult.isError()) {
				ctx.error(String.format("Error handling %s: %s",
						Strings.isNullOrEmpty(operationResult.dirEntryWithDomain.dirEntry.value.email)
								? operationResult.dirEntryWithDomain.dirEntry.uid
								: operationResult.dirEntryWithDomain.dirEntry.value.email,
						operationResult.exception.getMessage()));

				if (operationResult.exception instanceof CliException) {
					throw (CliException) operationResult.exception;
				}
			}

			if (!noProgress) {
				ctx.progress(entriesWithDomainUid.size(), ++ended);
			}
		}
		if (noops > 0) {
			int handled = entriesWithDomainUid.size() - noops;
			ctx.warn("Handled " + handled + " entries. " + noops + " entries have been ignored");
		}
	}

	/**
	 * Runs before iterating the entries
	 * 
	 * @param entriesWithDomainUid
	 */
	public void preIterate(@SuppressWarnings("unused") Set<String> domains) {

	}
}
