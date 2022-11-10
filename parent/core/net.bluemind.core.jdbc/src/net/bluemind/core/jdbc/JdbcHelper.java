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
package net.bluemind.core.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcHelper {
	private JdbcHelper() {
	}

	private static final Logger logger = LoggerFactory.getLogger(JdbcHelper.class);

	public static void cleanup(Connection con, ResultSet rs, Statement st) {
		try {
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
			logger.warn("error closing resultset", e);
		}
		try {
			if (st != null)
				st.close();
		} catch (SQLException e) {
			logger.warn("error closing statement", e);
		}
		try {
			if (con != null)
				con.close();
		} catch (SQLException e) {
			logger.warn("error closing connection", e);
		}

	}

	public static void enableAutoExplain(Connection con, int minimumDurationMillis) throws SQLException {
		try (Statement st = con.createStatement()) {
			st.execute("LOAD 'auto_explain'");
			st.execute("set auto_explain.log_min_duration TO " + minimumDurationMillis);
			st.execute("set auto_explain.log_level TO notice");
			st.execute("set auto_explain.log_analyze to on");
			st.execute("set auto_explain.log_verbose to on");
			st.execute("set auto_explain.log_triggers to on");
			st.execute("set auto_explain.log_nested_statements to on");
			st.execute("set auto_explain.log_timing to on");
		}
	}

	public static boolean tableExists(Connection con, String tableName) throws SQLException {
		try (PreparedStatement st = con
				.prepareStatement("SELECT 1 FROM information_schema.tables WHERE table_name = ?")) {
			st.setString(1, tableName);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}
}
