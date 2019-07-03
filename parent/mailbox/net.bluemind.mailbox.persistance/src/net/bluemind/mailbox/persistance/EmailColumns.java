/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 20122015
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
package net.bluemind.mailbox.persistance;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.Email;
import net.bluemind.mailbox.api.Mailbox;

public enum EmailColumns {
	left_address(null), //
	right_address(null), //
	all_aliases(null), //
	is_default(null);

	private final String enumType;

	private EmailColumns(String enumType) {
		this.enumType = enumType;
	}

	public static void appendNames(String prefix, StringBuilder query) {
		EmailColumns[] vals = EmailColumns.values();
		for (int i = 0; i < vals.length; i++) {
			if (i != 0) {
				query.append(", ");
			}

			if (prefix != null) {
				query.append(prefix).append(".");
			}
			query.append(vals[i].name());
		}
	}

	public static void appendValues(StringBuilder query) {
		EmailColumns[] vals = EmailColumns.values();
		for (int i = 0; i < vals.length; i++) {
			if (i != 0) {
				query.append(',');
			}

			query.append("?");
			EmailColumns uc = vals[i];
			if (uc.enumType != null) {
				query.append("::").append(uc.enumType);
			}
		}
	}

	public static MailboxStore.StatementValues<Email> statementValues(final long itemId) {
		return new MailboxStore.StatementValues<Email>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, Email value)
					throws SQLException {
				statement.setLong(index++, itemId);
				String[] addr = value.address.split("@");
				statement.setString(index++, addr[0]);
				statement.setString(index++, addr[1]);
				statement.setBoolean(index++, value.allAliases);
				statement.setBoolean(index++, value.isDefault);
				return index;
			}
		};
	}

	public static MailboxStore.EntityPopulator<Email> populator(String domain) {
		return new MailboxStore.EntityPopulator<Email>() {

			@Override
			public int populate(ResultSet rs, int index, Email value) throws SQLException {
				String left = rs.getString(index++);
				String right = rs.getString(index++);

				value.allAliases = rs.getBoolean(index++);
				value.isDefault = rs.getBoolean(index++);
				value.address = left + "@" + right;
				return index;
			}

		};
	}

	public static MailboxStore.EntityPopulator<Mailbox> aggPopulator(String domain) {
		return new MailboxStore.EntityPopulator<Mailbox>() {

			@Override
			public int populate(ResultSet rs, int index, Mailbox value) throws SQLException {
				Array left = rs.getArray(index++);
				Array right = rs.getArray(index++);
				Array allAliases = rs.getArray(index++);
				Array isDefault = rs.getArray(index++);

				if (left == null) {
					value.emails = Collections.emptyList();
					return index;
				}

				String[] l = (String[]) left.getArray();
				String[] r = (String[]) right.getArray();
				Boolean[] a = (Boolean[]) allAliases.getArray();
				Boolean[] id = (Boolean[]) isDefault.getArray();
				List<Email> ret = new ArrayList<>(l.length);
				for (int i = 0; i < l.length; i++) {
					ret.add(Email.create(l[i] + "@" + r[i], id[i], a[i]));
				}
				value.emails = ret;
				return index;
			}

		};
	}

}
