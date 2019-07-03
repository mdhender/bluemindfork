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

import net.bluemind.backend.mail.replica.api.SieveScript;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class SieveScriptStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(SieveScriptStore.class);

	public SieveScriptStore(DataSource pool) {
		super(pool);
	}

	public void store(SieveScript script) throws SQLException {
		String query = "INSERT INTO t_sieve_script ( " + SieveScriptColumns.COLUMNS.names() + ") VALUES ("
				+ SieveScriptColumns.COLUMNS.values() + ") ON CONFLICT (user_id,filename) DO UPDATE SET ("
				+ SieveScriptColumns.COLUMNS.names() + ")=(" + SieveScriptColumns.COLUMNS.values() + ")";
		insert(query, script, Arrays.asList(SieveScriptColumns.values(), SieveScriptColumns.values()));
		logger.info("upsert-ed {}", script.fileName);
	}

	public void delete(SieveScript sub) throws SQLException {
		String query = "DELETE FROM t_sieve_script where user_id=? AND filename=?";
		delete(query, new Object[] { sub.userId, sub.fileName });
		logger.info("Sieve {} deleted.", sub.fileName);
	}

	public List<SieveScript> byUser(String userId) throws SQLException {
		String query = "SELECT " + SieveScriptColumns.COLUMNS.names() + " FROM t_sieve_script WHERE user_id = ?";
		return select(query, rs -> new SieveScript(), SieveScriptColumns.populator(), new Object[] { userId });
	}
}
