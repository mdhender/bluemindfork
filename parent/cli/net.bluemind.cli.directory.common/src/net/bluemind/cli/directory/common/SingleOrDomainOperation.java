package net.bluemind.cli.directory.common;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

import io.airlift.airline.Arguments;
import io.airlift.airline.Option;
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

public abstract class SingleOrDomainOperation implements ICmdLet, Runnable {
	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Arguments(required = true, description = "email address or domain name")
	public String target;

	@Option(name = "--workers", description = "run with X workers")
	public int workers = 1;

	@Option(name = "--match", description = "regex that entity must match, for example : [a-c].*")
	public String match = "";

	public abstract void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws Exception;

	public abstract Kind[] getDirEntryKind();

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	public void run() {
		String domainUid = cliUtils.getDomainUidFromEmailOrDomain(target);

		Optional<String> email = getDefaultEmailFromTarget(domainUid);

		List<ItemValue<DirEntry>> entries = getEntries(domainUid, email);
		if (entries.isEmpty()) {
			throw new CliException(String.format("Your search for '%s', filtered by '%s' did not match anything",
					target, match.isEmpty() ? "" : match));
		}

		// create executor & completion service with workers thread
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		CompletionService<Void> opsWatcher = new ExecutorCompletionService<>(pool);

		entries.forEach(de -> opsWatcher.submit(() -> {
			try {
				synchronousDirOperation(domainUid, de);
			} catch (Exception e) {
				throw new CliException(String.format("Error handling dirEntry : %s", de.uid), e);
			}

			return null;
		}));

		int ended = 0;
		for (int i = 0; i < entries.size(); i++) {
			try {
				opsWatcher.take().get();
				ctx.progress(entries.size(), ++ended);
			} catch (Exception e) {
				throw new CliException(e);
			}
		}
	}

	private List<ItemValue<DirEntry>> getEntries(String domainUid, Optional<String> email) {
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

		return Stream.of(rootEntry, entries.values).flatMap(x -> x.stream()).collect(Collectors.toList());
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
