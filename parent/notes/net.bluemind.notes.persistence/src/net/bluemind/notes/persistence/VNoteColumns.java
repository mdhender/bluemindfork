/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNote.Color;

public class VNoteColumns {
	public static final Columns cols = Columns.create().col("subject") //
			.col("body") //
			.col("color", "t_notes_vnote_color") //
			.col("posX") //
			.col("posY") //
			.col("height") //
			.col("width");

	public static VNoteStore.StatementValues<VNote> values(final Item item) {
		return new VNoteStore.StatementValues<VNote>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow, VNote value)
					throws SQLException {
				statement.setString(index++, value.subject);
				statement.setString(index++, value.body);
				statement.setString(index++, value.color.name());

				if (value.posX != null) {
					statement.setInt(index++, value.posX);
				}
				if (value.posY != null) {
					statement.setInt(index++, value.posY);
				}
				if (value.height != null) {
					statement.setInt(index++, value.height);
				}
				if (value.width != null) {
					statement.setInt(index++, value.width);
				}

				statement.setLong(index++, item.id);
				return index;
			}
		};
	}

	public static VNoteStore.EntityPopulator<VNote> populator() {
		return new VNoteStore.EntityPopulator<VNote>() {

			@Override
			public int populate(ResultSet rs, int index, VNote value) throws SQLException {
				value.subject = rs.getString(index++);
				value.body = rs.getString(index++);
				value.color = Color.valueOf(rs.getString(index++));
				value.posX = Integer.parseInt(rs.getString(index++));
				value.posY = Integer.parseInt(rs.getString(index++));
				value.height = Integer.parseInt(rs.getString(index++));
				value.width = Integer.parseInt(rs.getString(index++));

				return index;
			}

		};

	}

}
