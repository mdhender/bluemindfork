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
package net.bluemind.imap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Splitter;

@SuppressWarnings("serial")
public final class FlagsList extends HashSet<Flag> {

	private int uid;
	private static final Splitter tagsSplitter = Splitter.on(' ');

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");

		Iterator<Flag> it = iterator();
		int len = size();
		for (int i = 0; i < len; i++) {
			if (i > 0) {
				sb.append(' ');
			}
			sb.append(it.next().toString());
		}

		sb.append(")");
		return sb.toString();
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public static FlagsList of(List<String> flags) {
		FlagsList ret = new FlagsList();
		flags.stream().map(s -> Flag.from(s.toLowerCase())).filter(Objects::nonNull).forEach(ret::add);
		return ret;
	}

	public static Set<String> tagsFromString(String flagString, boolean autoAdd) {
		FlagsList fl = new FlagsList();
		Iterable<String> it = tagsSplitter.split(flagString);
		for (String s : it) {
			Flag f = Flag.from(s);
			if (f != null) {
				fl.add(f);
			}
		}
		return fl.asTags(autoAdd);
	}

	public static FlagsList fromString(String flagString) {
		FlagsList fl = new FlagsList();
		Iterable<String> it = tagsSplitter.split(flagString);
		for (String s : it) {
			Flag f = Flag.from(s);
			if (f != null) {
				fl.add(f);
			}
		}
		return fl;
	}

	public Set<String> asTags() {
		return asTags(true);
	}

	public Set<String> asTags(boolean autoAddMissing) {
		Set<String> fl = new HashSet<>();
		for (Flag f : this) {
			fl.add(f.name().toLowerCase());
		}
		if (fl.contains("seen")) {
			fl.add("read");
		} else if (autoAddMissing) {
			fl.add("unread");
			fl.add("unseen");
		}
		if (fl.contains("flagged")) {
			fl.add("starred");
		}
		if (fl.contains("bmarchived")) {
			fl.add("archived");
		}
		return fl;
	}

}
