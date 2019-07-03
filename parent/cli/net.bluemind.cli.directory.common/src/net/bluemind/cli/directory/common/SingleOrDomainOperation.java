package net.bluemind.cli.directory.common;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import io.airlift.airline.Arguments;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;
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

		Pattern p = Pattern.compile(match, Pattern.CASE_INSENSITIVE);

		String email = null;
		if (target.contains("@")) {
			if ("admin0@global.virt".equals(target)) {
				email = target;
			} else {
				if (!Regex.EMAIL.validate(target)) {
					throw new ServerFault("Not an email");
				}
				IMailboxes mboxApi = ctx.adminApi().instance(IMailboxes.class, domainUid);
				ItemValue<Mailbox> resolved = mboxApi.byEmail(target);
				if (resolved == null) {
					ctx.error("No mailbox matches " + target);
					return;
				}
				email = resolved.value.defaultEmail().address;
			}
		}

		// create executor & completion service with workers thread
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		CompletionService<Void> opsWatcher = new ExecutorCompletionService<>(pool);
		IDirectory dirApi = ctx.adminApi().instance(IDirectory.class, domainUid);
		DirEntryQuery q = DirEntryQuery.filterKind(getDirEntryKind());
		q.hiddenFilter = false;
		q.emailFilter = email;
		if (target.equals("admin0@global.virt")) {
			q.systemFilter = false;
			q.kindsFilter = Arrays.asList(Kind.USER);
		}
		ListResult<ItemValue<DirEntry>> entries = dirApi.search(q);
		if (entries.total == 0) {
			ctx.error("Your search for '" + email + "' did not match anything");
			return;
		}

		if (!match.isEmpty()) {
			entries.values = entries.values.stream().filter(de -> p.matcher(unaccent(de.displayName)).matches())
					.collect(Collectors.toList());
			entries.total = entries.values.size();
		}
		if (email == null && Sets.newHashSet(getDirEntryKind()).contains(Kind.DOMAIN)) {
			// domain repair, also repair the root entry
			DirEntry root = dirApi.getRoot();
			try {
				synchronousDirOperation(domainUid, ItemValue.create(domainUid, root));
			} catch (Exception e) {
				throw new CliException("Error handling domain ", e);
			}
		}
		for (ItemValue<DirEntry> de : entries.values) {
			opsWatcher.submit(() -> {
				try {
					synchronousDirOperation(domainUid, de);
				} catch (Exception e) {
					throw new CliException("Error handling dirEntry : " + de.uid, e);
				}
				return null;
			});
		}
		entries.values.forEach(de -> {
			try {
				opsWatcher.take().get();
			} catch (Exception e) {
				throw new CliException(e);
			}
		});
	}

	private String unaccent(String src) {
		return Normalizer.normalize(src, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

}
