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
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.authentication.api.APIKey;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class APIKeyStore extends JdbcAbstractStore {

	private SecurityContext context;
	private static final EntityPopulator<APIKey> APIKEY_POPULATOR = new EntityPopulator<APIKey>() {
		@Override
		public int populate(ResultSet rs, int index, APIKey value) throws SQLException {
			value.sid = rs.getString(index++);
			value.displayName = rs.getString(index++);
			value.subject = rs.getString(index++);
			value.domainUid = rs.getString(index++);
			return index;
		}
	};

	public APIKeyStore(DataSource dataSource, SecurityContext context) {
		super(dataSource);
		this.context = context;
	}

	public void create(APIKey apikey) throws ServerFault {
		String query = "INSERT INTO t_api_key (sid, displayname, subject, domain_uid) VALUES (?, ?, ?, ?)";
		doOrFail(() -> {
			insert(query, apikey, (con, statement, index, rowIndex, value) -> {

				statement.setString(index++, value.sid);
				statement.setString(index++, value.displayName);
				statement.setString(index++, context.getSubject());
				statement.setString(index++, context.getContainerUid());
				return index;

			});
			return null;
		});
	}

	public void delete(String sid) throws ServerFault {
		doOrFail(() -> {
			delete("DELETE FROM t_api_key where sid = ? and subject = ? and domain_uid = ? ",
					new Object[] { sid, context.getSubject(), context.getContainerUid() });
			return null;
		});
	}

	public void deleteAll() throws ServerFault {
		doOrFail(() -> {
			delete("DELETE FROM t_api_key where subject = ? and domain_uid = ?",
					new Object[] { context.getSubject(), context.getContainerUid() });
			return null;
		});
	}

	public List<APIKey> list() throws ServerFault {
		try {
			String query = "SELECT sid, displayname, subject, domain_uid FROM t_api_key "
					+ " WHERE subject = ? and domain_uid = ?";

			return select(query, (rs) -> new APIKey(), Arrays.<EntityPopulator<APIKey>>asList(APIKEY_POPULATOR),
					new Object[] { context.getSubject(), context.getContainerUid() });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public APIKey get(String sid) throws ServerFault {
		try {
			String query = "SELECT sid, displayname, subject, domain_uid FROM t_api_key " + " WHERE sid = ? ";

			return unique(query, (rs) -> new APIKey(), Arrays.<EntityPopulator<APIKey>>asList(APIKEY_POPULATOR),
					new Object[] { sid });
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public boolean check(String userUid, String sid) throws SQLException {
		String query = "SELECT sid, displayname FROM t_api_key " + " WHERE subject = ? and sid = ? ";

		List<APIKey> selected = select(query, (rs) -> new APIKey(), new EntityPopulator<APIKey>() {

			@Override
			public int populate(ResultSet rs, int index, APIKey value) throws SQLException {
				return 0;
			}
		}, new Object[] { userUid, sid });

		return !selected.isEmpty();
	}

}
