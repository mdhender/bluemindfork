/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.integrity.check;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;

public class SpoolDirectoryValidator {

	private static final Logger logger = LoggerFactory.getLogger(SpoolDirectoryValidator.class);

	public static class Builder {
		private List<CyrusPartition> parts = Collections.emptyList();
		private List<MailboxEntry> entries = Collections.emptyList();

		public Builder validPartitions(List<CyrusPartition> parts) {
			this.parts = parts;
			return this;
		}

		public Builder backendEntries(List<MailboxEntry> entries) {
			this.entries = entries;
			return this;
		}

		public SpoolDirectoryValidator build() {
			Set<String> domainPrefixes = parts
					.stream().map(cp -> cp.name + "/domain/"
							+ CyrusFileSystemPathHelper.mapLetter(cp.domainUid.charAt(0)) + "/" + cp.domainUid)
					.collect(Collectors.toSet());
			Map<String, List<MailboxEntry>> rootsByDomain = entries.stream()
					.collect(Collectors.groupingBy(me -> me.domain, Collectors.toList()));
			Map<String, Set<String>> prefixesByDomain = new HashMap<>();
			rootsByDomain.entrySet().forEach(entry -> {
				prefixesByDomain.put(entry.getKey(),
						entry.getValue().stream().flatMap(me -> me.filesystemPrefixes()).collect(Collectors.toSet()));
			});
			return new SpoolDirectoryValidator(domainPrefixes, prefixesByDomain);
		}

	}

	public static String letter(String domainOrMailbox) {
		char ret = Character.toLowerCase(domainOrMailbox.charAt(0));
		if (ret < 'a' || ret > 'z') {
			ret = 'q';
		}
		return Character.toString(ret);
	}

	public static Builder builder() {
		return new Builder();
	}

	private Set<String> domainPrefixes;

	private Map<String, Set<String>> mailboxRootsByDomain;

	private static final Splitter slashSplit = Splitter.on('/');

	private SpoolDirectoryValidator(Set<String> domainPrefixes, Map<String, Set<String>> mailboxRootsByDomain) {
		this.domainPrefixes = domainPrefixes;
		this.mailboxRootsByDomain = mailboxRootsByDomain;
	}

	public boolean verify(String spoolDirectory) {
		logger.debug("Verify '{}'", spoolDirectory);

		String[] dirParts = slashSplit.splitToList(spoolDirectory).toArray(new String[0]);

		if (dirParts.length == 1 && dirParts[0].equals("mail")) {
			// the default partition
			return true;
		}

		if (dirParts.length < 4) {
			// skip parent dirs, eg. 172_17_0_4__test1569862422297_lab/domain
			return true;
		}

		String[] firstParts = new String[4];
		System.arraycopy(dirParts, 0, firstParts, 0, 4);
		// top directory is in a known partition root
		String prefix = String.join("/", firstParts);
		if (!domainPrefixes.contains(prefix)) {
			logger.warn("'{}' is not in a valid prefix ({} are valid)", prefix, domainPrefixes);
			return false;
		}
		if (spoolDirectory.equals(prefix)) {
			return true;
		}
		String domainUid = firstParts[3].replace('_', '.');
		Set<String> roots = mailboxRootsByDomain.get(domainUid);
		String trailer = spoolDirectory.substring(prefix.length() + 1);
		if (!roots.contains(trailer)) {
			String[] trailParts = slashSplit.splitToList(trailer).toArray(new String[0]);

			// s/bang^bus/Sent
			if (trailParts.length == 3 && !trailParts[1].equals("user")) {
				boolean letterIsRight = trailParts[0]
						.equals(String.valueOf(CyrusFileSystemPathHelper.mapLetter(trailParts[2].charAt(0))));
				String basePrefix = CyrusFileSystemPathHelper.mapLetter(trailParts[1].charAt(0)) + "/" + trailParts[1];

				logger.debug("letter is right: {}, base: {}", letterIsRight, basePrefix);
				return letterIsRight && roots.contains(basePrefix);
			}
			logger.warn("Unknown trailer: '{}' ({} are valid)", trailer, roots);
			return false;
		}

		return true;
	}

}
