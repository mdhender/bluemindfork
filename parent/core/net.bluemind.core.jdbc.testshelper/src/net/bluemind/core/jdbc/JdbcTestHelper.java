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

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import net.bluemind.core.caches.testhelper.CachesTestHelper;
import net.bluemind.pool.BMPoolActivator;
import net.bluemind.pool.Pool;
import net.bluemind.pool.impl.BmConfIni;

public class JdbcTestHelper {

	static {
		// HOLLOW uses JUL...
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private static final Logger logger = LoggerFactory.getLogger(JdbcTestHelper.class);

	private static final JdbcTestHelper instance = new JdbcTestHelper();
	private String schemaName;
	private Pool pool;

	private Pool dataPool;
	private List<Pool> otherPools = new LinkedList<>();

	private JdbcTestHelper() {
	}

	public void beforeTest() throws Exception {
		beforeTest("junit_" + System.nanoTime());
	}

	public void beforeTest(String schemaName) throws Exception {
		initPools(schemaName);
		initializeSchema();
	}

	public void initPools() throws Exception {
		initPools("junit_" + System.nanoTime());
	}

	public void initPools(String schemaName) throws Exception {
		if (pool != null) {
			logger.info("stop directory pool");
			stopPool(pool);
			JdbcActivator.getInstance().setDataSource(null);
		}
		if (dataPool != null) {
			logger.info("stop data pool");
			stopPool(dataPool);
			JdbcActivator.getInstance().setMailboxDataSource(new HashMap<String, DataSource>());
		}

		CachesTestHelper.invalidate();
		BmConfIni conf = new BmConfIni();
		String dbType = conf.get("dbtype");
		String login = conf.get("user");
		String password = conf.get("password");
		String dbName = conf.get("db");
		String dbHost = conf.get("host");
		System.out.println("conf: " + conf.getClass().getCanonicalName());

		pool = BMPoolActivator.getDefault().newPool(dbType, login, password, dbName, dbHost,
				Runtime.getRuntime().availableProcessors() * 2 - 1, schemaName);

		JdbcActivator.getInstance().setDataSource(pool.getDataSource());

		dataPool = BMPoolActivator.getDefault().newPool(dbType, login, password, "test-data", dbHost,
				Runtime.getRuntime().availableProcessors() * 2 - 1, schemaName);

		String dataLocation = new BmConfIni().get("imap-role");
		if (dataLocation == null) {
			dataLocation = dbHost;
		}

		Map<String, DataSource> mailboxDataSource = new HashMap<String, DataSource>();
		mailboxDataSource.put(dataLocation, dataPool.getDataSource());
		// PopulateHelper.FAKE_CYRUS_IP
		mailboxDataSource.put("10.1.2.3", dataPool.getDataSource());
		JdbcActivator.getInstance().setMailboxDataSource(mailboxDataSource);

		BMPoolActivator.getDefault().addMailboxDataSource(dataLocation, dataPool);

		JdbcActivator.getInstance().setSchemaName(schemaName);
		this.schemaName = schemaName;

		BMPoolActivator.getDefault().defaultPool().stop();
	}

	private void initializeSchema() {
		Thread dirSchema = new Thread(() -> {
			DbSchemaService.getService(pool.getDataSource(), true).initialize(false);
		});
		dirSchema.start();
		logger.info("data pool init schema");
		DbSchemaService.getService(dataPool.getDataSource(), true).initialize(false);
		try {
			dirSchema.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void initNewServer(String ip) throws Exception {
		BmConfIni conf = new BmConfIni();
		String dbType = conf.get("dbtype");
		String login = conf.get("user");
		String password = conf.get("password");

		Pool newPool = BMPoolActivator.getDefault().newPool(dbType, login, password, "test-data", ip,
				Runtime.getRuntime().availableProcessors() * 2 - 1, schemaName);
		JdbcActivator.getInstance().addMailboxDataSource(ip, newPool.getDataSource());
		otherPools.add(newPool);
		DbSchemaService.getService(newPool.getDataSource(), true).initialize();
	}

	public void beforeTestWithoutSchema() throws Exception {
		beforeTest(null);
	}

	public void afterTest() throws Exception {
		if (pool != null) {
			stopPool(pool);
			JdbcActivator.getInstance().setDataSource(null);
		}
		if (dataPool != null) {
			stopPool(dataPool);
			JdbcActivator.getInstance().setMailboxDataSource(Collections.emptyMap());
		}
		for (Pool other : otherPools) {
			stopPool(other);
		}
		otherPools.clear();
		// to ensure finalize overrides are executed
		System.gc();
	}

	private void stopPool(Pool pool) throws Exception {

		if (pool == null) {
			return;
		}
		if (pool.getDataSource().isClosed()) {
			return;
		}

		if (schemaName != null) {
			Connection conn = null;
			Statement st = null;

			try {
				conn = pool.getConnection();
				if (conn != null) {
					st = conn.createStatement();
					st.execute("DROP SCHEMA " + schemaName + " CASCADE");
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				JdbcHelper.cleanup(conn, null, st);
			}
		}
		pool.stop();
		pool = null;
	}

	public DataSource getDataSource() {
		return pool.getDataSource();
	}

	public static JdbcTestHelper getInstance() {
		return instance;
	}

	public DbSchemaService getDbSchemaService() {
		return DbSchemaService.getService(pool.getDataSource(), true);

	}

	public DataSource getMailboxDataDataSource() {
		return dataPool.getDataSource();
	}

}
