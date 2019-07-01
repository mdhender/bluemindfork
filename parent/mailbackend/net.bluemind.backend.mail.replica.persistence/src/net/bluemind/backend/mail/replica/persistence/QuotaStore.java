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

import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class QuotaStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(QuotaStore.class);

	public QuotaStore(DataSource pool) {
		super(pool);
	}

	public void store(QuotaRoot qr) throws SQLException {
		String query = "INSERT INTO t_quota_root ( " + QuotaColumns.COLUMNS.names() + ") VALUES ("
				+ QuotaColumns.COLUMNS.values() + ") ON CONFLICT (root) DO UPDATE SET (" + QuotaColumns.COLUMNS.names()
				+ ") = (" + QuotaColumns.COLUMNS.values() + ")";
		insert(query, qr, Arrays.asList(QuotaColumns.values(), QuotaColumns.values()));
		logger.info("QuotaRoot inserted.");
	}

	public void delete(QuotaRoot qr) throws SQLException {
		String query = "DELETE FROM t_quota_root where root=?";
		delete(query, new Object[] { qr.root });
		logger.info("QuotaRoot {} deleted.", qr);
	}

	public List<QuotaRoot> byUser(String userId) throws SQLException {
		String[] split = userId.split("@");
		String root = split[1] + "!user." + split[0].replace('.', '^');
		String query = "SELECT " + QuotaColumns.COLUMNS.names() + " FROM t_quota_root WHERE root = ?";
		return select(query, rs -> new QuotaRoot(), QuotaColumns.populator(), new Object[] { root });

	}
}
