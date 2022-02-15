/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfflineMgmtStore {

	private static final Logger logger = LoggerFactory.getLogger(OfflineMgmtStore.class);
	private final DataSource ds;

	public OfflineMgmtStore(DataSource ds) {
		this.ds = ds;
	}

	private static final String query(int count) {
		return "select locked_multi_nextval('t_container_item_id_seq', " + count + ")"; // NOSONAR
	}

	public long reserveItemIds(int count) throws SQLException {
		long ret = 0;
		try (Connection con = ds.getConnection();
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query(count))) {
			rs.next();
			ret = rs.getLong(1);
			if (logger.isDebugEnabled()) {
				logger.debug("SEQVAL: {}", ret);
			}
		}
		return ret;
	}

}
