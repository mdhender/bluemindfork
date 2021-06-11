package net.bluemind.cli.directory.common;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class SingleOrDomainOperation implements ICmdLet, Runnable {
	private class DirEntryWithDomain {
		public final String domainUid;
		public final ItemValue<DirEntry> dirEntry;

		public DirEntryWithDomain(String domainUid, ItemValue<DirEntry> dirEntry) {
			this.domainUid = domainUid;
			this.dirEntry = dirEntry;
		}
	}

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
		List<DirEntryWithDomain> entriesWithDomainUid = getEntries();
		if (entriesWithDomainUid.isEmpty()) {
			throw new CliException(String.format("Your search for '%s', filtered by '%s' did not match anything",
					target, match.isEmpty() ? "" : match));
		}

		// create executor & completion service with workers thread
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		CompletionService<OperationResult> opsWatcher = new ExecutorCompletionService<OperationResult>(pool);

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
			ctx.info("Handled " + handled + " entries. " + noops + " entries have been ignored");
		}
	}

	private List<DirEntryWithDomain> getEntries() {
		List<String> domainUids = new ArrayList<>();
		Optional<String> email = Optional.empty();

		if (target.equals("all")) {
			domainUids.addAll(cliUtils.getDomainUids());
		} else {
			domainUids.add(cliUtils.getDomainUidByEmailOrDomain(target));
			email = getDefaultEmailFromTarget(domainUids.get(0));
		}

		return getEntries(domainUids, email);
	}

	private List<DirEntryWithDomain> getEntries(List<String> domainUids, Optional<String> email) {
		return domainUids.stream().map(domainUid -> getDomainEntries(domainUid, email))
				.flatMap(entries -> entries.stream()).collect(Collectors.toList());
	}

	private List<DirEntryWithDomain> getDomainEntries(String domainUid, Optional<String> email) {
		IDirectory dirApi = ctx.adminApi().instance(IDirectory.class, domainUid);

		List<ItemValue<DirEntry>> rootEntry = Collections.emptyList();
		if (!email.isPresent() && Sets.newHashSet(getDirEntryKind()).contains(Kind.DOMAIN)) {
			DirEntry root = dirApi.getRoot();
			rootEntry = Arrays.asList(ItemValue.create(domainUid, root));
		}

		DirEntryQuery q = DirEntryQuery.filterKind(getDirEntryKind());
		q.hiddenFilter = false;
		q.emailFilter = email.orElse(null);
		if (target.equals("admin0@global.virt")) {
			q.systemFilter = false;
			q.kindsFilter = Arrays.asList(Kind.USER);
		}

		ListResult<ItemValue<DirEntry>> entries = dirApi.search(q);

		Pattern p = Pattern.compile(match, Pattern.CASE_INSENSITIVE);
		if (!match.isEmpty()) {
			entries.values = entries.values.stream().filter(de -> p.matcher(unaccent(de.displayName)).matches())
					.collect(Collectors.toList());
		}

		return Stream.of(rootEntry, entries.values).flatMap(x -> x.stream())
				.map(de -> new DirEntryWithDomain(domainUid, de)).collect(Collectors.toList());
	}

	/**
	 * If target is an email, return corresponding default email
	 * 
	 * @param domainUid
	 * @return null if target is not an email, defaultEmail otherwise
	 */
	private Optional<String> getDefaultEmailFromTarget(String domainUid) {
		Optional<String> email = Optional.empty();

		if (target.contains("@")) {
			if ("admin0@global.virt".equals(target)) {
				email = Optional.of(target);
			} else {
				if (!Regex.EMAIL.validate(target)) {
					throw new CliException(String.format("Target is not a valid email %s", target));
				}

				IMailboxes mboxApi = ctx.adminApi().instance(IMailboxes.class, domainUid);
				ItemValue<Mailbox> resolved = mboxApi.byEmail(target);
				if (resolved == null) {
					throw new CliException(String.format("No mailbox matches %s", target));
				}

				email = Optional.of(resolved.value.defaultEmail().address);
			}
		}

		return email;
	}

	private String unaccent(String src) {
		return Normalizer.normalize(src, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}
}
