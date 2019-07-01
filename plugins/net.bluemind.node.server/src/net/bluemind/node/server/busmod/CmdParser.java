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
package net.bluemind.node.server.busmod;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Splitter;

public final class CmdParser {

	private static final String[] fixArguments(Collection<String> args) {
		List<String> as = new LinkedList<String>();

		String cur = null;
		boolean apoFound = false;

		for (String a : args) {
			if (apoFound) {
				cur += " " + a.replace("'", "");
			} else {
				cur = a.replace("'", "");
			}
			if (a.startsWith("'")) {
				apoFound = true;
			}
			if (a.endsWith("'")) {
				apoFound = false;
			}
			if (!apoFound) {
				as.add(cur);
			}
		}

		return as.toArray(new String[0]);
	}

	public static final String[] args(String cmd) {
		List<String> spaceSplit = Splitter.on(' ').splitToList(cmd);
		return fixArguments(spaceSplit);
	}

}
