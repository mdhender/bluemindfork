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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.core.container.persistance.StringCreator;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class MessageBodyStore extends JdbcAbstractStore {

	private Creator<MessageBody> MB_CREATOR = rs -> new MessageBody();

	public MessageBodyStore(DataSource pool) {
		super(pool);
		Objects.requireNonNull(pool, "datasource must not be null");
	}

	private static final String CREATE_QUERY = "INSERT INTO t_message_body ( " + MessageBodyColumns.COLUMNS.names()
			+ ", guid) VALUES (" + MessageBodyColumns.COLUMNS.values() + ", ? )";

	public void create(MessageBody value) throws SQLException {
		insert(CREATE_QUERY, value, MessageBodyColumns.values(value.guid));
	}

	private static final String UPD_QUERY = "UPDATE t_message_body SET ( " + MessageBodyColumns.COLUMNS.names()
			+ ") = (" + MessageBodyColumns.COLUMNS.values() + " )" + " WHERE guid = ? ";

	public void update(MessageBody value) throws SQLException {

		update(UPD_QUERY, value, MessageBodyColumns.values(value.guid));
	}

	public void delete(String guid) throws SQLException {
		delete("DELETE FROM t_message_body WHERE guid = ?", new Object[] { guid });
	}

	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_message_body", new Object[0]);
	}

	private static final String GET_QUERY = "SELECT " + MessageBodyColumns.COLUMNS.names()
			+ " FROM t_message_body WHERE guid = ?";

	public MessageBody get(String guid) throws SQLException {
		return unique(GET_QUERY, MB_CREATOR, MessageBodyColumns.populator(guid), new Object[] { guid });
	}

	private static final String MGET_QUERY = "SELECT guid, " + MessageBodyColumns.COLUMNS.names()
			+ " FROM t_message_body WHERE guid = ANY(?::text[])";

	public List<MessageBody> multiple(String... guids) throws SQLException {
		return select(MGET_QUERY, MB_CREATOR, (rs, index, mb) -> {
			mb.guid = rs.getString(index++);
			return MessageBodyColumns.simplePopulator().populate(rs, index, mb);
		}, new Object[] { guids });
	}

	public List<MessageBody> multiple(List<String> guid) throws SQLException {
		return multiple(guid.toArray(new String[guid.size()]));
	}

	public boolean exists(String uid) throws SQLException {
		String q = "select 1 from t_message_body mb where guid=?";
		Boolean found = unique(q, rs -> Boolean.TRUE, Collections.emptyList(), new Object[] { uid });
		return found == null ? false : true;
	}

	public List<String> existing(List<String> toCheck) throws SQLException {
		List<String> theList = java.util.Optional.ofNullable(toCheck).orElse(Collections.emptyList());
		String q = "select guid from t_message_body mb where guid=ANY(?)";
		return select(q, rs -> rs.getString(1), (rs, index, val) -> index,
				new Object[] { theList.toArray(new String[0]) });
	}

	public List<String> deleteOrphanBodies() throws SQLException {
		String query = "delete from t_message_body b where created < NOW() - INTERVAL '2 hour' and not exists (select from t_mailbox_record where message_body_guid = b.guid)";
		String selectQuery = "select b.guid from t_message_body b where created < NOW() - INTERVAL '2 hour' and not exists (select from t_mailbox_record where message_body_guid = b.guid)";
		List<String> selected = select(selectQuery, StringCreator.FIRST, (rs, index, val) -> index);
		int handled = delete(query, new Object[0]);
		logger.info("{} orphan bodies purged.", handled);
		return selected;
	}

}
