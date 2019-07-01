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
package net.bluemind.imap.command.parser;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.bluemind.imap.NameSpaceInfo;
import net.bluemind.lib.jutf7.UTF7Converter;

public class NamespaceParser {

	public static final String expectedResponseStart = "* NAMESPACE";

	private static class ConsumedToken {
		String remaining;
		List<String> ns = Collections.emptyList();
	}

	public static NameSpaceInfo parse(String payload) {
		java.lang.String tokens = payload.substring(expectedResponseStart.length() + 1);

		NameSpaceInfo nsi = new NameSpaceInfo();
		ConsumedToken token = consume(tokens);
		nsi.setPersonal(token.ns);
		token = consume(token.remaining);
		nsi.setOtherUsers(token.ns);
		token = consume(token.remaining);
		nsi.setMailShares(token.ns);

		return nsi;
	}

	/**
	 * @param tokens
	 * @return
	 */
	private static ConsumedToken consume(String tokens) {
		// System.out.println("Consume: '" + tokens + "'");
		ConsumedToken ct = new ConsumedToken();
		if (tokens.startsWith("NIL")) {
			ct.remaining = tokens.substring(3);
		} else {
			// (("Autres utilisateurs/" "/"))
			int end = tokens.indexOf("\"))");
			String list = tokens.substring(2, end + 1);
			// "Autres utilisateurs/" "/"
			int split = list.indexOf("\" \"");
			// System.out.println("List: '" + list + "', split: " + split
			// + ", end: " + end);
			String firstString = list.substring(1, split);
			// String secondString = list.substring(split + 3, list.length() -
			// 1);
			ct.ns = ImmutableList.of(UTF7Converter.decode(firstString));
			ct.remaining = tokens.substring(end + 3);
			// System.out.println("F: '" + firstString + "', S: '" +
			// secondString
			// + "'");
		}
		if (ct.remaining.startsWith(" ")) {
			ct.remaining = ct.remaining.trim();
		}
		// System.out.println("Remaining: '" + ct.remaining + "'");
		return ct;
	}
}
