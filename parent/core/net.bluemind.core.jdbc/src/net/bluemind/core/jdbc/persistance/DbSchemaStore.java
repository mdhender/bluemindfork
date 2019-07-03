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
package net.bluemind.core.jdbc.persistance;

import static net.bluemind.core.jdbc.JdbcHelper.cleanup;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.core.jdbc.JdbcException;
import net.bluemind.core.jdbc.SchemaDescriptor;

public class DbSchemaStore {
	private static final Logger logger = LoggerFactory.getLogger(DbSchemaStore.class);

	private DataSource ds;

	public DbSchemaStore(DataSource pool) {
		this.ds = pool;
	}

	public String getSchemaVersion(String name) {
		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			con = ds.getConnection();
			st = con.prepareStatement("SELECT version FROM schema_version where name = ?");
			st.setString(1, name);
			rs = st.executeQuery();
			if (rs.next()) {
				String ret = rs.getString(1);
				if (rs.next()) {
					logger.warn("schema {} have multiple versions !", name);
				}
				return ret;
			} else {
				return null;
			}
		} catch (SQLException e) {
			logger.debug(e.getMessage());
			// throw new JdbcException(e);
			return null;
		} finally {
			cleanup(con, rs, st);
		}
	}

	public void createSchema(SchemaDescriptor schema, boolean walEnabled) {
		String schemaValue = null;
		try (InputStream in = schema.read()) {
			byte[] b = ByteStreams.toByteArray(in);
			schemaValue = new String(b);
		} catch (IOException e) {
			logger.error("error during schema reading {}-{}", schema.getName(), schema.getVersion());
			throw new RuntimeException(e);
		}

		if (!walEnabled) {
			schemaValue = schemaValue.replace("create table", "create unlogged table");
			schemaValue = schemaValue.replace("CREATE TABLE", "CREATE UNLOGGED TABLE");
		}

		Connection con = null;
		Statement st = null;
		try {
			con = ds.getConnection();
			st = con.createStatement();
			st.execute(schemaValue);
		} catch (SQLException e) {
			logger.error("error during creation of schema " + schema.getId() + " " + e.getMessage());
			if (!schema.isIgnoreErrors()) {
				throw new JdbcException(e);
			}
		} finally {
			cleanup(con, null, st);
		}

		try {
			con = ds.getConnection();
			st = con.createStatement();
			st.executeUpdate(
					"INSERT INTO schema_version values ('" + schema.getName() + "', '" + schema.getVersion() + "')");

		} catch (SQLException e) {
			logger.error("error during creation of schema " + schema.getId() + " " + e.getMessage());
			throw new JdbcException(e);
		} finally {
			cleanup(con, null, st);
		}
	}

	public void createSchema(SchemaDescriptor descr) {
		createSchema(descr, true);
	}
}
