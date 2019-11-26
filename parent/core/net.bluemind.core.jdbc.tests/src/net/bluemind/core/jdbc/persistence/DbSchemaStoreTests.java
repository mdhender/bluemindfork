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
package net.bluemind.core.jdbc.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.sql.DataSource;

import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import com.google.common.io.ByteStreams;

import net.bluemind.core.jdbc.JdbcException;
import net.bluemind.core.jdbc.JdbcHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.jdbc.SchemaDescriptor;

public class DbSchemaStoreTests {

	private DbSchemaStore schemaStore;
	private SchemaDescriptor sampleDescriptor;
	private DataSource ds;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().initPools();

		ds = JdbcTestHelper.getInstance().getDataSource();
		Bundle bundle = Platform.getBundle("net.bluemind.core.sqlschema");
		assertNotNull(bundle);

		URL url = bundle.getResource("sql/schema-version-0.0.1.sql");
		assertNotNull(url);
		String schemaVersion = null;
		try (InputStream in = url.openStream()) {
			byte[] b = ByteStreams.toByteArray(in);
			schemaVersion = new String(b);
		}

		Connection con = null;
		Statement st = null;
		try {
			con = ds.getConnection();
			st = con.createStatement();
			st.execute(schemaVersion);
		} finally {
			JdbcHelper.cleanup(con, null, st);
		}
		sampleDescriptor = new SchemaDescriptor("test", "2.3.0", new URL("file:sql/test-2.3.0.sql"),
				Arrays.<String>asList(), false);

		schemaStore = new DbSchemaStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateSchema() throws MalformedURLException {
		schemaStore.createSchema(sampleDescriptor);

		// check table unittest is created
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			con = ds.getConnection();
			st = con.createStatement();
			rs = st.executeQuery("select * from unittest");
		} catch (SQLException e) {
			fail(e.getMessage());
		} finally {
			JdbcHelper.cleanup(con, rs, st);
		}

		try {
			schemaStore.createSchema(sampleDescriptor);
			fail("cant create a schema 2 times");
		} catch (JdbcException e) {
		}
	}

	@Test
	public void testGetVersion() throws MalformedURLException {
		String version = schemaStore.getSchemaVersion(sampleDescriptor.getName());
		// schema is not yet created
		assertNull(version);
		schemaStore.createSchema(sampleDescriptor);
		version = schemaStore.getSchemaVersion(sampleDescriptor.getName());
		assertEquals(sampleDescriptor.getVersion(), version);
	}
}
