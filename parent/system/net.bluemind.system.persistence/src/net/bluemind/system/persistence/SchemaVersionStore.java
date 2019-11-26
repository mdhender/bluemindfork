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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.system.schemaupgrader.ComponentVersion;

public final class SchemaVersionStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(SchemaVersionStore.class);
	private static final Creator<SchemaVersion> SCHEMAVERSION_CREATOR = new Creator<SchemaVersion>() {

		@Override
		public SchemaVersion create(ResultSet con) throws SQLException {
			return new SchemaVersion();
		}
	};

	/**
	 * @param dataSource
	 */
	public SchemaVersionStore(DataSource dataSource) {
		super(dataSource);
	}

	private static final String INSERT = "INSERT INTO t_upgraders (" + SchemaVersionColumns.cols.names() + ") " //
			+ " VALUES" //
			+ "( " + SchemaVersionColumns.cols.values() + ")";

	public void add(SchemaVersion value) {
		try {
			insert(INSERT, value, SchemaVersionColumns.statementValues());
		} catch (SQLException e) {
			logger.warn("Cannot add upgrader status entry", e);
		}
		logger.debug("insert complete: {}", value);
	}

	private static final String SELECT = "SELECT " + SchemaVersionColumns.cols.names() + " FROM t_upgraders " //
			+ " WHERE schemaversion >= ?";

	public List<SchemaVersion> get(int major, int build) {
		try {
			List<SchemaVersion> s = select(SELECT, SCHEMAVERSION_CREATOR, SchemaVersionColumns.populator(),
					new Object[] { new SchemaVersion(major, build).toDbSchemaVersion() });
			Collections.sort(s);
			return s;
		} catch (SQLException e) {
			logger.warn("Cannot read upgrader status entries", e);
			return Collections.emptyList();
		}
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
}
