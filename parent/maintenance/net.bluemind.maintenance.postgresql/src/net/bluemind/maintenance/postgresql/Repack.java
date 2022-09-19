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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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
	private static final long TIMEOUT_HOURS = 8;
	private static final int DEFAULT_CHANGESET_PARTITION_COUNT = 256;
	private static final int DEFAULT_CONVERSATION_REFERENCE_PARTITION_COUNT = 256;
	private static final int DEFAULT_CONVERSATION_PARTITION_COUNT = 25;

	private static final Logger logger = LoggerFactory.getLogger(Repack.class);

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
		public void log(String message, boolean cont) {
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

	public void run(IServerTaskMonitor monitor) {
		monitor.begin(1, "pg_repack");

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

			if (!nodeClient.listFiles("/etc/bm/pg.need.postupgrade").isEmpty()) {
				continue;
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
				sb.append("  pg_repack --wait-timeout " + TimeUnit.HOURS.toSeconds(4) + " -d " + dbName
						+ " -t \"t_container_changeset_${i}\"\n");
				sb.append("done\n");

				// Conversations
				sb.append("conversationsPartitionCount=$(psql -d " + dbName
						+ " -AtqE -c \"SELECT COALESCE(current_setting('bm.conversation_partitions', true)::integer, "
						+ DEFAULT_CONVERSATION_PARTITION_COUNT + ") -1 AS partition_count;\")\n");
				sb.append("for i in $(seq 0 ${conversationsPartitionCount}); do\n");
				sb.append("  pg_repack --wait-timeout " + TimeUnit.HOURS.toSeconds(4) + " -d " + dbName
						+ " -t \"t_conversation_${i}\"\n");
				sb.append("done\n");

				// Conversation reference
				sb.append("conversationsReferencePartitionCount=$(psql -d " + dbName
						+ " -AtqE -c \"SELECT COALESCE(current_setting('bm.conversationreference_partitions', true)::integer, "
						+ DEFAULT_CONVERSATION_REFERENCE_PARTITION_COUNT + ") -1 AS partition_count;\")\n");
				sb.append("for i in $(seq 0 ${conversationsReferencePartitionCount}); do\n");
				sb.append("  pg_repack --wait-timeout " + TimeUnit.HOURS.toSeconds(4) + " -d " + dbName
						+ " -t \"t_conversationreference_${i}\"\n");
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

	@Override
	public String name() {
		return "pgRepack";
	}
}
