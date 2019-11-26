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
package net.bluemind.tag.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.tag.api.Tag;

public class TagColumns {

	public static Columns COLUMNS = Columns.create() //
			.col("label") //
			.col("color");

	public static TagStore.StatementValues<Tag> values() {
		return new TagStore.StatementValues<Tag>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow, Tag value)
					throws SQLException {

				statement.setString(index++, value.label);
				statement.setString(index++, value.color);
				return index;

			}

		};

	}

	public static TagStore.EntityPopulator<Tag> populator() {
		return new TagStore.EntityPopulator<Tag>() {

			@Override
			public int populate(ResultSet rs, int index, Tag value) throws SQLException {

				value.label = rs.getString(index++);
				value.color = rs.getString(index++);
				return index;
			}

		};

	}

}