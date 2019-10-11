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
package net.bluemind.exchange.mapi.persistence;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.exchange.mapi.api.MapiRule;

public class MapiRuleStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(MapiRuleStore.class);
	public final String folderId;

	public MapiRuleStore(DataSource dataSource, String folderId) {
		super(dataSource);
		this.folderId = folderId;
		logger.debug("RuleStore for ds {}", dataSource);
	}

	/**
	 * Upsert the given {@link MapiRule}
	 * 
	 * @param value the rule to upsert
	 * @throws SQLException
	 */
	public void store(MapiRule value) throws SQLException {
		String query = "INSERT INTO t_mapi_rule (" + MapiRuleColumns.cols.names() + ") VALUES ("
				+ MapiRuleColumns.cols.values() + ") ON CONFLICT (folder_id, rule_id) DO UPDATE SET ("
				+ MapiRuleColumns.cols.names() + ")=(" + MapiRuleColumns.cols.values() + ")";
		insert(query, value, Arrays.asList(MapiRuleColumns.values(folderId), MapiRuleColumns.values(folderId)));
	}

	public void delete(long ruleId) throws SQLException {
		delete("DELETE FROM t_mapi_rule WHERE folder_id = ? AND rule_id = ?", new Object[] { folderId, ruleId });
	}

	public List<MapiRule> all() throws SQLException {
		String query = "SELECT " + MapiRuleColumns.cols.names() + " FROM t_mapi_rule WHERE folder_id=?";
		return select(query, rs -> new MapiRule(), MapiRuleColumns.populator(), new Object[] { folderId });
	}

}
