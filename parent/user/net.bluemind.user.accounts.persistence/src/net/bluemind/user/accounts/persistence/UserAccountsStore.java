/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.user.api.UserAccount;
import net.bluemind.user.api.UserAccountInfo;

public class UserAccountsStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(UserAccountsStore.class);

	private static final String CREATE_QUERY = "INSERT INTO t_user_account (" + UserAccountColumns.cols.names()
			+ ", item_id, system) VALUES (" + UserAccountColumns.cols.values() + ", ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE t_user_account SET (" + UserAccountColumns.cols.names()
			+ ", system) = (" + UserAccountColumns.cols.values() + ", ?) WHERE item_id = ? and system = ?";
	private static final String DELETE_QUERY = "DELETE FROM t_user_account WHERE item_id = ? AND system = ?";
	private static String DELETE_ALL_QUERY = "DELETE FROM t_user_account WHERE item_id = ?";
	private static final String GET_QUERY = "SELECT " + UserAccountColumns.cols.names("u")
			+ " FROM t_user_account u INNER JOIN t_container_item item ON u.item_id = item.id WHERE u.item_id = ? AND u.system = ?";
	private static final String GET_ALL_QUERY = "SELECT " + UserAccountColumns.infoCols.names("u")
			+ " FROM t_user_account u INNER JOIN t_container_item item ON u.item_id = item.id WHERE u.item_id = ?";

	private static final Creator<UserAccount> USER_ACCOUNT_CREATOR = new Creator<UserAccount>() {
		@Override
		public UserAccount create(ResultSet con) throws SQLException {
			return new UserAccount();
		}
	};

	private static final Creator<UserAccountInfo> USER_ACCOUNT_INFO_CREATOR = new Creator<UserAccountInfo>() {
		@Override
		public UserAccountInfo create(ResultSet con) throws SQLException {
			return new UserAccountInfo();
		}
	};

	public UserAccountsStore(DataSource pool) {
		super(pool);
	}

	public void create(Item item, String systemIdentifier, UserAccount value) throws SQLException {
		insert(CREATE_QUERY, value, UserAccountColumns.statementValues(), new Object[] { item.id, systemIdentifier });
	}

	public void update(Item item, String systemIdentifier, UserAccount value) throws SQLException {
		update(UPDATE_QUERY, value, UserAccountColumns.statementValues(),
				new Object[] { systemIdentifier, item.id, systemIdentifier });
	}

	public void delete(Item item, String systemIdentifier) throws SQLException {
		delete(DELETE_QUERY, new Object[] { item.id, systemIdentifier });
	}

	public void deleteAll(Item item) throws SQLException {
		delete(DELETE_ALL_QUERY, new Object[] { item.id });
	}

	public UserAccount get(Item item, String systemIdentifier) throws SQLException {
		UserAccount u = null;
		u = unique(GET_QUERY, USER_ACCOUNT_CREATOR, UserAccountColumns.populator(),
				new Object[] { item.id, systemIdentifier });
		return u;

	}

	public List<UserAccountInfo> getAll(Item item) throws SQLException {
		return select(GET_ALL_QUERY, USER_ACCOUNT_INFO_CREATOR, UserAccountColumns.infoPopulator(),
				new Object[] { item.id });
	}

}
