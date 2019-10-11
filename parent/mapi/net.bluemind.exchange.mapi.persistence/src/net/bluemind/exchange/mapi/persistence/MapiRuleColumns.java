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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.exchange.mapi.api.MapiRule;

public class MapiRuleColumns {

	public static final Columns cols = Columns.create()//
			.col("folder_id")//
			.col("rule_id")//
			.col("blob");

	public static StatementValues<MapiRule> values(String folderId) {
		return new StatementValues<MapiRule>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, MapiRule value)
					throws SQLException {
				statement.setString(index++, folderId);
				statement.setLong(index++, value.ruleId);
				statement.setString(index++, value.ruleBase64);
				return index;
			}
		};
	}

	public static EntityPopulator<MapiRule> populator() {
		return (ResultSet rs, int index, MapiRule value) -> {
			index++;// skip folderId
			value.ruleId = rs.getLong(index++);
			value.ruleBase64 = rs.getString(index++);
			return index;
		};
	}

}
