/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.mail.replica.api.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.backend.mail.replica.api.ImapBinding;

public class UidRanges {

	public static class UidRange {
		public long lowBound;
		public long highBound;

		public String toString() {
			return lowBound == highBound ? "[" + lowBound + "]" : "[" + lowBound + ", " + highBound + "]";
		}

		public int size() {
			return (int) ((highBound - lowBound) + 1);
		}
	}

	public static List<ImapBinding> notInRange(List<UidRange> ranges, List<ImapBinding> msgs) {
		Map<Long, ImapBinding> indexByUid = msgs.stream().collect(Collectors.toMap(b -> b.imapUid, b -> b));
		for (UidRange r : ranges) {
			for (long i = r.lowBound; i <= r.highBound; i++) {
				if (indexByUid.containsKey(i)) {
					indexByUid.remove(i);
				}
			}
		}
		return new ArrayList<>(indexByUid.values());
	}

	public static boolean contains(List<UidRange> ranges, long v) {
		boolean ret = false;
		for (UidRange r : ranges) {
			if (v >= r.lowBound && v <= r.highBound) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	public static List<UidRange> from(String idSet) {
		if (Strings.isNullOrEmpty(idSet)) {
			return Collections.emptyList();
		}

		List<UidRange> ranges = new LinkedList<>();
		UidRange current = new UidRange();
		StringBuilder digitBuilder = new StringBuilder();
		char[] chars = idSet.toCharArray();
		boolean lowBoundFound = false;
		boolean highBoundFound = false;
		for (char c : chars) {
			switch (c) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				digitBuilder.append(c);
				break;
			case '*':
				current.highBound = Long.MAX_VALUE;
				highBoundFound = true;
				break;
			case ':':
				long colonUid = Long.parseLong(digitBuilder.toString());
				digitBuilder.setLength(0);
				current.lowBound = colonUid;
				lowBoundFound = true;
				break;
			case ',':
				if (highBoundFound == true) {
					// just add
				} else {
					long commaUid = Long.parseLong(digitBuilder.toString());
					digitBuilder.setLength(0);
					if (!lowBoundFound) {
						current.lowBound = commaUid;
						current.highBound = commaUid;
					} else {
						current.highBound = commaUid;
					}
				}
				ranges.add(current);
				current = new UidRange();
				lowBoundFound = false;
				highBoundFound = false;
				break;
			}
		}
		if (highBoundFound == true) {
			// just add
		} else {
			long commaUid = Long.parseLong(digitBuilder.toString());
			if (!lowBoundFound) {
				current.lowBound = commaUid;
				current.highBound = commaUid;
			} else {
				current.highBound = commaUid;
			}
		}
		ranges.add(current);
		return ranges;
	}

}
