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
package net.bluemind.core.container.persistance;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

public class OfflineMgmtStore {

	private DataSource ds;

	public OfflineMgmtStore(DataSource ds) {
		this.ds = ds;
	}

	public long reserveItemIds(int count) throws SQLException {
		long ret = 0;
		try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
			try (ResultSet rs = st.executeQuery("select nextval('t_container_item_id_seq')")) {
				rs.next();
				ret = rs.getLong(1);
				st.execute("ALTER SEQUENCE t_container_item_id_seq RESTART with " + (ret + count)); // NOSONAR
			}
		}
		return ret;
	}

}
