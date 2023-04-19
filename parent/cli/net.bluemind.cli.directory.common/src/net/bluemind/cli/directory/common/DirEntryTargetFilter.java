/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.directory.common;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class DirEntryTargetFilter {
	public static class DirEntryWithDomain {
		public final String domainUid;
		public final ItemValue<DirEntry> dirEntry;

		public DirEntryWithDomain(String domainUid, ItemValue<DirEntry> dirEntry) {
			this.domainUid = domainUid;
			this.dirEntry = dirEntry;
		}

		public String sortKey() {
			// ensure domain root is processed first
			if (dirEntry.value != null && dirEntry.value.kind == Kind.DOMAIN) {
				return this.domainUid;
			}
			return this.domainUid + "-" + (dirEntry.value != null ? dirEntry.value.email : "null") + "-" + dirEntry.uid;
		}
	}

	private final boolean allDomains;
	private final Optional<String> target;
	private final Optional<String> dirEntryUid;
	private final CliUtils cliUtils;
	private final CliContext ctx;
	private final Kind[] dirEntriesKind;
	private final Optional<String> dirEntryMatch;

	public static DirEntryTargetFilter allDomains(CliContext ctx, Kind[] dirEntriesKind,
			Optional<String> dirEntryMatch) {
		return new DirEntryTargetFilter(ctx, dirEntriesKind, dirEntryMatch);
	}

	/**
	 * 
	 * @param ctx
	 * @param target         email or domain
	 * @param dirEntriesKind
	 * @param dirEntryMatch
	 * @return
	 */
	public static DirEntryTargetFilter byTarget(CliContext ctx, String target, Kind[] dirEntriesKind,
			Optional<String> dirEntryMatch) {
		return Optional.of(target).filter(t -> t.equals("all"))
				.map(t -> new DirEntryTargetFilter(ctx, dirEntriesKind, dirEntryMatch))
				.orElseGet(() -> new DirEntryTargetFilter(ctx, false, target, dirEntriesKind, dirEntryMatch));
	}

	/**
	 * 
	 * @param ctx
	 * @param entryUid       Directory entry UID
	 * @param dirEntriesKind
	 * @param dirEntryMatch
	 * @return
	 */
	public static DirEntryTargetFilter byUid(CliContext ctx, String entryUid, Kind[] dirEntriesKind,
			Optional<String> dirEntryMatch) {
		return new DirEntryTargetFilter(ctx, true, entryUid, dirEntriesKind, dirEntryMatch);
	}

	private DirEntryTargetFilter(CliContext ctx, Kind[] dirEntriesKind, Optional<String> dirEntryMatch) {
		this.allDomains = true;
		this.target = Optional.empty();
		this.dirEntryUid = Optional.empty();

		this.dirEntriesKind = dirEntriesKind;
		this.dirEntryMatch = dirEntryMatch;
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
	}

	private DirEntryTargetFilter(CliContext ctx, boolean targetIsUid, String targetOrUid, Kind[] dirEntriesKind,
			Optional<String> dirEntryMatch) {
		this.allDomains = false;
		this.target = !targetIsUid ? Optional.of(targetOrUid) : Optional.empty();
		this.dirEntryUid = targetIsUid ? Optional.of(targetOrUid) : Optional.empty();

		this.dirEntriesKind = dirEntriesKind;
		this.dirEntryMatch = dirEntryMatch;
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
	}

	public List<DirEntryWithDomain> getEntries() {
		if (allDomains) {
			return getEntries(cliUtils.getDomainUids(), Optional.empty());
		}

		if (target.isPresent()) {
			String targetDomainUid = target.map(cliUtils::getDomainUidByEmailOrDomain)
					.orElseThrow(() -> new CliException("No target domain UID found"));

			return target
					.map(t -> getEntries(Arrays.asList(targetDomainUid), getDefaultEmailFromTarget(targetDomainUid, t)))
					.orElse(Collections.emptyList());
		}

		return dirEntryUid.map(deu -> cliUtils.getDomainUids().stream()
				.map(domainUid -> getDomainEntries(domainUid, dirEntryUid, Optional.empty())).flatMap(List::stream)
				.filter(dewd -> deu.equals(dewd.dirEntry.uid)).findAny().map(Arrays::asList)
				.orElse(Collections.emptyList())).orElse(Collections.emptyList());
	}

	private List<DirEntryWithDomain> getEntries(List<String> domainUids, Optional<String> email) {
		return domainUids.stream().map(domainUid -> getDomainEntries(domainUid,

				Optional.empty(), email)).flatMap(List::stream).toList();
	}

	private List<DirEntryWithDomain> getDomainEntries(String domainUid, Optional<String> dirEntryUid,
			Optional<String> email) {
		IDirectory dirApi = ctx.adminApi().instance(IDirectory.class, domainUid);
		List<ItemValue<DirEntry>> entries = new ArrayList<>();

		dirEntryUid.filter(deu -> deu.equals(domainUid)).map(deu -> getRoot(dirApi, domainUid)).ifPresentOrElse(
				entries::add, () -> email.filter(e -> Sets.newHashSet(dirEntriesKind).contains(Kind.DOMAIN))
						.map(e -> getRoot(dirApi, domainUid)).ifPresent(entries::add));

		DirEntryQuery q = DirEntryQuery.filterKind(dirEntriesKind);
		q.hiddenFilter = false;
		q.entryUidFilter = dirEntryUid.map(Arrays::asList).orElse(null);
		q.emailFilter = email.orElse(null);
		if (target.filter(t -> t.equals("admin0@global.virt")).isPresent()) {
			q.systemFilter = false;
			q.kindsFilter = Arrays.asList(Kind.USER);
		}

		entries.addAll(matchingEntries(dirApi.search(q).values));

		return entries.stream().map(de -> new DirEntryWithDomain(domainUid, de))
				.sorted(Comparator.comparing(DirEntryWithDomain::sortKey)).toList();
	}

	private ItemValue<DirEntry> getRoot(IDirectory dirApi, String domainUid) {
		return Optional.ofNullable(ctx.adminApi().instance(IDirectory.class, domainUid).getRoot())
				.map(root -> ItemValue.create(domainUid, root)).orElse(null);
	}

	private List<ItemValue<DirEntry>> matchingEntries(List<ItemValue<DirEntry>> entries) {
		return dirEntryMatch.map(dEM -> Pattern.compile(dEM, Pattern.CASE_INSENSITIVE))
				.map(p -> entries.stream().filter(de -> isEntryMatching(p, de)).toList()).orElse(entries);
	}

	private boolean isEntryMatching(Pattern p, ItemValue<DirEntry> entry) {
		if (entry.value != null && entry.value.email != null) {
			return p.matcher(entry.value.email).matches();
		}

		return p.matcher(unaccent(entry.displayName)).matches();
	}

	/**
	 * If target is an email, return corresponding default email
	 * 
	 * @param domainUid
	 * @return null if target is not an email, defaultEmail otherwise
	 */
	private Optional<String> getDefaultEmailFromTarget(String domainUid, String target) {
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
