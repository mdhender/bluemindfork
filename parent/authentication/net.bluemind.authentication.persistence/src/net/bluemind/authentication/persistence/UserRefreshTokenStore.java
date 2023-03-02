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
package net.bluemind.authentication.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import javax.sql.DataSource;

import net.bluemind.authentication.api.RefreshToken;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class UserRefreshTokenStore extends JdbcAbstractStore {

	private String userUid;

	private static final EntityPopulator<RefreshToken> TOKEN_POPULATOR = new EntityPopulator<>() {
		@Override
		public int populate(ResultSet rs, int index, RefreshToken value) throws SQLException {
			value.systemIdentifier = rs.getString(index++);
			value.token = rs.getString(index++);
			Timestamp timestamp = rs.getTimestamp(index++);
			if (timestamp != null) {
				value.expiryTime = new Date(timestamp.toInstant().toEpochMilli());
			}
			return index;
		}
	};

	public UserRefreshTokenStore(DataSource dataSource, String userUid) {
		super(dataSource);
		this.userUid = userUid;
	}

	public void add(RefreshToken refreshToken) throws ServerFault {
		delete(refreshToken.systemIdentifier);
		String query = "INSERT INTO t_user_refreshtoken (system_identifier, token, expiry_time, user_uid) VALUES (?, ?, ?, ?)";
		doOrFail(() -> {
			insert(query, refreshToken, (con, statement, index, rowIndex, value) -> {

				statement.setString(index++, value.systemIdentifier);
				statement.setString(index++, value.token);
				if (value.expiryTime != null) {
					statement.setTimestamp(index++, new Timestamp(value.expiryTime.getTime()));
				} else {
					statement.setTimestamp(index++, null);
				}
				statement.setString(index++, userUid);
				return index;

			});
			return null;
		});
	}

	public void delete(String systemIdentifier) {
		doOrFail(() -> {
			delete("DELETE FROM t_user_refreshtoken where system_identifier = ? and user_uid = ?",
					new Object[] { systemIdentifier, userUid });
			return null;
		});
	}

	public void deleteAll() {
		doOrFail(() -> {
			delete("DELETE FROM t_user_refreshtoken where user_uid = ?", new Object[] { userUid });
			return null;
		});
	}

	public RefreshToken get(String systemIdentifier) {
		try {
			String query = "SELECT system_identifier, token, expiry_time FROM t_user_refreshtoken WHERE system_identifier = ? and user_uid = ?";

			RefreshToken token = unique(query, (rs) -> new RefreshToken(),
					Arrays.<EntityPopulator<RefreshToken>>asList(TOKEN_POPULATOR),
					new Object[] { systemIdentifier, userUid });
			if (token != null) {
				if (token.expiryTime == null || token.expiryTime.after(new Date())) {
					return token;
				} else {
					delete(systemIdentifier);
					return null;
				}
			}
			return token;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
