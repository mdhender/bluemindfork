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
package net.bluemind.mailbox.identity.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;

public class MailboxIdentityStore extends JdbcAbstractStore {

	public MailboxIdentityStore(DataSource dataSource) {
		super(dataSource);
	}

	public void create(Item mboxItem, String id, Identity identity) throws SQLException {
		String query = "INSERT INTO t_mailbox_identity (" + IdentityColumns.cols.names() + ", id, mbox_id ) VALUES ("
				+ IdentityColumns.cols.values() + ", ? , ? )";

		insert(query, identity, statementValue(mboxItem, id));
	}

	public void update(Item mboxItem, String id, Identity identity) throws SQLException {
		String query = "UPDATE t_mailbox_identity set (" + IdentityColumns.cols.names() + " ) = ("
				+ IdentityColumns.cols.values() + ") WHERE id = ? and mbox_id = ?";

		insert(query, identity, statementValue(mboxItem, id));
	}

	public void delete(Item mboxItem, String id) throws SQLException {
		String query = "DELETE FROM t_mailbox_identity WHERE id = ? and mbox_id = ?";
		delete(query, new Object[] { id, mboxItem.id });
	}

	public Identity get(Item mboxItem, String id) throws SQLException {
		String query = "SELECT " + IdentityColumns.cols.names()
				+ " FROM t_mailbox_identity WHERE id = ? and mbox_id = ?";
		return unique(query, identityCreate(), Arrays.asList(identityPopulator()), new Object[] { id, mboxItem.id });
	}

	public void delete(Item item) throws SQLException {
		String query = "DELETE FROM t_mailbox_identity WHERE mbox_id = ?";
		delete(query, new Object[] { item.id });
	}

	private EntityPopulator<Identity> identityPopulator() {
		return new EntityPopulator<Identity>() {

			@Override
			public int populate(ResultSet rs, int index, Identity value) throws SQLException {
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

	private Creator<Identity> identityCreate() {
		return new Creator<Identity>() {

			@Override
			public Identity create(ResultSet con) throws SQLException {
				return new Identity();
			}
		};
	}

	private StatementValues<Identity> statementValue(final Item mboxItem, final String id) {
		return new StatementValues<Identity>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, Identity value)
					throws SQLException {

				statement.setString(index++, value.name);
				statement.setString(index++, value.format.name());
				statement.setString(index++, value.signature);
				statement.setString(index++, value.displayname);
				statement.setString(index++, value.email);
				statement.setString(index++, value.sentFolder);
				statement.setBoolean(index++, value.isDefault);
				statement.setString(index++, id);
				statement.setLong(index++, mboxItem.id);
				return index;
			}
		};
	}

	public List<IdentityDescription> getDescriptions(final ItemValue<Mailbox> mboxItemValue) throws SQLException {

		String query = "SELECT id, email, name, displayname, is_default, signature FROM t_mailbox_identity WHERE mbox_id = ?";

		return select(query, identityDescriptionCreate(mboxItemValue.uid, mboxItemValue.value.name),
				Arrays.asList(identityDescriptionPopulator()), new Object[] { mboxItemValue.internalId });
	}

	private EntityPopulator<IdentityDescription> identityDescriptionPopulator() {
		return new EntityPopulator<IdentityDescription>() {

			@Override
			public int populate(ResultSet rs, int index, IdentityDescription value) throws SQLException {
				value.id = rs.getString(index++);
				value.email = rs.getString(index++);
				value.name = rs.getString(index++);
				value.displayname = rs.getString(index++);
				value.isDefault = rs.getBoolean(index++);
				value.signature = rs.getString(index++);
				return index;
			}
		};
	}

	private Creator<IdentityDescription> identityDescriptionCreate(final String uid, final String mboxName) {
		return new Creator<IdentityDescription>() {

			@Override
			public IdentityDescription create(ResultSet con) throws SQLException {
				IdentityDescription ret = new IdentityDescription();
				ret.mbox = uid;
				ret.mboxName = mboxName;
				return ret;
			}
		};
	}

}
