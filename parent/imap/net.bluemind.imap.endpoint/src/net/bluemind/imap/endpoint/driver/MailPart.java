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

import java.util.Set;
import java.util.stream.Collectors;

public class MailPart {

	public String name;
	public String section;
	public Set<String> options;
	public String partial;

	@Override
	public String toString() {
		return name + (section != null ? "[" + section + optStr(options) + "]" : "")
				+ (partial != null ? "<" + partial + ">" : "");
	}

	private String optStr(Set<String> options) {
		if (options == null || options.isEmpty()) {
			return "";
		}
		return options.stream().collect(Collectors.joining(" ", " (", ")"));
	}

	public String outName() {
		return toString().replace(".peek", "").replace(".PEEK", "");
	}

}
