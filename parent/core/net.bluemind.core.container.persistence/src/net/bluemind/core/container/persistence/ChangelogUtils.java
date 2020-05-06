/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlagFilter;

public final class ChangelogUtils {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ChangelogUtils.class);

	public static <T extends Comparable<T>> ContainerChangeset<T> toChangesetByExtId(IWeightProvider wp, long from,
			ContainerChangelog cc, final Function<ChangeLogEntry, T> toId) {
		List<ChangeLogEntry> entries = cc.entries;
		Comparator<ChangeLogEntry> sortByExtIdAndVersionAsc = new Comparator<ChangeLogEntry>() {

			@Override
			public int compare(ChangeLogEntry o1, ChangeLogEntry o2) {
				T id1 = toId.apply(o1);
				T id2 = toId.apply(o2);
				int ret = 0;
				if (id1.equals(id2)) {
					ret = Long.compare(o1.version, o2.version);
				} else {
					ret = id1.compareTo(id2);
				}
				return ret;
			}
		};
		entries.sort(sortByExtIdAndVersionAsc);

		return toChangeset(wp, from, entries, toId, ItemFlagFilter.all());
	}

	public static <T extends Comparable<?>, E extends ChangeLogEntry> ContainerChangeset<T> toChangeset(
			IWeightProvider wp, long from, List<E> entries, final Function<E, T> toId, ItemFlagFilter filter) {

		ArrayList<T> updated = new ArrayList<>(entries.size());
		ArrayList<T> created = new ArrayList<>(entries.size());
		ArrayList<T> deleted = new ArrayList<>(entries.size());

		ArrayList<E> zipped = new ArrayList<>(entries.size());
		E last = null;
		long version = -1;
		for (E entry : entries) {
			entry.weightSeed = wp.weight(entry.weightSeed);

			if (version < entry.version) {
				version = entry.version;
			}
			if (last != null && !toId.apply(last).equals(toId.apply(entry))) {
				zipped.add(last);
				last = null;
			}

			if (last == null) {
				last = entry;
			} else if (toId.apply(last).equals(toId.apply(entry))) {
				// "compress"
				last = zipChange(last, entry);
			}
		}

		if (last != null) {
			zipped.add(last);
		}

		zipped.sort((e1, e2) -> {
			return Long.compare(e2.weightSeed, e1.weightSeed);
		});

		for (E entry : zipped) {
			if (!entry.match(filter)) {
				if (from > 0) {
					deleted.add(toId.apply(entry));
				}
				// no deletions for initial sync
			} else {
				switch (entry.type) {
				case Created:
					created.add(toId.apply(entry));
					break;
				case Updated:
					updated.add(toId.apply(entry));
					break;
				case Deleted:
					if (from > 0) {
						deleted.add(toId.apply(entry));
					}
					// no deletions for initial sync
					break;
				default:
					break;
				}
			}
		}

		if (version == -1) {
			version = from;
		}

		return ContainerChangeset.create(created, updated, deleted, version);
	}

	private static <T extends ChangeLogEntry> T zipChange(T before, T after) {
		if (before.type == Type.Created && after.type == Type.Deleted) {
			return null;
		}

		if (before.type == Type.Created && after.type == Type.Updated) {
			after.type = Type.Created;
			return after;
		}

		return after;
	}

}
