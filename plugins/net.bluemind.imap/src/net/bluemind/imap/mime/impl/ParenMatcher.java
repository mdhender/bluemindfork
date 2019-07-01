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
package net.bluemind.imap.mime.impl;

public class ParenMatcher {

	private static char charAt(byte[] bytes, int i) {
		return (char) bytes[i];
	}

	private static int indexOf(byte[] bytes, char c, int pos) {
		int idx = pos;
		while (charAt(bytes, idx) != c) {
			idx++;
		}
		return idx;
	}

	private static byte[] substring(byte[] bytes, int start, int end) {
		byte[] ret = new byte[end - start];
		System.arraycopy(bytes, start, ret, 0, ret.length);
		return ret;
	}

	public static final int closingParenIndex(byte[] bs, int parsePosition) {
		int open = 1;
		int currentPosition = parsePosition + 1;
		while (currentPosition < bs.length && open != 0) {
			char c = charAt(bs, currentPosition);
			if (c == '"') {
				currentPosition = indexOf(bs, '"', currentPosition + 1) + 1;
			} else if (c == '{') {
				int size = currentPosition + 1;
				while (Character.isDigit(charAt(bs, size))) {
					size++;
				}
				int bytes = Integer.parseInt(new String(substring(bs, currentPosition + 1, size)));
				// 2 times for '}' added by another minig crap
				if (charAt(bs, size) == '}') {
					size++;
				}
				if (charAt(bs, size) == '}') {
					size++;
				}
				int atomStart = size;
				currentPosition = atomStart + bytes;
			} else {
				if (c == '(') {
					open++;
				} else if (c == ')') {
					open--;
				}
				currentPosition++;
			}
		}
		return currentPosition - 1;
	}

}
