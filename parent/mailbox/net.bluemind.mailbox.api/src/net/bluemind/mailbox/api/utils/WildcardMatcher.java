/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bluemind.mailbox.api.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class WildcardMatcher {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final int NOT_FOUND = -1;

	private WildcardMatcher() {

	}

	public static boolean match(final String fileName, final String wildcardMatcher) {
		if (fileName == null && wildcardMatcher == null) {
			return true;
		}
		if (fileName == null || wildcardMatcher == null) {
			return false;
		}
		final String[] wcs = splitOnTokens(wildcardMatcher);
		boolean anyChars = false;
		int textIdx = 0;
		int wcsIdx = 0;
		final Deque<int[]> backtrack = new ArrayDeque<>(wcs.length);

		// loop around a backtrack stack, to handle complex * matching
		do {
			if (!backtrack.isEmpty()) {
				final int[] array = backtrack.pop();
				wcsIdx = array[0];
				textIdx = array[1];
				anyChars = true;
			}

			// loop whilst tokens and text left to process
			while (wcsIdx < wcs.length) {

				if (wcs[wcsIdx].equals("?")) {
					// ? so move to next text char
					textIdx++;
					if (textIdx > fileName.length()) {
						break;
					}
					anyChars = false;

				} else if (wcs[wcsIdx].equals("*")) {
					// set any chars status
					anyChars = true;
					if (wcsIdx == wcs.length - 1) {
						textIdx = fileName.length();
					}

				} else {
					// matching text token
					if (anyChars) {
						// any chars then try to locate text token
						textIdx = checkIndexOf(fileName, textIdx, wcs[wcsIdx]);
						if (textIdx == NOT_FOUND) {
							// token not found
							break;
						}
						final int repeat = checkIndexOf(fileName, textIdx + 1, wcs[wcsIdx]);
						if (repeat >= 0) {
							backtrack.push(new int[] { wcsIdx, repeat });
						}
					} else {
						// matching from current position
						if (!checkRegionMatches(fileName, textIdx, wcs[wcsIdx])) {
							// couldnt match token
							break;
						}
					}

					// matched text token, move text index to end of matched token
					textIdx += wcs[wcsIdx].length();
					anyChars = false;
				}

				wcsIdx++;
			}

			// full match
			if (wcsIdx == wcs.length && textIdx == fileName.length()) {
				return true;
			}

		} while (!backtrack.isEmpty());

		return false;
	}

	private static String[] splitOnTokens(final String text) {
		// used by wildcardMatch
		// package level so a unit test may run on this

		if (text.indexOf('?') == NOT_FOUND && text.indexOf('*') == NOT_FOUND) {
			return new String[] { text };
		}

		final char[] array = text.toCharArray();
		final ArrayList<String> list = new ArrayList<>();
		final StringBuilder buffer = new StringBuilder();
		char prevChar = 0;
		for (final char ch : array) {
			if (ch == '?' || ch == '*') {
				if (buffer.length() != 0) {
					list.add(buffer.toString());
					buffer.setLength(0);
				}
				if (ch == '?') {
					list.add("?");
				} else if (prevChar != '*') {// ch == '*' here; check if previous char was '*'
					list.add("*");
				}
			} else {
				buffer.append(ch);
			}
			prevChar = ch;
		}
		if (buffer.length() != 0) {
			list.add(buffer.toString());
		}

		return list.toArray(EMPTY_STRING_ARRAY);
	}

	private static int checkIndexOf(final String str, final int strStartIndex, final String search) {
		final int endIndex = str.length() - search.length();
		if (endIndex >= strStartIndex) {
			for (int i = strStartIndex; i <= endIndex; i++) {
				if (checkRegionMatches(str, i, search)) {
					return i;
				}
			}
		}
		return -1;
	}

	private static boolean checkRegionMatches(final String str, final int strStartIndex, final String search) {
		return str.regionMatches(false, strStartIndex, search, 0, search.length());
	}
}
