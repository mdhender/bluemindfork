/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.postgresql.util.PGInterval;

import io.netty.buffer.ByteBufUtil;
import net.bluemind.backend.mail.replica.api.IMessageBodyTierChange;
import net.bluemind.backend.mail.replica.api.Tier;
import net.bluemind.backend.mail.replica.api.TierMove;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class MessageBodyTierChangeQueueStore extends JdbcAbstractStore {
	public MessageBodyTierChangeQueueStore(DataSource dataSource) {
		super(dataSource);
		Objects.requireNonNull(dataSource, "datasource must not be null");
	}

	public void insert(String messageBodyGuid, Instant changeAfter, Tier tier) throws SQLException {
		insert("""
				INSERT INTO q_message_body_tier_change (message_body_guid, change_after, tier)
				VALUES (decode(?, 'hex'), ?, ?::enum_q_tier)
				""", new Object[] { messageBodyGuid, Timestamp.from(changeAfter), tier.name() });
	}

	public List<TierMove> getMoves(int limit) throws SQLException {
		return select("""
				SELECT encode(message_body_guid, 'hex'), tier, retries
				FROM q_message_body_tier_change
				WHERE change_after < now()
				ORDER BY change_after ASC, message_body_guid ASC
				LIMIT ?
				""", //
				rs -> new TierMove(rs.getString(1), Tier.from(rs.getString(2)), rs.getInt(3)), //
				(rs, index, val) -> index, new Object[] { limit });
	}

	public void deleteMoves(List<String> guids) throws SQLException {
		try (Connection con = getConnection()) {
			try (PreparedStatement st = con
					.prepareStatement("DELETE FROM q_message_body_tier_change WHERE message_body_guid = ANY(?)")) {
				st.setArray(1, con.createArrayOf("bytea", toByteArray(guids)));
				st.executeUpdate();
			}
		}
	}

	private byte[][] toByteArray(List<String> guids) {
		return guids.stream().map(ByteBufUtil::decodeHexDump).toList().toArray(new byte[0][0]);
	}

	public void retryDelayedMoves(List<String> guids) throws SQLException {
		try (Connection con = getConnection()) {
			try (PreparedStatement st = con.prepareStatement("""
					UPDATE q_message_body_tier_change
					SET change_after = change_after + '1 days'::interval,
					retries = retries + 1
					WHERE message_body_guid = ANY(?)
					""")) {
				st.setArray(1, con.createArrayOf("bytea", toByteArray(guids)));
				st.executeUpdate();
			}
		}
	}

	public void truncate() throws SQLException {
		try (Connection con = getConnection()) {
			try (PreparedStatement st = con.prepareStatement("TRUNCATE q_message_body_tier_change")) {
				st.executeUpdate();
			}
		}
	}

	public static record TierAddResult(int inserted, byte[] lastguid) {
	}

	public TierAddResult rebuildTierMoves(int limit, int archiveDays, byte[] minGuid) throws SQLException {
		final String insertQuery = """
				INSERT INTO q_message_body_tier_change (
				    message_body_guid,
				    change_after,
				    tier,
				    retries
				)
				SELECT guid, created + ?, 'SLOW'::enum_q_tier, ?
				FROM t_message_body
				WHERE guid > ?
				ORDER BY guid ASC
				LIMIT ?
				RETURNING message_body_guid
				""";
		int inserted = 0;
		byte[] lastGuid = { (byte) 0x00, (byte) 0x00 };
		try (Connection con = getConnection()) {
			try (PreparedStatement st = con.prepareStatement(insertQuery)) {
				st.setObject(1, new PGInterval(0, 0, archiveDays, 0, 0, 0));
				st.setInt(2, IMessageBodyTierChange.TIER_CHANGES_MAX_RETRIES); // We don't want to retry thoses
				st.setBytes(3, minGuid);
				st.setInt(4, limit);
				st.execute();
				try (ResultSet rs = st.getResultSet()) {
					while (rs.next()) {
						inserted++;
						lastGuid = rs.getBytes(1);
					}
				}
			}
		}
		return new TierAddResult(inserted, lastGuid);
	}

}
