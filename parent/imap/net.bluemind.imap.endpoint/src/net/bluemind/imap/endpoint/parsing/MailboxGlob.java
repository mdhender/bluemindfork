/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.imap.endpoint.parsing;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

public class MailboxGlob {

	private static final Logger logger = LoggerFactory.getLogger(MailboxGlob.class);

	private MailboxGlob() {

	}

	private static final Predicate<String> NO_SLASH = noSlash();
	private static final Predicate<String> MATCH_ALL = s -> true;

	// https://www.baeldung.com/java-split-string-keep-delimiters
	private static final Splitter SPLIT_GLOB = Splitter.onPattern("((?=[%\\*])|(?<=[%\\*]))");

	public static Predicate<String> matcher(String mailboxPattern) {
		switch (mailboxPattern) {
		case "%":
			return NO_SLASH;
		case "*":
			return MATCH_ALL;
		default:
			return mixedPattern(mailboxPattern);
		}

	}

	private static Predicate<String> mixedPattern(String mailboxPattern) {
		String asRegExp = SPLIT_GLOB.splitToStream(mailboxPattern).map(s -> {
			switch (s) {
			case "%":
				return "[^/]+";
			case "*":
				return ".*$";
			default:
				return Pattern.quote(s);
			}
		}).collect(Collectors.joining("", "^", ""));
		logger.debug("mailboxPattern {} becomes regexp {}", mailboxPattern, asRegExp);
		Pattern compiled = Pattern.compile(asRegExp);
		return s -> compiled.matcher(s).find();
	}

	private static Predicate<String> noSlash() {
		CharMatcher match = CharMatcher.is('/');
		return match::matchesNoneOf;
	}

}
