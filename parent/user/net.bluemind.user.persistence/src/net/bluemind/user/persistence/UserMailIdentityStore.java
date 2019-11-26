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
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentityStore extends JdbcAbstractStore {

	private Container container;

	public UserMailIdentityStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	public void create(Item userItem, String id, UserMailIdentity identity) throws SQLException {
		String query = "INSERT INTO t_user_mailidentity (" + UserMailIdentityColumns.cols.names()
				+ ", id, user_id) VALUES (" + UserMailIdentityColumns.cols.values() + ", ?, ? )";

		insert(query, identity, statementValue(userItem, id));
	}

	public void update(Item userItem, String id, UserMailIdentity identity) throws SQLException {
		String query = "UPDATE t_user_mailidentity set (" + UserMailIdentityColumns.cols.names() + " ) = ("
				+ UserMailIdentityColumns.cols.values() + ") WHERE id = ? and user_id = ?";

		insert(query, identity, statementValue(userItem, id));
	}

	public void delete(Item userItem, String id) throws SQLException {
		String query = "DELETE FROM t_user_mailidentity WHERE id = ? and user_id = ?";
		delete(query, new Object[] { id, userItem.id });
	}

	public void delete(Item userItem) throws SQLException {
		String query = "DELETE FROM t_user_mailidentity WHERE user_id = ?";
		delete(query, new Object[] { userItem.id });
	}

	public void deleteAll() throws SQLException {
		String query = "DELETE FROM t_user_mailidentity WHERE user_id in (select id from t_container_item where container_id = ? )";
		delete(query, new Object[] { container.id });

	}

	public UserMailIdentity get(Item userItem, String id) throws SQLException {
		String query = "SELECT " + UserMailIdentityColumns.cols.names()
				+ ", is_default FROM t_user_mailidentity WHERE id = ? and user_id = ?";
		return unique(query, identityCreate(), Arrays.asList(identityPopulator()), new Object[] { id, userItem.id });
	}

	private EntityPopulator<UserMailIdentity> identityPopulator() {
		return new EntityPopulator<UserMailIdentity>() {

			@Override
			public int populate(ResultSet rs, int index, UserMailIdentity value) throws SQLException {
				value.mailboxUid = rs.getString(index++);
				value.name = rs.getString(index++);
				value.format = SignatureFormat.valueOf(rs.getString(index++));
				value.signature = rs.getString(index++);
				value.displayname = rs.getString(index++);
				value.email = rs.getString(index++);
				value.sentFolder = rs.getString(index++);
				value.isDefault = rs.getBoolean(index++);
				return index;
			}
		};
	}

	private Creator<UserMailIdentity> identityCreate() {
		return new Creator<UserMailIdentity>() {

			@Override
			public UserMailIdentity create(ResultSet con) throws SQLException {
				return new UserMailIdentity();
			}
		};
	}

	private StatementValues<UserMailIdentity> statementValue(final Item userItem, final String id) {
		return new StatementValues<UserMailIdentity>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					UserMailIdentity value) throws SQLException {

				statement.setString(index++, value.mailboxUid);
				statement.setString(index++, value.name);
				statement.setString(index++, value.format.name());
				statement.setString(index++, value.signature);
				statement.setString(index++, value.displayname);
				statement.setString(index++, value.email);
				statement.setString(index++, value.sentFolder);
				statement.setString(index++, id);
				statement.setLong(index++, userItem.id);
				return index;
			}
		};
	}

	public List<IdentityDescription> getDescriptions(Item userItem) throws SQLException {

		String query = "SELECT id, email, name, mbox_uid, is_default, displayname, signature FROM t_user_mailidentity WHERE user_id = ? order by is_default desc, name";

		return select(query, identityDescriptionCreate(), Arrays.asList(identityDescriptionPopulator()),
				new Object[] { userItem.id });
	}

	private EntityPopulator<IdentityDescription> identityDescriptionPopulator() {
		return new EntityPopulator<IdentityDescription>() {

			@Override
			public int populate(ResultSet rs, int index, IdentityDescription value) throws SQLException {
				value.id = rs.getString(index++);
				value.email = rs.getString(index++);
				value.name = rs.getString(index++);
				value.mbox = rs.getString(index++);
				value.isDefault = rs.getBoolean(index++);
				value.displayname = rs.getString(index++);
				value.signature = rs.getString(index++);
				return index;
			}
		};
	}

	private Creator<IdentityDescription> identityDescriptionCreate() {
		return new Creator<IdentityDescription>() {

			@Override
			public IdentityDescription create(ResultSet con) throws SQLException {
				IdentityDescription ret = new IdentityDescription();
				return ret;
			}
		};
	}

	public void setDefault(Item item, String id) throws SQLException {
		String query = "UPDATE t_user_mailidentity set is_default = ( id = ? ) where user_id = ?";
		update(query, null, new Object[] { id, item.id });
	}

	public void deleteMailboxIdentities(Item item, String mailboxUid) throws SQLException {
		String query = "DELETE FROM t_user_mailidentity WHERE mbox_uid = ? and user_id = ?";
		delete(query, new Object[] { mailboxUid, item.id });
	}

	public void deleteMailboxIdentities(String mailboxUid) throws SQLException {
		String query = "DELETE FROM t_user_mailidentity WHERE mbox_uid = ?";
		delete(query, new Object[] { mailboxUid });
	}
}
