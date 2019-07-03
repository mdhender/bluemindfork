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
package net.bluemind.imap.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import net.bluemind.imap.SearchQuery;

public class UIDSearchCommand extends AbstractUIDSearchCommand {

	// Mon, 7 Feb 1994 21:52:25 -0800

	private SearchQuery sq;

	public UIDSearchCommand(SearchQuery sq) {
		this.sq = sq;
	}

	@Override
	protected CommandArgument buildCommand() {
		String cmd = "UID SEARCH NOT DELETED";
		if (sq.isUnseenOnly()) {
			cmd += " UNSEEN";
		}
		if (sq.isUseOr()) {
			cmd += " OR";
		}
		if (sq.getTo() != null) {
			cmd += " TO \"" + sq.getTo() + "\"";
		}
		if (sq.getFrom() != null) {
			cmd += " FROM \"" + sq.getFrom() + "\"";
		}
		if (sq.getSubject() != null) {
			cmd += " SUBJECT \"" + sq.getSubject() + "\"";
		}
		if (sq.getBody() != null) {
			cmd += " BODY \"" + sq.getBody() + "\"";
		}
		if (sq.getAfter() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT BEFORE " + df.format(sq.getAfter());
		}
		if (sq.getBefore() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " BEFORE " + df.format(sq.getBefore());
		}

		if (sq.getKeyword() != null) {
			cmd += " KEYWORD " + sq.getKeyword();

		}

		Map<String, String> heads = sq.getHeaders();
		if (!heads.isEmpty()) {
			for (Entry<String, String> s : heads.entrySet()) {
				cmd += " HEADER " + s.getKey() + " \"" + s.getValue() + "\"";
			}
		}

		return new CommandArgument(cmd, null);
	}

}
