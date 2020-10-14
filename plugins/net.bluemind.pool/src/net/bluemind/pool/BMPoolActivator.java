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
package net.bluemind.pool;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.bluemind.config.InstallationId;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.pool.impl.BmConfIni;

/**
 * Creates a connection to the BM database referenced in the
 * <code>/etc/bm/bm.ini</code> file.
 * 
 * 
 */
public class BMPoolActivator extends Plugin {

	// The shared instance
	private static BMPoolActivator plugin;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Pool defaultPool;

	private Map<String, Pool> dataPool;

	private List<IJDBCDriver> factories;

	private static final boolean defaultInSchema = new File(System.getProperty("user.home") + "/core2.in.schema")
			.exists();

	private List<IPoolListener> listeners;

	/**
	 * The constructor
	 */
	public BMPoolActivator() {
		dataPool = new HashMap<String, Pool>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);

		RunnableExtensionLoader<IJDBCDriver> rel = new RunnableExtensionLoader<IJDBCDriver>();
		factories = rel.loadExtensions("net.bluemind.pool", "jdbcdriver", "jdbc_driver", "implementation");
		listeners = new LinkedList<IPoolListener>();

		try {
			defaultPool = startPool();
		} catch (Exception e) {
			logger.error("error during default pool initialization {}", e.getMessage());
		}

		plugin = this;
	}

	public void restartDefaultPool() throws Exception {
		if (defaultPool != null) {
			destroy();
		}

		defaultPool = startPool();
	}

	private Pool startPool() throws Exception {
		BmConfIni oci = new BmConfIni();
		String dbType = oci.get("dbtype");
		String login = oci.get("user");
		String password = oci.get("password");
		String dbName = oci.get("db");
		String dbHost = oci.get("host");
		int poolSize = poolSize();

		return startPool(dbType, login, password, dbHost, dbName, poolSize);
	}

	private int poolSize() {
		BmConfIni oci = new BmConfIni();
		return Optional.ofNullable(oci.get("dbpoolsize")).map(this::poolSizeAsInt)
				.orElseGet(() -> 2 + 2 * Runtime.getRuntime().availableProcessors());
	}

	private Integer poolSizeAsInt(String poolSize) {
		try {
			return Integer.valueOf(poolSize);
		} catch (NumberFormatException e) {
			return null;
		}

	}

	public Pool startPool(String dbType, String login, String password, String dbHost, String dbName) throws Exception {
		return startPool(dbType, login, password, dbHost, dbName, poolSize());
	}

	public Pool startPool(String dbType, String login, String password, String dbHost, String dbName, int poolSize)
			throws Exception {
		String schemaName = defaultInSchema ? InstallationId.getIdentifier().replace('-', '_') : null;
		logger.info("startPool with schema {}", schemaName);
		return newPool(dbType, login, password, dbName, dbHost, poolSize, schemaName);
	}

	public Pool newPool(String dbType, String login, String password, String dbName, String dbHost, int poolSize,
			String schema) throws Exception {
		logger.info("Starting connection pool {}/{}, schema: {}, dbtype: {}", dbHost, dbName, schema, dbType);
		ClassLoader threadContextCl = Thread.currentThread().getContextClassLoader();
		try {

			IJDBCDriver cf = getDriver(dbType);
			if (cf == null) {
				logger.error("No connection factory found for dbtype {}", dbType);
				throw new Exception("No connection factory found for dbtype " + dbType);
			} else {
				String jdbcUrl = cf.getJDBCUrl(dbHost, dbName, login, password);
				String lastIdQuery = cf.getLastInsertIdQuery();

				// try to detect we are running in eclipse, on host
				String os = System.getProperty("os.name");
				if ("Mac OS X".equalsIgnoreCase(os)) {
					poolSize = Math.min(poolSize, 6);
				}
				// TODO: add test suitable for linux devs

				Object driver = Class.forName(cf.getDriverClass()).newInstance();
				Thread.currentThread().setContextClassLoader(driver.getClass().getClassLoader());

				HikariConfig config = new HikariConfig();
				config.setJdbcUrl(jdbcUrl);
				config.setUsername(login);
				config.setPassword(password);
				config.setPoolName(dbName);

				config.setDriverClassName(cf.getDriverClass());
				config.addDataSourceProperty("cachePrepStmts", "true");
				config.addDataSourceProperty("prepStmtCacheSize", "250");
				config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
				config.setConnectionTestQuery(cf.getKeepAliveQuery());
				if (schema != null) {
					config.setSchema(schema);
					config.setConnectionInitSql("CREATE SCHEMA IF NOT EXISTS " + schema);
				}
				config.setMaximumPoolSize(poolSize);

				long start = System.currentTimeMillis();
				do {
					try {
						HikariDataSource ds = new HikariDataSource(config);
						logger.info("Got DS {}", ds);

						return new Pool(lastIdQuery, ds);
					} catch (Exception e) {
						logger.warn("Pool {} startup problem: {}, retrying in 2sec", jdbcUrl, e.getMessage());
						Thread.sleep(2000);
					}
				} while (System.currentTimeMillis() - start < TimeUnit.MINUTES.toMillis(2));
				logger.error("Pool startup for {} failed, exiting.", jdbcUrl);
				System.exit(1);
				return null; // we exited...
			}
		} catch (Exception t) {
			logger.error("Unable to connect to pool {}/{}, schema: {}, dbtype: {}", dbHost, dbName, schema, dbType, t);
			throw t;
		} finally {
			// restore previous context classloader
			Thread.currentThread().setContextClassLoader(threadContextCl);
		}
	}

	private IJDBCDriver getDriver(String dbType) {
		IJDBCDriver cf = null;
		for (IJDBCDriver icf : factories) {
			if (icf.getSupportedDbType().equalsIgnoreCase(dbType)) {
				cf = icf;
				break;
			}
		}
		return cf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// defaultPool.stop();

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static BMPoolActivator getDefault() {
		return plugin;
	}

	public Connection getConnection() {
		return defaultPool.getConnection();
	}

	public int lastInsertId(Connection con) throws SQLException {
		return defaultPool.lastInsertId(con);
	}

	public static void cleanup(Connection con, Statement st, ResultSet rs) {
		try {
			rs.close();
		} catch (Exception e) {
		}
		try {
			st.close();
		} catch (Exception e) {
		}
		try {
			con.close();
		} catch (Exception e) {
		}
	}

	public Pool defaultPool() {
		return defaultPool;
	}

	public Pool dataPool(String datalocation) {
		return dataPool.get(datalocation);
	}

	public void addListener(IPoolListener hl) {
		logger.info("****** Adding destroy handler: " + hl + " ******");
		listeners.add(hl);
	}

	public void destroy() {
		logger.info("Destroy pool");
		try {
			defaultPool.stop();
			for (IPoolListener hl : listeners) {
				hl.poolDestroyed(defaultPool);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public void addMailboxDataSource(String uid, Pool p) {
		dataPool.put(uid, p);
	}

}