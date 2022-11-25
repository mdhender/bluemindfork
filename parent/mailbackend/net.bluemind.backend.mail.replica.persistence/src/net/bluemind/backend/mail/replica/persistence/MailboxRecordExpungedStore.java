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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.postgresql.util.PGInterval;

import net.bluemind.backend.mail.replica.api.MailboxRecordExpunged;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class MailboxRecordExpungedStore extends JdbcAbstractStore {

	private static final Creator<MailboxRecordExpunged> MB_CREATOR = rs -> new MailboxRecordExpunged();

	public static final Integer LIMIT = 1000;

	public MailboxRecordExpungedStore(DataSource pool) {
		super(pool);
		Objects.requireNonNull(pool, "datasource must not be null");
	}

	private static final String CREATE_QUERY = "INSERT INTO q_mailbox_record_expunged ( "
			+ MailboxRecordExpungedColumns.COLUMNS.names() + ") VALUES ("
			+ MailboxRecordExpungedColumns.COLUMNS.values() + " ON CONFLICT DO NOTHUING";

	public void store(MailboxRecordExpunged value) throws SQLException {
		insert(CREATE_QUERY, value, Arrays.asList(MailboxRecordExpungedColumns.values(value.containerId, value.itemId),
				MailboxRecordExpungedColumns.values(null, null)));
	}

	public void delete(Integer containerId, Long itemId) throws SQLException {
		delete("DELETE FROM q_mailbox_record_expunged WHERE container_id = ? AND item_id = ?",
				new Object[] { containerId, itemId });
	}

	public void deleteAll() throws SQLException {
		delete("TRUNCATE q_mailbox_record_expunged", new Object[0]);
	}

	private static final String GET_QUERY = "SELECT " + MailboxRecordExpungedColumns.COLUMNS.names()
			+ " FROM q_mailbox_record_expunged WHERE container_id = ? AND item_id = ?";

	public MailboxRecordExpunged get(Integer containerId, Long itemId) throws SQLException {
		return unique(GET_QUERY, MB_CREATOR, MailboxRecordExpungedColumns.populator(containerId.intValue(), itemId),
				new Object[] { containerId, itemId });
	}

	public List<MailboxRecordExpunged> getExpiredItems(int days) throws SQLException {
		String query = "SELECT container_id, subtree_id, item_id, imap_uid "//
				+ " FROM q_mailbox_record_expunged" //
				+ " WHERE created < now() - ?" //
				+ " LIMIT ?";

		return select(query, con -> new MailboxRecordExpunged(), (rs, index, itemv) -> {
			itemv.containerId = rs.getInt(index++);
			itemv.subtreeId = rs.getInt(index++);
			itemv.itemId = rs.getLong(index++);
			itemv.imapUid = rs.getLong(index++);
			return index;
		}, new Object[] { new PGInterval(0, 0, days, 0, 0, 0), LIMIT });
	}

	public void deleteExpunged(Integer containerId, List<Long> imapUids) throws SQLException {
		if (imapUids.isEmpty()) {
			return;
		}

		delete("DELETE FROM q_mailbox_record_expunged WHERE container_id = ? AND imap_uid = ANY (?::int8[])",
				new Object[] { containerId, imapUids.stream().toArray(Long[]::new) });
	}

}
