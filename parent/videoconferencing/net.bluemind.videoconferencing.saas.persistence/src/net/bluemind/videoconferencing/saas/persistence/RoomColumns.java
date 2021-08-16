/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.saas.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.videoconferencing.saas.api.BlueMindVideoRoom;

public class RoomColumns {
	private RoomColumns() {
	}

	public static final Columns cols = Columns.create() //
			.col("identifier") //
			.col("owner") //
			.col("title");

	public static JdbcAbstractStore.StatementValues<BlueMindVideoRoom> values(Item it) {
		return (final Connection con, final PreparedStatement statement, int index, final int currentRow,
				final BlueMindVideoRoom value) -> {
			statement.setString(index++, value.identifier);
			statement.setString(index++, value.owner);
			statement.setString(index++, value.title);
			statement.setLong(index++, it.id);
			return index;
		};
	}

	public static JdbcAbstractStore.EntityPopulator<BlueMindVideoRoom> populator() {
		return (ResultSet rs, int index, BlueMindVideoRoom value) -> {
			value.identifier = rs.getString(index++);
			value.owner = rs.getString(index++);
			value.title = rs.getString(index++);
			return index;
		};
	}
}
