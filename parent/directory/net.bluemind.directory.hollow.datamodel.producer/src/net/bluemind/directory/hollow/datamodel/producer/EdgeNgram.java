/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.directory.hollow.datamodel.producer;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class EdgeNgram<T> {
	private final int minLength;
	private final int maxLength;

	public EdgeNgram(int minLength, int maxLength) {
		this.minLength = minLength;
		this.maxLength = maxLength != -1 ? maxLength : Integer.MAX_VALUE;
	}

	public abstract T map(String value);

	public List<T> compute(String value) {
		return edgeNGrams(value).stream().map(this::map).collect(Collectors.toList());
	}

	private Set<String> edgeNGrams(String value) {
		Set<String> ngrams = new HashSet<>();
		int len = value.length();
		ngrams.add(value);
		ngrams.add(unaccent(value));
		if (len > minLength) {
			for (int j = minLength; j < Math.min(maxLength, len + 1); j++) {
				String sub = value.substring(0, j);
				ngrams.add(sub);
				ngrams.add(unaccent(sub));
			}
		}
		return ngrams;
	}

	private static String unaccent(String src) {
		return Normalizer.normalize(src, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

	public static class StringEdgeNgram extends EdgeNgram<String> {

		public StringEdgeNgram(int minLength, int maxLength) {
			super(minLength, maxLength);
		}

		@Override
		public String map(String value) {
			return value;
		}

	}

	public static class EmailEdgeNGram extends StringEdgeNgram {

		public EmailEdgeNGram() {
			super(3, -1);
		}

	}

}
