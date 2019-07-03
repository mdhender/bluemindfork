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
package net.bluemind.dataprotect.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.dataprotect.api.RetentionPolicy;

public class RetentionPolicyStore extends JdbcAbstractStore {

	private static final Creator<RetentionPolicy> CREATOR = new Creator<RetentionPolicy>() {

		@Override
		public RetentionPolicy create(ResultSet con) throws SQLException {
			RetentionPolicy rp = new RetentionPolicy();
			return rp;
		}
	};
	private static final EntityPopulator<RetentionPolicy> POPULATOR = new EntityPopulator<RetentionPolicy>() {

		@Override
		public int populate(ResultSet rs, int index, RetentionPolicy value) throws SQLException {
			int retention = rs.getInt(index++);
			if (!rs.wasNull()) {
				value.daily = retention;
			}
			value.weekly = rs.getInt(index++);
			value.monthly = rs.getInt(index++);
			return index;
		}
	};

	public RetentionPolicyStore(DataSource dataSource) {
		super(dataSource);
	}

	public RetentionPolicy get() throws SQLException {
		List<RetentionPolicy> policy = select("SELECT daily, weekly, monthly FROM t_dp_retentionpolicy", CREATOR,
				POPULATOR);

		if (policy.size() == 0) {
			return new RetentionPolicy();
		} else {
			return policy.get(0);
		}

	}

	public void update(RetentionPolicy rp) throws ServerFault {
		doOrFail(() -> {
			delete("DELETE FROM t_dp_retentionpolicy", new Object[] {});
			insert("INSERT INTO t_dp_retentionpolicy ( daily, weekly, monthly) values (?,?,?)", rp,
					new StatementValues<RetentionPolicy>() {

						@Override
						public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
								RetentionPolicy value) throws SQLException {

							if (value.daily == null) {
								statement.setNull(index++, Types.INTEGER);
							} else {
								statement.setInt(index++, value.daily);
							}

							if (value.weekly == null) {
								statement.setNull(index++, Types.INTEGER);
							} else {
								statement.setInt(index++, value.weekly);
							}

							if (value.monthly == null) {
								statement.setNull(index++, Types.INTEGER);
							} else {
								statement.setInt(index++, value.monthly);
							}

							return index;
						}
					});

			return null;
		});
	}
}
