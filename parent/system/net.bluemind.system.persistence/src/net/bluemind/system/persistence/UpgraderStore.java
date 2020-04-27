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

package net.bluemind.system.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.persistence.BooleanCreator;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.system.api.Database;
import net.bluemind.system.schemaupgrader.ComponentVersion;

public final class UpgraderStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(UpgraderStore.class);

	/**
	 * @param dataSource
	 */
	public UpgraderStore(DataSource dataSource) {
		super(dataSource);
	}

	private static final String INSERT = "INSERT INTO t_bm_upgraders (" + UpgraderColumns.cols.names() + ") " //
			+ " VALUES" //
			+ "( " + UpgraderColumns.cols.values() + ") ON CONFLICT DO NOTHING";

	private static final String UPDATE = "UPDATE t_upgraders set success = ? WHERE upgrader_id = ?";

	public void add(Upgrader value) {
		try {
			insert(INSERT, value, UpgraderColumns.statementValues());
		} catch (SQLException e) {
			logger.warn("Cannot add upgrader status entry", e);
		}
		logger.debug("insert complete: {}", value);
	}

	public void update(String upgraderId, boolean success) {
		try {
			update(UPDATE, new Object[] { success, upgraderId });
		} catch (SQLException e) {
			logger.warn("Cannot add upgrader status entry", e);
		}
	}

	private static final String SELECT = "SELECT 1 FROM t_bm_upgraders WHERE upgrader_id = ? AND server = ? AND database_name = ?::enum_database_name AND success = true";

	public boolean upgraderCompleted(String upgraderId, String server, Database database) throws SQLException {
		Boolean found = unique(SELECT, rs -> Boolean.TRUE, Collections.emptyList(),
				new Object[] { upgraderId, server, database.name() });
		return found != null;
	}

	public List<ComponentVersion> getComponentsVersion() throws SQLException {
		return select("SELECT component, version FROM t_component_version", rs -> new ComponentVersion(),
				(ResultSet rs, int index, ComponentVersion value) -> {
					value.identifier = rs.getString(index++);
					value.version = rs.getString(index++);
					return index;
				});
	}

	public void updateComponentVersion(String component, String version) throws SQLException {
		insert("INSERT INTO t_component_version (component,version) VALUES (?,?) ON CONFLICT (component) DO UPDATE SET version = ? WHERE t_component_version.component = excluded.component ",
				new Object[] { component, version, version });
	}

	public boolean needsMigration() throws SQLException {
		String sql = "select exists (select from pg_tables where tablename = 't_bm_upgraders')";
		boolean foundUpgraderTable = unique(sql, BooleanCreator.FIRST, Collections.emptyList(), new Object[0]);
		if (!foundUpgraderTable) {
			String createEnum = "create type enum_database_name as enum ('DIRECTORY', 'SHARD', 'ALL')";
			String createTable = "create table t_bm_upgraders (server text, " //
					+ "phase enum_upgrader_phase, " //
					+ "database_name enum_database_name, " //
					+ "upgrader_id text, " //
					+ "success boolean, " //
					+ "PRIMARY KEY (server, database_name, upgrader_id))";
			try (Connection con = datasource.getConnection(); Statement st = con.createStatement()) {
				st.execute(createEnum);
				st.execute(createTable);
			}

		}
		return !foundUpgraderTable;
	}

}
