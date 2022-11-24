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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class MessageBodyStore extends JdbcAbstractStore {
	private static final Creator<MessageBody> MB_CREATOR = rs -> new MessageBody();

	public MessageBodyStore(DataSource pool) {
		super(pool);
		Objects.requireNonNull(pool, "datasource must not be null");
	}

	private static final String CREATE_QUERY = "INSERT INTO t_message_body ( " + MessageBodyColumns.COLUMNS.names()
			+ ", guid) VALUES (" + MessageBodyColumns.COLUMNS.values()
			+ ", decode(?, 'hex')) ON CONFLICT (guid) DO UPDATE SET (" + MessageBodyColumns.COLUMNS.names() + ") = ("
			+ MessageBodyColumns.COLUMNS.values() + " )";

	public void store(MessageBody value) throws SQLException {
		insert(CREATE_QUERY, value,
				Arrays.asList(MessageBodyColumns.values(value.guid), MessageBodyColumns.values(null)));
	}

	public void delete(String guid) throws SQLException {
		delete("DELETE FROM t_message_body WHERE guid = decode(?, 'hex')", new Object[] { guid });
	}

	public void deleteAll() throws SQLException {
		delete("TRUNCATE t_message_body CASCADE", new Object[0]);
	}

	private static final String GET_QUERY = "SELECT " + MessageBodyColumns.COLUMNS.names()
			+ " FROM t_message_body WHERE guid = decode(?, 'hex')";

	public MessageBody get(String guid) throws SQLException {
		return unique(GET_QUERY, MB_CREATOR, MessageBodyColumns.populator(guid), new Object[] { guid });
	}

	private static final String MGET_QUERY = "SELECT encode(guid, 'hex'), " + MessageBodyColumns.COLUMNS.names()
			+ " FROM t_message_body WHERE guid = ANY(?::bytea[])";

	public List<MessageBody> multiple(String... guids) throws SQLException {
		return multiple(new Object[] { toByteArray(guids) });
	}

	public List<MessageBody> multiple(List<String> guids) throws SQLException {
		return multiple(new Object[] { toByteArray(guids) });
	}

	private List<MessageBody> multiple(Object[] byteArrays) throws SQLException {
		return select(MGET_QUERY, MB_CREATOR, (rs, index, mb) -> {
			mb.guid = rs.getString(index++);
			return MessageBodyColumns.simplePopulator().populate(rs, index, mb);
		}, byteArrays);
	}

	public boolean exists(String uid) throws SQLException {
		String q = "SELECT 1 FROM t_message_body WHERE guid = decode(?, 'hex')";
		Boolean found = unique(q, rs -> Boolean.TRUE, Collections.emptyList(), new Object[] { uid });
		return found != null;
	}

	public List<String> existing(List<String> toCheck) throws SQLException {
		List<String> theList = java.util.Optional.ofNullable(toCheck).orElse(Collections.emptyList());
		String q = "SELECT encode(guid, 'hex') FROM t_message_body WHERE guid = ANY(?::bytea[])";
		return select(q, StringCreator.FIRST, (rs, index, val) -> index, new Object[] { toByteArray(theList) });
	}

	public List<String> deleteOrphanBodies() throws SQLException {
		String query = "DELETE FROM t_message_body mb USING t_message_body_purge_queue pq "
				+ "WHERE pq.message_body_guid = mb.guid AND pq.created <= now() - '2 days'::interval "
				+ "RETURNING encode(mb.guid, 'hex')";
		List<String> selected = delete(query, StringCreator.FIRST, Arrays.asList((rs, index, val) -> index));
		int size = selected.size();
		logger.info("{} orphan bodies purged.", size);
		if (size > 0) {
			markPurgeQueueRemoved();
		}
		return selected;
	}

	/**
	 * Mark any entry of t_message_body_purge_queue which are not present inside
	 * t_message_body for later removal. The removal is done by an external
	 * verticle, which uses a configurable remove delay in SystemConfiguration.
	 * 
	 * @throws SQLException
	 */
	private void markPurgeQueueRemoved() throws SQLException {
		String query = "UPDATE t_message_body_purge_queue SET removed=now() FROM (" //
				+ "    SELECT pq.message_body_guid FROM t_message_body_purge_queue " //
				+ "    pq LEFT JOIN t_message_body mb ON (mb.guid = pq.message_body_guid) " //
				+ "    WHERE mb.guid IS NULL" //
				+ ") pqnull " //
				+ "WHERE removed IS NULL AND pqnull.message_body_guid = t_message_body_purge_queue.message_body_guid";
		update(query, null);
	}

	public List<String> deletePurgedBodies(Instant removedBefore, long limit) throws SQLException {
		String query = "WITH bodies AS" //
				+ " (SELECT message_body_guid FROM t_message_body_purge_queue" //
				+ " WHERE removed IS NOT NULL AND removed <= ? LIMIT ?)" //
				+ " DELETE FROM t_message_body_purge_queue WHERE message_body_guid IN (SELECT message_body_guid FROM bodies) RETURNING encode(message_body_guid, 'hex')";

		return delete(query, StringCreator.FIRST, Arrays.asList((rs, index, val) -> index),
				new Object[] { Timestamp.from(removedBefore), limit });
	}

	private String[] toByteArray(String... guids) {
		int len = guids.length;
		String[] ret = new String[len];
		for (int i = 0; i < len; i++) {
			ret[i] = "\\x" + guids[i];
		}
		return ret;
	}

	private String[] toByteArray(List<String> guids) {
		String[] ret = new String[guids.size()];
		int i = 0;
		for (String guid : guids) {
			ret[i++] = "\\x" + guid;
		}
		return ret;
	}

}
