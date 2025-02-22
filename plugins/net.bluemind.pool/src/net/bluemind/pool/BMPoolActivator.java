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

import java.sql.Connection;
import java.sql.ResultSet;
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

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.bluemind.configfile.core.CoreConfig;
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

	private List<IPoolListener> listeners;

	/**
	 * The constructor
	 */
	public BMPoolActivator() {
		dataPool = new HashMap<>();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		RunnableExtensionLoader<IJDBCDriver> rel = new RunnableExtensionLoader<>();
		factories = rel.loadExtensions("net.bluemind.pool", "jdbcdriver", "jdbc_driver", "implementation");
		listeners = new LinkedList<>();

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
		return newPool(dbType, login, password, dbName, dbHost, poolSize, null);
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

				// try to detect we are running in eclipse, on host
				String os = System.getProperty("os.name");
				if ("Mac OS X".equalsIgnoreCase(os)) {
					poolSize = Math.min(poolSize, 6);
				}

				Object driver = Class.forName(cf.getDriverClass()).getDeclaredConstructor().newInstance();
				Thread.currentThread().setContextClassLoader(driver.getClass().getClassLoader());

				Config coreConfig = CoreConfig.get();

				HikariConfig config = new HikariConfig();
				config.setMetricsTrackerFactory(new SpectatorMetricsTrackerFactory());
				config.setJdbcUrl(jdbcUrl);
				config.setUsername(login);
				config.setPassword(password);
				config.setPoolName(dbName + "@" + dbHost);

				config.setDriverClassName(cf.getDriverClass());
				// List of settings for PostgreSQL JDBC:
				// https://jdbc.postgresql.org/documentation/use/
				config.addDataSourceProperty("sslmode", coreConfig.getString(CoreConfig.PostgreSQL.SSL_MODE));
				config.addDataSourceProperty("preparedStatementCacheQueries",
						coreConfig.getInt(CoreConfig.PostgreSQL.PREPARED_STATEMENT_CACHE_QUERIES));
				config.addDataSourceProperty("preparedStatementCacheSizeMiB",
						coreConfig.getInt(CoreConfig.PostgreSQL.PREPARED_STATEMENT_CACHE_SIZE_MIB));
				config.addDataSourceProperty("preferQueryMode",
						coreConfig.getString(CoreConfig.PostgreSQL.PREFER_QUERY_MODE));
				config.addDataSourceProperty("defaultRowFetchSize",
						coreConfig.getInt(CoreConfig.PostgreSQL.DEFAULT_ROW_FETCHSIZE));
				config.addDataSourceProperty("ApplicationName",
						coreConfig.getString(CoreConfig.PostgreSQL.APPLICATION_NAME));
				config.addDataSourceProperty("reWriteBatchedInserts",
						String.valueOf(coreConfig.getBoolean(CoreConfig.PostgreSQL.REWRITE_BATCHED_INSERTS)));
				// Avoids a nasty PostgreSQL memory leak by not reusing backends for more than 5
				// minutes
				config.setMaxLifetime(
						coreConfig.getDuration(CoreConfig.PostgreSQL.MAX_LIFETIME, TimeUnit.MILLISECONDS));

				// config.setConnectionTestQuery(cf.getKeepAliveQuery());
				// LC: if we do server side prepared statement,
				// on servers with a "big" t_directory_entry, the query
				// to check if an email exists becomes suuuper slow, because
				// postgresql uses a generic plan, which is not suitable for us
				config.addDataSourceProperty("prepareThreshold",
						String.valueOf(coreConfig.getInt(CoreConfig.PostgreSQL.PREPARE_THRESHOLD)));
				if (schema != null) {
					config.setSchema(schema);
					config.setConnectionInitSql("CREATE SCHEMA IF NOT EXISTS " + schema);
				}
				config.setMaximumPoolSize(poolSize);
				config.setLeakDetectionThreshold(
						coreConfig.getDuration(CoreConfig.PostgreSQL.LEAK_DETECTION_THRESHOLD, TimeUnit.MILLISECONDS));

				long startupTimeout = System.nanoTime()
						+ coreConfig.getDuration(CoreConfig.PostgreSQL.STARTUP_TIMEOUT, TimeUnit.NANOSECONDS);
				do {
					try {
						HikariDataSource ds = new HikariDataSource(config);
						logger.info("Got DS {}", ds);

						return new Pool(ds);
					} catch (Exception e) {
						logger.warn("Pool {} startup problem: {}, retrying in 2sec", jdbcUrl, e.getMessage());
						Thread.sleep(2000);
					}
				} while (System.nanoTime() < startupTimeout);
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

	@Override
	public void stop(BundleContext context) throws Exception {
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
		logger.info("****** Adding destroy handler: {}  ******", hl);
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