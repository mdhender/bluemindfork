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
package net.bluemind.system.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public enum SystemConfColumns {

	conf;

	public static void appendNames(String prefix, StringBuilder query) {
		boolean first = true;
		SystemConfColumns[] vals = SystemConfColumns.values();
		for (int i = 0; i < vals.length; i++) {
			if (!first) {
				query.append(", ");
			}

			if (prefix != null) {
				query.append(prefix).append(".");
			}
			query.append(vals[i].name());
			first = false;
		}

	}

	public static void appendValues(StringBuilder query) {
		boolean first = true;
		SystemConfColumns[] vals = SystemConfColumns.values();
		for (int i = 0; i < vals.length; i++) {
			if (!first) {
				query.append(',');
			}

			query.append("?");
		}
	}

	/**
	 * @return
	 */
	public static SystemConfStore.StatementValues<Map<String, String>> statementValues() {
		return new SystemConfStore.StatementValues<Map<String, String>>() {
			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					Map<String, String> settings) throws SQLException {
				statement.setObject(index++, settings);
				return index;
			}
		};
	}

	public static SystemConfStore.EntityPopulator<Map<String, String>> populator() {
		return new SystemConfStore.EntityPopulator<Map<String, String>>() {

			@SuppressWarnings("unchecked")
			@Override
			public int populate(ResultSet rs, int index, Map<String, String> value) throws SQLException {
				value.putAll((Map<String, String>) rs.getObject(index++));

				return index;
			}
		};
	}
}
