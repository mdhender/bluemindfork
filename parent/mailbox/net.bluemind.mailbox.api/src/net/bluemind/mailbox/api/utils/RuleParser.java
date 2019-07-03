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
package net.bluemind.mailbox.api.utils;

import net.bluemind.mailbox.api.MailFilter;

public class RuleParser {

	/**
	 * visit {@link #criteria}
	 * 
	 * @param handler
	 *            visitor
	 */
	public static void visit(MailFilter.Rule rule, RuleHandler handler) {
		if (rule.criteria == null || rule.criteria.isEmpty()) {
			return;
		}

		String[] crits = rule.criteria.split("\n");
		for (int i = 0; i < crits.length; i++) {
			String c = crits[i];
			parseCriterion(c, handler);
		}
	}

	private static void parseCriterion(String c, RuleHandler handler) {
		int i = c.indexOf(":");
		String crit = c.substring(0, i);
		c = c.substring(i + 1);
		i = c.indexOf(": ");
		String matchType = c.substring(0, i);
		String value = c.substring(i + 2);

		if (matchType.equals("EXISTS")) {
			handler.exists(crit);
			return;
		} else if (matchType.equals("DOESNOTEXIST")) {
			handler.doesnotExist(crit);
			return;
		} else if (matchType.equals("IS")) {
			handler.is(crit, value);
		} else if (matchType.equals("ISNOT")) {
			handler.isNot(crit, value);
		} else if (matchType.equals("CONTAINS")) {
			handler.contains(crit, value);
		} else if (matchType.equals("DOESNOTCONTAIN")) {
			handler.doesnotContain(crit, value);
		} else if (matchType.equals("MATCHES")) {
			handler.matches(crit, value);
		} else if (matchType.equals("DOESNOTMATCH")) {
			handler.doesnotMatch(crit, value);
		}

	}
}
