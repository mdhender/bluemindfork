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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
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

public class DirEntryTargetFilter {
	public static class DirEntryWithDomain {
		public final String domainUid;
		public final ItemValue<DirEntry> dirEntry;

		public DirEntryWithDomain(String domainUid, ItemValue<DirEntry> dirEntry) {
			this.domainUid = domainUid;
			this.dirEntry = dirEntry;
		}

		public String sortKey() {
			return this.domainUid + "-" + (dirEntry.value != null ? dirEntry.value.email : "null") + "-" + dirEntry.uid;
		}
	}

	private final String target;
	private final CliUtils cliUtils;
	private final CliContext ctx;
	private final Kind[] dirEntriesKind;
	private final String dirEntryMatch;

	public DirEntryTargetFilter(CliContext ctx, String target, Kind[] dirEntriesKind, String dirEntryMatch) {
		this.target = target;
		this.dirEntriesKind = dirEntriesKind;
		this.dirEntryMatch = dirEntryMatch;
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
	}

	public List<DirEntryWithDomain> getEntries() {
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
		List<DirEntryWithDomain> entries = domainUids.stream().map(domainUid -> getDomainEntries(domainUid, email))
				.flatMap(List::stream).collect(Collectors.toList());
		entries.sort((a, b) -> a.sortKey().compareTo(b.sortKey()));
		return entries;
	}

	private List<DirEntryWithDomain> getDomainEntries(String domainUid, Optional<String> email) {
		IDirectory dirApi = ctx.adminApi().instance(IDirectory.class, domainUid);

		List<ItemValue<DirEntry>> rootEntry = Collections.emptyList();
		if (!email.isPresent() && Sets.newHashSet(dirEntriesKind).contains(Kind.DOMAIN)) {
			DirEntry root = dirApi.getRoot();
			rootEntry = Arrays.asList(ItemValue.create(domainUid, root));
		}

		DirEntryQuery q = DirEntryQuery.filterKind(dirEntriesKind);
		q.hiddenFilter = false;
		q.emailFilter = email.orElse(null);
		if (target.equals("admin0@global.virt")) {
			q.systemFilter = false;
			q.kindsFilter = Arrays.asList(Kind.USER);
		}

		ListResult<ItemValue<DirEntry>> entries = dirApi.search(q);

		if (dirEntryMatch != null && !dirEntryMatch.isEmpty()) {
			Pattern p = Pattern.compile(dirEntryMatch, Pattern.CASE_INSENSITIVE);
			entries.values = entries.values.stream().filter(de -> {
				boolean match = false;
				if (de.value != null && de.value.email != null) {
					match = p.matcher(de.value.email).matches();
				} else {
					match = p.matcher(unaccent(de.displayName)).matches();
				}
				return match;
			}).collect(Collectors.toList());
		}

		return Stream.of(rootEntry, entries.values).flatMap(List::stream)
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
