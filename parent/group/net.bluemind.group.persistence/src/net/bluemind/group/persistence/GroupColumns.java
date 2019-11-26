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
package net.bluemind.group.persistence;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.group.api.Group;

public final class GroupColumns {

	public static final Columns cols = Columns.create() //
			.col("name") //
			.col("description") //
			.col("hidden") //
			.col("hiddenMembers") //
			.col("mailArchived") //
			.col("server_id") //
			.col("properties");

	/**
	 * @return
	 */
	public static GroupStore.StatementValues<Group> statementValues(Item groupItem, Container containerItem) {
		return (con, statement, index, currentRow, value) -> {
			statement.setString(index++, value.name);
			statement.setString(index++, value.description);
			statement.setBoolean(index++, value.hidden);
			statement.setBoolean(index++, value.hiddenMembers);
			statement.setBoolean(index++, value.mailArchived);
			statement.setString(index++, value.dataLocation);
			statement.setObject(index++, value.properties);
			statement.setLong(index++, groupItem.id);
			statement.setLong(index++, containerItem.id);
			return index;
		};
	}

	@SuppressWarnings("unchecked")
	public static GroupStore.EntityPopulator<Group> populator() {
		return (rs, index, value) -> {
			value.name = rs.getString(index++);
			value.description = rs.getString(index++);
			value.hidden = rs.getBoolean(index++);
			value.hiddenMembers = rs.getBoolean(index++);
			value.mailArchived = rs.getBoolean(index++);
			value.dataLocation = rs.getString(index++);
			value.properties = new HashMap<String, String>();
			Object properties = rs.getObject(index++);
			if (properties != null) {
				value.properties.putAll((Map<String, String>) properties);
			}
			value.memberCount = rs.getInt(index++);
			return index;
		};
	}
}
