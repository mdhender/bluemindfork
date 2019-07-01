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
package net.bluemind.im.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcHelper;

public class IMStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(IMStore.class);

	public IMStore(DataSource dataSource) {
		super(dataSource);
	}

	public void setRoster(String jabberId, String data) throws ServerFault {
		doOrFail(() -> {

			String query = "UPDATE t_im_user_repo SET data = ? WHERE id = ?";

			Connection conn = null;
			PreparedStatement st = null;
			try {
				conn = getConnection();
				st = conn.prepareStatement(query);
				int index = 1;
				st.setString(index++, data);
				st.setString(index++, jabberId);
				logger.debug("setRoster: {}", st);
				int count = st.executeUpdate();

				if (count == 0) {
					st.close();
					st = null;
					query = "INSERT INTO t_im_user_repo (id, data) VALUES (?, ?)";
					st = conn.prepareStatement(query);
					index = 1;
					st.setString(index++, jabberId);
					st.setString(index++, data);
					logger.debug("setRoster: {}", st);
					st.executeUpdate();
				}
			} finally {
				JdbcHelper.cleanup(conn, null, st);
			}
			return null;
		});
	}

	public String getRoster(String jabberId) {
		String query = "SELECT data FROM t_im_user_repo WHERE id = ?";

		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		String ret = null;
		try {
			conn = getConnection();
			st = conn.prepareStatement(query);
			int index = 1;
			st.setString(index++, jabberId);
			logger.debug("getRoster: {}", st);

			rs = st.executeQuery();

			if (rs.next()) {
				ret = rs.getString(1);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);

		} finally {
			JdbcHelper.cleanup(conn, rs, st);
		}

		return ret;
	}

}
