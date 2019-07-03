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
package net.bluemind.user.accounts.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.user.api.UserAccount;
import net.bluemind.user.api.UserAccountInfo;

public class UserAccountColumns {

	public static final Columns cols = Columns.create() //
			.col("login") //
			.col("credentials") //
			.col("properties");

	public static final Columns infoCols = Columns.create() //
			.col("login") //
			.col("credentials") //
			.col("properties") //
			.col("system");

	/**
	 * @return
	 */
	public static UserAccountsStore.StatementValues<UserAccount> statementValues() {
		return new UserAccountsStore.StatementValues<UserAccount>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, UserAccount u)
					throws SQLException {
				statement.setString(index++, u.login);
				statement.setString(index++, u.credentials);
				statement.setObject(index++, u.additionalSettings);

				return index;
			}
		};
	}

	public static UserAccountsStore.EntityPopulator<UserAccount> populator() {
		return new UserAccountsStore.EntityPopulator<UserAccount>() {

			@SuppressWarnings("unchecked")
			@Override
			public int populate(ResultSet rs, int index, UserAccount value) throws SQLException {
				value.login = rs.getString(index++);
				value.credentials = rs.getString(index++);
				value.additionalSettings = new HashMap<String, String>();
				Object properties = rs.getObject(index++);
				if (properties != null) {
					value.additionalSettings.putAll((Map<String, String>) properties);
				}

				return index;
			}

		};
	}

	public static UserAccountsStore.EntityPopulator<UserAccountInfo> infoPopulator() {
		return new UserAccountsStore.EntityPopulator<UserAccountInfo>() {

			@SuppressWarnings("unchecked")
			@Override
			public int populate(ResultSet rs, int index, UserAccountInfo value) throws SQLException {
				value.login = rs.getString(index++);
				value.credentials = rs.getString(index++);
				value.additionalSettings = new HashMap<String, String>();
				Object properties = rs.getObject(index++);
				if (properties != null) {
					value.additionalSettings.putAll((Map<String, String>) properties);
				}
				value.externalSystemId = rs.getString(index++);

				return index;
			}

		};
	}
}
