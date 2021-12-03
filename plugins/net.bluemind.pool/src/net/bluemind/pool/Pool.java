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
package net.bluemind.pool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import net.bluemind.pool.impl.WrappedDS;

public final class Pool {

	private static final Logger logger = LoggerFactory.getLogger(Pool.class);

	private final String lastInsertIdQuery;
	private final WrappedDS wrapped;

	Pool(String lastIdQuery, HikariDataSource xa) {
		this.wrapped = new WrappedDS(xa);
		this.lastInsertIdQuery = lastIdQuery;
	}

	public void stop() {
		wrapped.close();
	}

	public Connection getConnection() {
		Connection con = null;
		try {
			con = wrapped.getConnection();
		} catch (Exception e) {
			logger.error("Error getting SQL connection to database", e);
		}
		return con;
	}

	public int lastInsertId(Connection con) throws SQLException {
		int ret = 0;
		Statement st = null;
		ResultSet rs = null;
		try {
			st = con.createStatement();
			rs = st.executeQuery(lastInsertIdQuery);
			if (rs.next()) {
				ret = rs.getInt(1);
			}
		} finally {
			BMPoolActivator.cleanup(null, st, rs);
		}
		return ret;
	}

	public CloseableDataSource getDataSource() {
		return wrapped;
	}

	@Override
	protected void finalize() throws Throwable {
		if (!wrapped.isClosed()) {
			wrapped.close();
		}
		super.finalize();
	}

}
