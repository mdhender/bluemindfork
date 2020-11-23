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
package net.bluemind.domain.persistence;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.domain.api.Domain;

public class DomainColumns {

	public static final Columns cols = Columns.create() //
			.col("label")//
			.col("name") //
			.col("description")//
			.col("global") //
			.col("aliases") //
			.col("default_alias") //
			.col("properties");

	/**
	 * @return
	 */
	public static DomainStore.StatementValues<Domain> statementValues(final long itemId) {
		return (con, statement, index, currentRow, domain) -> {
			statement.setString(index++, domain.label);
			statement.setString(index++, domain.name);
			statement.setString(index++, domain.description);
			statement.setBoolean(index++, domain.global);
			statement.setArray(index++, con.createArrayOf("text", domain.aliases.toArray(new String[0])));
			statement.setString(index++, domain.defaultAlias);
			statement.setObject(index++, domain.properties);
			statement.setLong(index++, itemId);
			return index;
		};
	}

	public static DomainStore.EntityPopulator<Domain> populator() {
		return (rs, index, domain) -> {
			domain.label = rs.getString(index++);
			domain.name = rs.getString(index++);
			domain.description = rs.getString(index++);
			domain.global = rs.getBoolean(index++);
			domain.aliases = new HashSet<>(Arrays.asList(arrayOfString(rs.getArray(index++))));
			domain.defaultAlias = rs.getString(index++);
			domain.properties = new HashMap<String, String>();
			Object properties = rs.getObject(index++);
			if (properties != null) {
				domain.properties.putAll((Map<String, String>) properties);
			}
			return index;
		};
	}

	protected static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];
		}
		return ret;
	}

	public static DomainStore.Creator<Domain> creator() {
		return new DomainStore.Creator<Domain>() {

			@Override
			public Domain create(ResultSet con) throws SQLException {
				return new Domain();
			}

		};
	}
}
