/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.maintenance.postgresql;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.maintenance.IMaintenanceScript;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class Repack implements IMaintenanceScript {
	private static final long TIMEOUT_HOURS = 6;
	private static final int DEFAULT_CHANGESET_PARTITION_COUNT = 256;

	private static final Logger logger = LoggerFactory.getLogger(Repack.class);
	private static final Map<DataSource, Integer> dsPartitionCount = new HashMap<>();

	interface CompletionHandler {
		public void onCompleted(int exitCode);
	}

	public static class MonitorProcessHandler implements ProcessHandler {
		private final CompletableFuture<Integer> promise;
		private final IServerTaskMonitor monitor;
		private final CompletionHandler completionHandler;
		private String taskRef;

		public MonitorProcessHandler(IServerTaskMonitor monitor, CompletionHandler completionHandler) {
			this.completionHandler = completionHandler;
			this.monitor = monitor;
			this.promise = new CompletableFuture<>();
		}

		@Override
		public void log(String message) {
			monitor.log(taskRef != null ? ("[" + taskRef + "]: " + message) : message);
		}

		@Override
		public void completed(int exitCode) {
			monitor.end(exitCode == 0, null, null);
			if (completionHandler != null) {
				completionHandler.onCompleted(exitCode);
			}
			promise.complete(exitCode);
		}

		@Override
		public void starting(String taskRef) {
			this.taskRef = taskRef;
		}

		public CompletableFuture<Integer> promise() {
			return promise;
		}

	}

	private int getParitionCount(DataSource ds, IServerTaskMonitor monitor) {
		return dsPartitionCount.computeIfAbsent(ds, dsc -> {
			String partCountQuery = "SELECT COALESCE(current_setting('bm.changeset_partitions', true)::integer, 256) AS partition_count";
			try {
				try (ResultSet rs = executeSql(dsc, monitor, partCountQuery, false)) {
					if (rs != null && rs.next()) {
						return rs.getInt(1);
					}
				}
			} catch (SQLException e) {
				logger.error("Unable to retrieve partition count from database", e);
			}
			return DEFAULT_CHANGESET_PARTITION_COUNT;
		});
	}

	private void setColumnNotNullNonBlocking(DataSource ds, IServerTaskMonitor monitor, String tableName,
			String columnName) {
		// Temporary check constraint
		monitor.log("alter table " + tableName + " set column " + columnName + " not null");
		String constraintName = tableName + "_" + columnName + "_notnull_check_temp";
		List<String> queries = Arrays.asList(
				"ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName + " CHECK (" + columnName
						+ " IS NOT NULL) NOT VALID",
				"ALTER TABLE " + tableName + " VALIDATE CONSTRAINT " + constraintName,
				"ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " SET NOT NULL",
				"ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
		try {
			try (Connection con = ds.getConnection()) {
				for (String query : queries) {
					try (Statement st = con.createStatement()) {
						logger.debug(query);
						st.execute(query);
					}
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error", e);
			monitor.log("SQL exception: " + e);
		}
	}

	private boolean isColumnNotNull(DataSource ds, IServerTaskMonitor monitor, String tableName, String columnName) {
		try {
			logger.debug("check column not null {}({})", tableName, columnName);
			String checkQuery = "SELECT is_nullable FROM information_schema.columns WHERE table_name=? AND column_name=? LIMIT 1";
			try (Connection con = ds.getConnection(); PreparedStatement st = con.prepareStatement(checkQuery)) {
				st.setString(1, tableName);
				st.setString(2, columnName);
				try (ResultSet rs = st.executeQuery()) {
					if (rs.next()) {
						logger.debug("check column not null {}({}) result: {}", tableName, columnName, rs.getString(1));
						return !("YES".equals(rs.getString(1)));
					} else {
						monitor.log("Unable to check table " + tableName + "(" + columnName + ")");
					}
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error", e);
			monitor.log("SQL exception: " + e);
		}
		return false;
	}

	public void setColumnNotNull(DataSource ds, IServerTaskMonitor monitor, String tableName, String columnName) {
		if (isColumnNotNull(ds, monitor, tableName, columnName)) {
			setColumnNotNullNonBlocking(ds, monitor, tableName, columnName);
		}
	}

	private boolean createRepackIndexes(DataSource ds, IServerTaskMonitor monitor) {
		boolean indexesCreated = false;
		for (int i = 0; i < getParitionCount(ds, monitor); i++) {
			String tableName = "t_container_changeset_" + i;
			String indexName = tableName + "_unique_idx";
			boolean createIndex = false;
			String checkIndexQuery = "SELECT ix.indisvalid AS isvalid " //
					+ "FROM pg_class t, pg_class i, pg_index ix, pg_attribute a " //
					+ "WHERE t.oid = ix.indrelid and i.oid = ix.indexrelid AND a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " //
					+ "AND t.relname = '" + tableName + "'" //
					+ "AND i.relname = '" + indexName + "'";
			try {
				try (ResultSet checkRs = executeSql(ds, monitor, checkIndexQuery, false)) {
					if (checkRs != null && checkRs.next()) {
						boolean isvalid = checkRs.getBoolean(1);
						if (!isvalid) {
							try (ResultSet droprs = executeSql(ds, monitor, "drop index " + indexName, true)) {
								createIndex = true;
							}
						}
					} else {
						createIndex = true;
					}
				}
				if (createIndex) {
					String createIndexQuery = "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS " //
							+ indexName + " ON " + tableName //
							+ "(item_id, version, container_id)";
					try (ResultSet rsCreate = executeSql(ds, monitor.subWork("create index on partition " + i, 1),
							createIndexQuery, true)) {
						indexesCreated |= true;
					}
				}
			} catch (SQLException e) {
				monitor.log("SQL exception: " + e.getMessage());
				logger.error("SQL exception", e);
			}

		}
		return indexesCreated;
	}

	public void run(IServerTaskMonitor monitor) {
		monitor.begin(1, "pg_repack");

		// First check if unique index on t_container_changeset exists
		Set<DataSource> datasources = new HashSet<>();
		datasources.add(ServerSideServiceProvider.defaultDataSource);
		datasources.addAll(ServerSideServiceProvider.mailboxDataSource.values());

		boolean indexesCreated = false;
		for (DataSource ds : datasources) {
			logger.debug("pg_repack check datasource {}", ds);
			setColumnNotNull(ds, monitor.subWork(ds.toString() + " check unique constraints", 1),
					"t_container_changelog", "item_id");
			setColumnNotNull(ds, monitor.subWork(ds.toString() + " check unique constraints", 1),
					"t_container_changeset", "item_id");
			indexesCreated |= createRepackIndexes(ds, monitor.subWork(ds.toString(), 1));
		}
		if (indexesCreated) {
			monitor.log("pg_repack skipped: indexes just created");
			return;
		}

		IServer service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext().provider()
				.instance(IServer.class, "default");

		List<Server> servers = service.allComplete().stream()
				.filter(ivs -> ivs.value.tags.contains(TagDescriptor.bm_pgsql.getTag())
						|| ivs.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag()))
				.map(ivs -> ivs.value).collect(Collectors.toList());
		logger.info("pg_repack servers: {}", servers);
		List<MonitorProcessHandler> processes = new ArrayList<>();
		for (Server server : servers) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			List<String> dbNames = new ArrayList<>();
			if (server.tags.contains(TagDescriptor.bm_pgsql.getTag())) {
				dbNames.add("bj");
			}
			if (server.tags.contains(TagDescriptor.bm_pgsql_data.getTag())) {
				dbNames.add("bj-data");
			}
			StringBuilder sb = new StringBuilder();
			sb.append("#!/bin/sh\n\n");
			sb.append("set -e\n");
			sb.append("export PGPASSWORD=bj PGUSER=bj PGHOST=localhost\n");
			for (String dbName : dbNames) {
				sb.append("partitionCount=$(psql -d " + dbName
						+ " -AtqE -c \"SELECT COALESCE(current_setting('bm.changeset_partitions', true)::integer, "
						+ DEFAULT_CHANGESET_PARTITION_COUNT + ") -1 AS partition_count;\")\n");
				sb.append("for i in $(seq 0 ${partitionCount}); do\n");
				sb.append("  pg_repack -d " + dbName + " -t \"t_container_changeset_${i}\"\n");
				sb.append("done\n");
			}
			String scriptPath = "/tmp/maintenance_repack_" + System.nanoTime() + ".sh";
			nodeClient.writeFile(scriptPath, new ByteArrayInputStream(sb.toString().getBytes()));
			NCUtils.exec(nodeClient, "chmod +x " + scriptPath);
			MonitorProcessHandler ph = new MonitorProcessHandler(monitor.subWork(server.ip, 1),
					exitcode -> nodeClient.deleteFile(scriptPath));
			processes.add(ph);
			nodeClient.asyncExecute(ExecRequest.anonymous(scriptPath), ph);
		}

		try {
			CompletableFuture
					.allOf(processes.stream().map(MonitorProcessHandler::promise).toArray(CompletableFuture[]::new))
					.get(TIMEOUT_HOURS, TimeUnit.HOURS);
		} catch (InterruptedException ie) {
			logger.error("interrupted", ie);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			logger.error("execution error", e);
			monitor.log("Failed to complete all pg_repacks: execution error: " + e.getMessage());
		} catch (TimeoutException e) {
			logger.error("Timeout", e);
			monitor.log("Failed to complete all pg_repacks: Timeout after " + TIMEOUT_HOURS + " hours");
		}
	}

	private ResultSet executeSql(DataSource ds, IServerTaskMonitor monitor, String query, boolean log) {
		long start = System.currentTimeMillis();
		try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
			st.execute(query);
			SQLWarning warn = st.getWarnings();
			while (warn != null) {
				monitor.log(warn.getMessage());
				warn = warn.getNextWarning();
			}
			if (log) {
				monitor.end(true, query + " took " + (System.currentTimeMillis() - start) + " ms to execute", null);
			} else {
				monitor.end(true, null, null);
			}
			return st.getResultSet();
		} catch (SQLException e) {
			monitor.log("error exexuting " + query + ": " + e);
			monitor.end(false, null, e.getMessage());
			return null;
		}
	}
}
