/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.driver;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public class MailPart {

	public final String name;
	public final String section;
	public final Set<String> options;
	public final String partial;
	private final String toString;
	private final byte[] outName;

	public MailPart(String name, String section, Set<String> options, String partial) {
		this.name = name;
		this.section = section;
		this.options = options;
		this.partial = partial;
		this.toString = toStringImpl();
		this.outName = (" " + toString + " ").replace(".peek", "").replace(".PEEK", "")
				.getBytes(StandardCharsets.US_ASCII);
	}

	private String toStringImpl() {
		return name + (section != null ? "[" + section + optStr(options) + "]" : "")
				+ (partial != null ? "<" + partial + ">" : "");
	}

	private String optStr(Set<String> options) {
		if (options == null || options.isEmpty()) {
			return "";
		}
		return options.stream().collect(Collectors.joining(" ", " (", ")"));
	}

	@Override
	public String toString() {
		return toString;
	}

	public byte[] outName() {
		return outName;
	}

}
