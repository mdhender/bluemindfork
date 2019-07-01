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
package net.bluemind.lmtp.impl;

/**
 * Used for ensuring headers lines will not exceed 76 chars
 * 
 * 
 */
public class MimeUtility {

	private static int whitespaceIndexOf(String s, int offset, int step, int len) {
		for (int i = offset; i > 0 && i < len; i += step) {
			char c = s.charAt(i);
			if (c == ' ' || c == '\t')
				return i;
		}
		return -1;
	}

	/**
	 * Folds the specified string such that each line is no longer than 76
	 * characters, whitespace permitting.
	 * 
	 * @param used
	 *            the number of characters used in the line already
	 * @param s
	 *            the string to fold
	 */
	public static String fold(int used, String s) {
		int len = s.length();
		int k = Math.min(76 - used, len);
		if (k == len) {
			return s;
		}
		StringBuilder buf = new StringBuilder();
		int i;
		do {
			i = whitespaceIndexOf(s, k, -1, len);
			if (i == -1) {
				i = whitespaceIndexOf(s, k, 1, len);
			}
			if (i != -1) {
				buf.append(s.substring(0, i));
				buf.append('\n');
				s = s.substring(i);
				len -= i;
			}
			k = Math.min(76, len);
		} while (i != -1);
		buf.append(s);
		return buf.toString();
	}
}
