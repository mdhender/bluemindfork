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
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class SeenOverlayStore extends JdbcAbstractStore {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(SeenOverlayStore.class);

	public SeenOverlayStore(DataSource pool) {
		super(pool);
	}

	public void store(SeenOverlay overlay) throws SQLException {
		String query = "INSERT INTO t_seen_overlay ( " + SeenOverlayColumns.COLUMNS.names() + ") VALUES ("
				+ SeenOverlayColumns.COLUMNS.values() + ") ON CONFLICT (user_id, unique_id) DO UPDATE SET ("
				+ SeenOverlayColumns.COLUMNS.names() + ")=(" + SeenOverlayColumns.COLUMNS.values() + ")";
		insert(query, overlay, Arrays.asList(SeenOverlayColumns.values(), SeenOverlayColumns.values()));
	}

	public List<SeenOverlay> byUser(String userId) throws SQLException {
		String query = "SELECT " + SeenOverlayColumns.COLUMNS.names() + " FROM t_seen_overlay WHERE user_id = ?";
		return select(query, rs -> new SeenOverlay(), SeenOverlayColumns.populator(), new Object[] { userId });
	}

	public void deleteByUser(String userId) throws SQLException {
		delete("DELETE FROM t_seen_overlay WHERE user_id = ?", new Object[] { userId });
	}

	public SeenOverlay byUser(String userId, String mboxId) throws SQLException {
		String query = "SELECT " + SeenOverlayColumns.COLUMNS.names()
				+ " FROM t_seen_overlay WHERE user_id = ? AND unique_id = ?";
		return unique(query, rs -> new SeenOverlay(), SeenOverlayColumns.populator(), new Object[] { userId, mboxId });

	}
}
