/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.service.internal;

import java.io.PrintWriter;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class TransactionalContext implements BmContext {
	private SecurityContext securityContext;
	private UnpooledDataSource dataSource;
	private final Map<String, UnpooledDataSource> mailboxDataSources = new HashMap<>();
	private final List<Connection> connections = new ArrayList<>();

	private static final Logger logger = LoggerFactory.getLogger(TransactionalContext.class);

	private static class NotClosableConnection implements Connection {
		private final Connection c;

		public NotClosableConnection(Connection conn) {
			c = conn;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return c.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return c.isWrapperFor(iface);
		}

		@Override
		public Statement createStatement() throws SQLException {
			return c.createStatement();
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return c.prepareStatement(sql);
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return c.prepareCall(sql);
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return c.nativeSQL(sql);
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			c.setAutoCommit(autoCommit);
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return c.getAutoCommit();
		}

		@Override
		public void commit() throws SQLException {
			c.commit();
		}

		@Override
		public void rollback() throws SQLException {
			c.rollback();
		}

		@Override
		public void close() throws SQLException {
			if (c.getAutoCommit()) {
				c.close();
			}
		}

		@Override
		public boolean isClosed() throws SQLException {
			return c.isClosed();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return c.getMetaData();
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
			c.setReadOnly(readOnly);
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return c.isReadOnly();
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			c.setCatalog(catalog);
		}

		@Override
		public String getCatalog() throws SQLException {
			return c.getCatalog();
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			c.setTransactionIsolation(level);
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return c.getTransactionIsolation();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return c.getWarnings();
		}

		@Override
		public void clearWarnings() throws SQLException {
			c.clearWarnings();
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return c.createStatement(resultSetType, resultSetConcurrency);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return c.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return c.prepareCall(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return c.getTypeMap();
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			c.setTypeMap(map);
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
			c.setHoldability(holdability);
		}

		@Override
		public int getHoldability() throws SQLException {
			return c.getHoldability();
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return c.setSavepoint();
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return c.setSavepoint(name);
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
			c.rollback(savepoint);
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			c.releaseSavepoint(savepoint);
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			return c.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return c.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return c.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return c.prepareStatement(sql, autoGeneratedKeys);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return c.prepareStatement(sql, columnIndexes);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return c.prepareStatement(sql, columnNames);
		}

		@Override
		public Clob createClob() throws SQLException {
			return c.createClob();
		}

		@Override
		public Blob createBlob() throws SQLException {
			return c.createBlob();
		}

		@Override
		public NClob createNClob() throws SQLException {
			return c.createNClob();
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return c.createSQLXML();
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return c.isValid(timeout);
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
			c.setClientInfo(name, value);
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
			c.setClientInfo(properties);
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return c.getClientInfo(name);
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return c.getClientInfo();
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return c.createArrayOf(typeName, elements);
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return c.createStruct(typeName, attributes);
		}

		@Override
		public void setSchema(String schema) throws SQLException {
			c.setSchema(schema);
		}

		@Override
		public String getSchema() throws SQLException {
			return c.getSchema();
		}

		@Override
		public void abort(Executor executor) throws SQLException {
			c.abort(executor);
		}

		@Override
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
			c.setNetworkTimeout(executor, milliseconds);
		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			return c.getNetworkTimeout();
		}

		public String toString() {
			return c.toString();
		}
	}

	private static class UnpooledDataSource implements DataSource {
		private final DataSource wrappedDs;
		private final Connection connection;
		private final Connection unwrappedConnection;

		public UnpooledDataSource(DataSource wrappedDs) throws SQLException {
			this.wrappedDs = wrappedDs;
			unwrappedConnection = wrappedDs.getConnection();
			this.connection = new NotClosableConnection(unwrappedConnection);
			this.connection.setAutoCommit(false);
		}

		public Connection getUnwrappedConnection() {
			return unwrappedConnection;
		}

		public String toString() {
			return wrappedDs.toString();
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return this.wrappedDs.getLogWriter();
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.wrappedDs.setLogWriter(out);
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			this.wrappedDs.setLoginTimeout(seconds);
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return this.wrappedDs.getLoginTimeout();
		}

		@Override
		public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return this.wrappedDs.getParentLogger();
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return this.wrappedDs.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return this.wrappedDs.isWrapperFor(iface);
		}

		@Override
		public Connection getConnection() throws SQLException {
			return this.connection;
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			throw new UnsupportedOperationException();
		}
	}

	public TransactionalContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
		BmConfIni oci = new BmConfIni();
		String dbLogin = oci.get("user");
		String dbPassword = oci.get("password");
		String dbName = oci.get("db");
		String dbHost = oci.get("host");

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		List<ItemValue<Server>> servers = serverService.allComplete();

		try {
			PGSimpleDataSource ds = new PGSimpleDataSource();
			ds.setServerNames(new String[] { dbHost });
			ds.setDatabaseName(dbName);
			ds.setUser(dbLogin);
			ds.setPassword(dbPassword);
			dataSource = new UnpooledDataSource(ds);
			connections.add(dataSource.getUnwrappedConnection());
		} catch (Exception e) {
			logger.error("Unable to start directory transactional pool: {}", e.getMessage(), e);
		}
		servers.stream().filter(ivserver -> ivserver.value.tags.contains("bm/pgsql-data")).forEach(ivserver -> {
			try {
				PGSimpleDataSource dataDs = new PGSimpleDataSource();
				dataDs.setServerNames(new String[] { ivserver.value.ip });
				dataDs.setDatabaseName("bj-data");
				dataDs.setUser(dbLogin);
				dataDs.setPassword(dbPassword);
				UnpooledDataSource unpooledDataSource = new UnpooledDataSource(dataDs);
				mailboxDataSources.put(ivserver.uid, unpooledDataSource);
				connections.add(unpooledDataSource.getUnwrappedConnection());
			} catch (Exception e) {
				logger.error("Unable to start shard transactional pool: {}", e.getMessage(), e);
			}
		});
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public IServiceProvider getServiceProvider() {
		return ServerSideServiceProvider.getProvider(this);
	}

	@Override
	public IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(this);
	}

	@Override
	public BmContext su() {
		return this;
	}

	@Override
	public BmContext su(String userUid, String domainUid) {
		return this;
	}

	@Override
	public BmContext su(String sid, String userUid, String domainUid) {
		return this;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(TransactionalContext.class)//
				.add("sec", securityContext)//
				.toString();
	}

	@Override
	public BmContext withRoles(Set<String> roles) {
		return this;
	}

	@Override
	public DataSource getMailboxDataSource(String datalocation) {
		return mailboxDataSources.get(datalocation);
	}

	@Override
	public List<DataSource> getAllMailboxDataSource() {
		List<DataSource> ret = new ArrayList<>();
		ret.addAll(mailboxDataSources.values());
		return ret;
	}

	@Override
	public String dataSourceLocation(DataSource ds) {
		if (ds == dataSource) {
			return "dir";
		} else {
			return mailboxDataSources.entrySet().stream().filter(e -> e.getValue() == ds).map(Entry::getKey).findAny()
					.orElse(null);
		}
	}

	public void stop() {
		connections.stream().forEach(c -> {
			try {
				c.close();
			} catch (SQLException e) {
			}
		});
	}
}
