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
package net.bluemind.imap.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageSet {

	private static final Logger logger = LoggerFactory.getLogger(MessageSet.class);

	public static final String asString(Collection<Integer> uids) {
		if (uids == null) {
			return "1:*";
		}

		TreeSet<Integer> sortedUids = new TreeSet<Integer>(uids);
		StringBuilder sb = new StringBuilder(uids.size() * 7);
		long firstUid = 0;
		long lastUid = 0;
		boolean firstLoop = true;
		for (Integer currentValue : sortedUids) {
			if (firstUid > 0 && currentValue == lastUid + 1) {
				lastUid = currentValue;
				firstLoop = false;
				continue;
			}
			if (firstUid > 0 && lastUid > 0 && lastUid > firstUid) {
				sb.append(':');
				sb.append(lastUid);
				firstUid = 0;
				lastUid = 0;
			}
			if (!firstLoop) {
				sb.append(',');
			}
			sb.append(currentValue);
			firstUid = currentValue;
			lastUid = currentValue;
			firstLoop = false;
		}
		if (firstUid > 0 && lastUid > 0 && lastUid > firstUid) {
			sb.append(':');
			sb.append(lastUid);
		}

		String ret = sb.toString();
		if (logger.isDebugEnabled()) {
			logger.debug("computed set string: " + ret);
		}
		return ret;

	}

	public static ArrayList<Integer> asLongCollection(String set, int sizeHint) {
		String[] parts = set.split(",");
		ArrayList<Integer> ret = new ArrayList<Integer>(sizeHint > 0 ? sizeHint : parts.length);
		for (String s : parts) {
			if (!s.contains(":")) {
				ret.add(Integer.parseInt(s));
			} else {
				String[] p = s.split(":");
				int start = Integer.parseInt(p[0]);
				int end = Integer.parseInt(p[1]);
				for (int l = start; l <= end; l++) {
					ret.add(l);
				}
			}
		}
		return ret;
	}

	public static ArrayList<Integer> asFilteredLongCollection(String set, long minValue) {
		String[] parts = set.split(",");
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for (String s : parts) {
			if (!s.contains(":")) {
				int l = Integer.parseInt(s);
				if (l >= minValue) {
					ret.add(Integer.parseInt(s));
				}
			} else {
				String[] p = s.split(":");
				int start = Integer.parseInt(p[0]);
				int end = Integer.parseInt(p[1]);
				for (int l = start; l <= end; l++) {
					if (l >= minValue) {
						ret.add(l);
					}
				}
			}
		}
		return ret;
	}

}
