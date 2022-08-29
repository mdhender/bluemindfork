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
package net.bluemind.user.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.User;

public class UserColumns {

	public static final Columns cols = Columns.create() //
			.col("login") //
			.col("password") //
			.col("password_lastchange") //
			.col("password_mustchange") //
			.col("password_neverexpires") //
			.col("routing", "t_domain_routing") //
			.col("hidden") //
			.col("archived") //
			.col("system") //
			.col("server_id") //
			.col("properties") //
			.col("mailbox_copy_guid"); //

	/**
	 * @return
	 */
	public static UserStore.StatementValues<User> statementValues() {
		return new UserStore.StatementValues<User>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, User u)
					throws SQLException {
				statement.setString(index++, u.login);
				statement.setString(index++, u.password);
				statement.setTimestamp(index++,
						u.passwordLastChange == null ? null : Timestamp.from(u.passwordLastChange.toInstant()));
				statement.setBoolean(index++, u.passwordMustChange);
				statement.setBoolean(index++, u.passwordNeverExpires);
				statement.setString(index++, u.routing.name());
				statement.setBoolean(index++, u.hidden);
				statement.setBoolean(index++, u.archived);
				statement.setBoolean(index++, u.system);
				if (u.dataLocation != null) {
					statement.setString(index++, u.dataLocation);
				} else {
					statement.setNull(index++, Types.VARCHAR);
				}
				statement.setObject(index++, u.properties);
				statement.setString(index++, u.mailboxCopyGuid);
				return index;
			}
		};
	}

	public static UserStore.EntityPopulator<User> populator() {
		return new UserStore.EntityPopulator<User>() {

			@SuppressWarnings("unchecked")
			@Override
			public int populate(ResultSet rs, int index, User value) throws SQLException {
				value.login = rs.getString(index++);
				value.password = rs.getString(index++);

				Timestamp passwordLastChange = rs.getTimestamp(index++);
				value.passwordLastChange = passwordLastChange == null ? null
						: Date.from(passwordLastChange.toInstant());

				value.passwordMustChange = rs.getBoolean(index++);
				value.passwordNeverExpires = rs.getBoolean(index++);
				value.routing = Mailbox.Routing.valueOf(rs.getString(index++));
				value.hidden = rs.getBoolean(index++);
				value.archived = rs.getBoolean(index++);
				value.system = rs.getBoolean(index++);
				value.dataLocation = rs.getString(index++);

				value.properties = new HashMap<String, String>();
				Object properties = rs.getObject(index++);
				if (properties != null) {
					value.properties.putAll((Map<String, String>) properties);
				}
				value.mailboxCopyGuid = rs.getString(index++);
				return index;
			}

		};
	}
}
