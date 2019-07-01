/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.pool.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariDataSource;

import net.bluemind.pool.CloseableDataSource;

public class WrappedDS implements CloseableDataSource {

	private final HikariDataSource ds;

	public WrappedDS(HikariDataSource ds) {
		this.ds = ds;
	}

	public int hashCode() {
		return ds.hashCode();
	}

	public Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof WrappedDS)) {
			return false;
		}
		WrappedDS casted = (WrappedDS) obj;
		return ds.equals(casted.ds);
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return ds.getConnection(username, password);
	}

	public PrintWriter getLogWriter() throws SQLException {
		return ds.getLogWriter();
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		ds.setLogWriter(out);
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		ds.setLoginTimeout(seconds);
	}

	public int getLoginTimeout() throws SQLException {
		return ds.getLoginTimeout();
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return ds.getParentLogger();
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return ds.unwrap(iface);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return ds.isWrapperFor(iface);
	}

	public void close() {
		ds.close();
	}

	public boolean isClosed() {
		return ds.isClosed();
	}

	public String toString() {
		return ds.toString();
	}

}
