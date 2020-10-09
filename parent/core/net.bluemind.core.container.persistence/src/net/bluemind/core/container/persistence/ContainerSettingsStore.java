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
package net.bluemind.core.container.persistence;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ContainerSettingsStore extends JdbcAbstractStore {

	private static final class MapHolder {
		Map<String, String> value;
	}

	private static final Creator<MapHolder> mapCreator = con -> new MapHolder();
	@SuppressWarnings("unchecked")
	private static final EntityPopulator<MapHolder> mapPopulator = (rs, index, map) -> {
		map.value = (Map<String, String>) rs.getObject(index++);
		return index;
	};

	private static final StatementValues<Map<String, String>> statementValues = (con, statement, index, currentRow,
			value) -> {
		statement.setObject(index++, value);
		return index;
	};
	private Container container;

	public ContainerSettingsStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	public Map<String, String> getSettings() throws SQLException {
		String query = "SELECT settings FROM t_container_settings WHERE container_id = ?";
		MapHolder res = unique(query, mapCreator, mapPopulator, container.id);
		return res != null ? res.value : null;
	}

	public void setSettings(Map<String, String> settings) throws SQLException {
		String query = "UPDATE t_container_settings set settings = ? where container_id = ?";

		update(query, settings, statementValues, new Object[] { container.id });
	}

	public void mutateSettings(Map<String, String> mutatedValues) throws SQLException {
		String query = "UPDATE t_container_settings set settings = settings || ? where container_id = ?";

		update(query, mutatedValues, statementValues, new Object[] { container.id });
	}

	public void delete() throws SQLException {
		delete("DELETE FROM t_container_settings WHERE container_id = ?", new Object[] { container.id });
	}

}
