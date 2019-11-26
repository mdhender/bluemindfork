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
package net.bluemind.core.jdbc;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.persistence.DbSchemaStore;

public class DbSchemaServiceTests {

	private DataSource ds;
	private DbSchemaService dbSchemaServcie;
	private DbSchemaStore schemaStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		ds = JdbcTestHelper.getInstance().getDataSource();

		schemaStore = new DbSchemaStore(ds);
		dbSchemaServcie = new DbSchemaService(schemaStore, true);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testInitializeSchema() {
		dbSchemaServcie.initializeSchema("schema-version");
		dbSchemaServcie.initializeSchema("test");
		// check tables version and unittest exists
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			con = ds.getConnection();
			st = con.createStatement();
			rs = st.executeQuery("select * from schema_version");
			JdbcHelper.cleanup(null, rs, st);

			st = con.createStatement();
			rs = st.executeQuery("select * from unittest");
			JdbcHelper.cleanup(null, rs, st);
		} catch (SQLException e) {
			fail(e.getMessage());
		} finally {
			JdbcHelper.cleanup(con, rs, st);
		}

	}
}
