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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import net.bluemind.directory.hollow.datamodel.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.AnrToken;
import net.bluemind.directory.hollow.datamodel.Email;

public class AnrTokens extends EdgeNgram<AnrToken> {

	public AnrTokens() {
		super(2, 5);
	}

	private static final Splitter EMAIL_CHUNKS = Splitter.on(CharMatcher.anyOf(".-")).omitEmptyStrings();
	private static final Splitter DN_CHUNKS = Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();

	public List<AnrToken> compute(AddressBookRecord rec) {
		Set<AnrToken> tokens = new HashSet<>();
		if (!Strings.isNullOrEmpty(rec.name)) {
			for (String chunk : DN_CHUNKS.split(rec.name)) {
				tokens.addAll(new AnrTokens().compute(chunk));
			}
		}
		if (!Strings.isNullOrEmpty(rec.email)) {
			tokens.add(map(rec.email.toLowerCase()));
			String localPart = rec.email.substring(0, rec.email.indexOf('@'));
			for (String chunk : EMAIL_CHUNKS.split(localPart)) {
				tokens.addAll(new AnrTokens().compute(chunk));
			}
		}
		for (Email e : rec.emails) {
			tokens.add(map(e.address.toLowerCase()));
			String localPart = e.address.substring(0, e.address.indexOf('@'));
			for (String chunk : EMAIL_CHUNKS.split(localPart)) {
				tokens.addAll(new AnrTokens().compute(chunk));
			}
		}
		return new ArrayList<>(tokens);
	}

	@Override
	public AnrToken map(String value) {
		AnrToken ant = new AnrToken();
		ant.token = value;
		return ant;
	}

}
