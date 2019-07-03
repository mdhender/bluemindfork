/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.pool.tests;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import net.bluemind.pool.BMPoolActivator;

public class PoolTests extends TestCase {

	public void testCreatePool() {
		BMPoolActivator opa = BMPoolActivator.getDefault();
		assertNotNull(opa);
	}

	public void testSQL() throws SQLException {
		BMPoolActivator opa = BMPoolActivator.getDefault();
		assertNotNull(opa);
		Connection con = opa.getConnection();
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("SELECT 1");
		rs.next();
		int ret = rs.getInt(1);
		assertTrue(ret == 1);
		rs.close();
		st.close();
		con.close();
	}

	public void testLeak() throws SQLException {
		BMPoolActivator opa = BMPoolActivator.getDefault();
		assertNotNull(opa);
		List<Connection> cons = new LinkedList<Connection>();
		// the pool has 2xcores + 2
		int limit = Runtime.getRuntime().availableProcessors() * 2 + 4;
		for (int i = 0; i < limit; i++) {
			System.out.println("getting connection " + (i + 1) + "...");
			Connection con = opa.getConnection();
			assertNotNull(con);
			cons.add(con);
			System.out.println((i + 1) + " obtained.");
		}
	}
}
