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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import net.bluemind.pool.impl.WrappedDS;

public final class Pool {

	private static final Logger logger = LoggerFactory.getLogger(Pool.class);

	private final WrappedDS wrapped;

	Pool(HikariDataSource xa) {
		this.wrapped = new WrappedDS(xa);
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
