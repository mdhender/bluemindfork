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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import net.bluemind.directory.hollow.datamodel.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.AnrToken;
import net.bluemind.directory.hollow.datamodel.Email;

public class AnrTokens {

	private static AnrToken token(String s) {
		AnrToken ant = new AnrToken();
		ant.token = s;
		return ant;
	}

	private AnrTokens() {
	}

	private static final Splitter EMAIL_CHUNKS = Splitter.on(CharMatcher.anyOf(".-")).omitEmptyStrings();
	private static final Splitter DN_CHUNKS = Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();

	public static List<AnrToken> compute(AddressBookRecord rec) {
		Set<String> tokens = new HashSet<>();
		int min = 2;
		int max = 5;
		if (!Strings.isNullOrEmpty(rec.name)) {
			for (String chunk : DN_CHUNKS.split(rec.name)) {
				tokens.addAll(edgeNGrams(chunk, min, max));
			}
		}
		if (!Strings.isNullOrEmpty(rec.email)) {
			tokens.add(rec.email.toLowerCase());
			String localPart = rec.email.substring(0, rec.email.indexOf('@'));
			for (String chunk : EMAIL_CHUNKS.split(localPart)) {
				tokens.addAll(edgeNGrams(chunk, min, max));
			}
		}
		for (Email e : rec.emails) {
			tokens.add(e.address.toLowerCase());
			String localPart = e.address.substring(0, e.address.indexOf('@'));
			for (String chunk : EMAIL_CHUNKS.split(localPart)) {
				tokens.addAll(edgeNGrams(chunk, min, max));
			}
		}
		return tokens.stream().map(AnrTokens::token).collect(Collectors.toList());
	}

	private static Set<String> edgeNGrams(String s, int min, int max) {
		Set<String> ngrams = new HashSet<>();
		int len = s.length();
		ngrams.add(s);
		ngrams.add(unaccent(s));
		if (len > min) {
			for (int j = min; j < Math.min(max, len + 1); j++) {
				String sub = s.substring(0, j);
				ngrams.add(sub);
				ngrams.add(unaccent(sub));
			}
		}
		return ngrams;
	}

	private static String unaccent(String src) {
		return Normalizer.normalize(src, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

}
