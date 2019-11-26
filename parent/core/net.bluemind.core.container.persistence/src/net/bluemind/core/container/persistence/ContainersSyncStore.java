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
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ContainersSyncStore extends JdbcAbstractStore {

	public ContainersSyncStore(DataSource dataSource) {
		super(dataSource);
	}

	private static final String QUERY_LIST = "SELECT t_container.uid FROM t_container_sync " //
			+ "INNER JOIN t_container ON t_container.id = t_container_sync.container_id "//
			+ "join t_container_settings s on s.container_id = t_container.id and s.settings::hstore ?? ? "//
			+ "WHERE t_container.container_type = ? AND next_sync <= ? " //
			+ "order by next_sync LIMIT ?";

	public List<String> list(String containerType, Long before, int limit, String settingsKey) throws SQLException {
		return select(QUERY_LIST, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { settingsKey, containerType, new Timestamp(before), limit });
	}

}
