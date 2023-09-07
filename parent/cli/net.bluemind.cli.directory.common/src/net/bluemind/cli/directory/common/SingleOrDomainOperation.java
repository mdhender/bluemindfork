package net.bluemind.cli.directory.common;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import io.netty.util.concurrent.DefaultThreadFactory;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.directory.common.DirEntryTargetFilter.DirEntryWithDomain;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import picocli.CommandLine.ArgGroup;
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

	@ArgGroup(exclusive = true, multiplicity = "1")
	public Scope scope;

	public static class Scope {
		@Parameters(paramLabel = "<target>", description = "email address or domain name")
		public String target;

		@Option(names = "--direntry-uid", required = false, description = "Directory entry UID")
		public String dirEntryUid;

		@Option(names = "--all-domains", required = false, description = "All domains except global.virt")
		public boolean allDomains;
	}

	@Option(names = "--workers", defaultValue = "1", description = "run with X workers (default: ${DEFAULT-VALUE})")
	public int workers = 1;

	@Option(names = "--match", description = "regex that entity must match, for example : [a-c].*")
	public String match;

	@Option(names = "--progress", description = "display progress messages")
	public boolean progress = false;

	public abstract void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws Exception;

	public abstract Kind[] getDirEntryKind();

	public void done() {

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	public Runnable forTarget(String target) {
		scope = new Scope();
		scope.target = target;
		return this;
	}

	public void run() {
		DirEntryTargetFilter targetFilter = targetFilterFromScope();

		List<DirEntryWithDomain> entriesWithDomainUid = targetFilter.getEntries();
		if (entriesWithDomainUid.isEmpty()) {
			throw new CliException("Your search for "
					+ (scope.allDomains ? "all domains except global.virt"
							: "'" + Optional.ofNullable(scope.target).orElse(scope.dirEntryUid))
					+ "'" //
					+ (match != null ? ", filtered by '" + match + "'" : "") //
					+ " did not match anything");
		}
		Set<String> domains = entriesWithDomainUid.stream().map(e -> e.domainUid).collect(Collectors.toSet());
		preIterate(domains);

		// create executor & completion service with workers thread
		ExecutorService pool = Executors.newFixedThreadPool(workers, new DefaultThreadFactory("cli-repair"));
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

			if (progress) {
				ctx.progress(entriesWithDomainUid.size(), ++ended);
			}
		}
		if (noops > 0) {
			int handled = entriesWithDomainUid.size() - noops;
			ctx.warn("Handled " + handled + " entries. " + noops + " entries have been ignored");
		}
		done();
	}

	private DirEntryTargetFilter targetFilterFromScope() {
		if (scope.allDomains) {
			return DirEntryTargetFilter.allDomains(ctx, getDirEntryKind(), Optional.ofNullable(match));
		}

		if (!Strings.isNullOrEmpty(scope.target)) {
			return DirEntryTargetFilter.byTarget(ctx, scope.target, getDirEntryKind(), Optional.ofNullable(match));
		}

		if (!Strings.isNullOrEmpty(scope.dirEntryUid)) {
			return DirEntryTargetFilter.byUid(ctx, scope.dirEntryUid, getDirEntryKind(), Optional.ofNullable(match));
		}

		throw new CliException("Invalid scope!");
	}

	/**
	 * Runs before iterating the entries
	 * 
	 * @param entriesWithDomainUid
	 */
	public void preIterate(@SuppressWarnings("unused") Set<String> domains) {

	}
}
