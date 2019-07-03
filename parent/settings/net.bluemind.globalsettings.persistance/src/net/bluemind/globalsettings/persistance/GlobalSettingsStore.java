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
package net.bluemind.globalsettings.persistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.jdbc.JdbcAbstractStore;

public class GlobalSettingsStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(GlobalSettingsStore.class);

	private static final Creator<Map<String, String>> GLOBAL_CREATOR = new Creator<Map<String, String>>() {
		@Override
		public Map<String, String> create(ResultSet con) throws SQLException {
			return new HashMap<String, String>();
		}
	};

	public GlobalSettingsStore(DataSource pool) {
		super(pool);
	}

	public Map<String, String> get() throws SQLException {

		String query = "SELECT " + GlobalSettingsColumns.COLUMNS.names() + " FROM t_settings_global";

		Map<String, String> settings = unique(query.toString(), GLOBAL_CREATOR, GlobalSettingsColumns.populator());

		logger.debug("retrieve global settings {}", settings);
		return settings;
	}

	public void set(Map<String, String> values) throws SQLException {
		String query = "UPDATE t_settings_global SET ( " + GlobalSettingsColumns.COLUMNS.names() + ") = ROW("
				+ GlobalSettingsColumns.COLUMNS.values() + ")";

		update(query, values, GlobalSettingsColumns.statementValues(), new Object[] {});
	}

}
