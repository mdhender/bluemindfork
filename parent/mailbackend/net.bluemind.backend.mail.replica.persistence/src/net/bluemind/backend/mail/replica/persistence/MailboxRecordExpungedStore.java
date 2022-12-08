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
import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class MailboxRecordExpungedStore extends JdbcAbstractStore {

	protected static final Creator<MailboxRecordExpunged> MB_CREATOR = rs -> new MailboxRecordExpunged();

	public static final Integer LIMIT = 1000;

	private Container folderContainer;
	private Container subtreeContainer;

	public MailboxRecordExpungedStore(DataSource pool) {
		super(pool);
		Objects.requireNonNull(pool, "datasource must not be null");
		this.folderContainer = null;
		this.subtreeContainer = null;
	}

	public MailboxRecordExpungedStore(DataSource pool, Container folderContainer, Container subtreeContainer) {
		this(pool);
		this.folderContainer = folderContainer;
		this.subtreeContainer = subtreeContainer;
	}

	public void deleteAll() throws SQLException {
		delete("TRUNCATE q_mailbox_record_expunged", new Object[0]);
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

	private static final String CREATE_QUERY = "INSERT INTO q_mailbox_record_expunged ( "
			+ MailboxRecordExpungedColumns.COLUMNS.names() + ") VALUES ("
			+ MailboxRecordExpungedColumns.COLUMNS.values() + " ON CONFLICT DO NOTHUING";

	public void store(MailboxRecordExpunged value) throws SQLException {
		if (folderContainer == null || subtreeContainer == null) {
			return;
		}
		insert(CREATE_QUERY, value, Arrays.asList(MailboxRecordExpungedColumns.values(value.itemId),
				MailboxRecordExpungedColumns.values(null)));
	}

	public void delete(Long itemId) throws SQLException {
		if (folderContainer == null || subtreeContainer == null) {
			return;
		}
		delete("DELETE FROM q_mailbox_record_expunged WHERE container_id = ? AND subtree_id = ? AND item_id = ?",
				new Object[] { folderContainer.id, subtreeContainer.id, itemId });
	}

	private static final String GET_QUERY = "SELECT " + MailboxRecordExpungedColumns.COLUMNS.names()
			+ " FROM q_mailbox_record_expunged WHERE container_id = ? AND subtree_id = ? AND item_id = ?";

	public MailboxRecordExpunged get(Long itemId) throws SQLException {
		if (folderContainer == null || subtreeContainer == null) {
			return null;
		}
		return unique(GET_QUERY, MB_CREATOR, MailboxRecordExpungedColumns.populator(itemId),
				new Object[] { folderContainer.id, subtreeContainer.id, itemId });
	}

	private static final String FETCH_QUERY = "SELECT " + MailboxRecordExpungedColumns.COLUMNS.names()
			+ " FROM q_mailbox_record_expunged WHERE container_id = ? AND subtree_id = ?";

	public List<MailboxRecordExpunged> fetch() throws SQLException {
		if (folderContainer == null || subtreeContainer == null) {
			return null;
		}
		return select(FETCH_QUERY, con -> new MailboxRecordExpunged(), POPULATOR,
				new Object[] { folderContainer.id, subtreeContainer.id });
	}

	private static final EntityPopulator<MailboxRecordExpunged> POPULATOR = (rs, index, value) -> {
		value.containerId = rs.getInt(index++);
		value.subtreeId = rs.getInt(index++);
		value.itemId = rs.getLong(index++);
		value.imapUid = rs.getLong(index++);
		value.created = rs.getDate(index++);
		return index;
	};

	public Long count() throws SQLException {
		if (folderContainer == null || subtreeContainer == null) {
			return null;
		}
		String q = "SELECT COUNT(*) FROM q_mailbox_record_expunged WHERE container_id = ? AND subtree_id = ?";
		return unique(q, rs -> rs.getLong(1), (rs, index, v) -> index,
				new Object[] { folderContainer.id, subtreeContainer.id });
	}

	public String getContainerUid() {
		return folderContainer.uid;
	}
}
