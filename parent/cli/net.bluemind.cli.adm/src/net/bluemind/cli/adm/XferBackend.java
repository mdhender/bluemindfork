/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.adm;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.core.streams.ReadStream;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.DirEntryTargetFilter;
import net.bluemind.cli.directory.common.DirEntryTargetFilter.DirEntryWithDomain;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "xfer", description = "xfer users from one backend to another backend")
public class XferBackend implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return XferBackend.class;
		}
	}

	@Option(names = "--dry", description = "don't write antyhing, just print the todolist")
	public Boolean dry = false;

	@Option(names = "--target-backend", description = "xfer all matching users to specified backend (server uid, ip or name)", required = true)
	public String targetBackendId;

	@Option(names = "--source-backend", description = "xfer all matching users from specified backend only (server uid, ip or name)")
	public String sourceBackendId;

	@Option(names = "--workers", description = "run with X workers")
	public int workers = 1;

	@Option(names = "--match", description = "regex that entity must match, for example : [a-c].*")
	public String match = "";

	@Parameters(paramLabel = "<target>", description = "email address, domain name or 'all' for all domains")
	public String target;

	private List<ItemValue<Server>> imapServers;
	private CliContext ctx;

	@Override
	public void run() {
		imapServers = ctx.adminApi().instance(IServer.class, "default").allComplete().stream()
				.filter(s -> s.value.tags.contains("mail/imap")).collect(Collectors.toList());

		Optional<ItemValue<Server>> targetBackend = getImapServer(targetBackendId);
		Optional<ItemValue<Server>> sourceBackend = getImapServer(sourceBackendId);

		if (!targetBackend.isPresent()) {
			ctx.error("Target backend {} not found. Available servers: {}", targetBackendId,
					imapServers.stream().map(s -> s.value.ip).collect(Collectors.toList()));
			return;
		}
		if (!sourceBackend.isPresent() && sourceBackendId != null && !sourceBackendId.isEmpty()) {
			ctx.error("Source backend {} not found. Available servers: {}", sourceBackendId,
					imapServers.stream().map(s -> s.value.ip).collect(Collectors.toList()));
			return;
		}

		DirEntryTargetFilter targetFilter = new DirEntryTargetFilter(ctx, target,
				new Kind[] { Kind.USER, Kind.GROUP, Kind.MAILSHARE }, match);

		ArrayBlockingQueue<ItemValue<DirEntry>> q = new ArrayBlockingQueue<>(workers);
		ExecutorService pool = Executors.newFixedThreadPool(workers);

		for (DirEntryWithDomain dirEntryWithDomain : targetFilter.getEntries()) {
			try {
				ItemValue<DirEntry> dirEntry = dirEntryWithDomain.dirEntry;
				ItemValue<Server> targetServer = targetBackend.get();
				final String logId = dirEntry.value.kind.name() + ": "
						+ (Kind.GROUP.equals(dirEntry.value.kind) ? dirEntry.displayName : dirEntry.value.email);

				if (dirEntry.value.dataLocation.isEmpty()) {
					ctx.info("{} has no dataLocation specified ?!: Can't migrate", logId);
					continue;
				}

				if (dirEntry.value.dataLocation.equals(targetServer.uid)) {
					ctx.info("{} already on backend {}: no xfer required", logId, targetBackendId);
					continue;
				}

				if (sourceBackend.isPresent() && !dirEntry.value.dataLocation.equals(sourceBackend.get().uid)) {
					ctx.info("{} is not currently located on backend {}: no xfer", logId, sourceBackendId);
					continue;
				}

				if (Boolean.TRUE.equals(dry)) {
					ctx.info("Dry mode enabled: not transferring {}", logId);
					continue;
				}
				try {
					q.put(dirEntry); // block until a slot is free
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
				ctx.info("xfer {} to backend {}", logId, targetBackendId);
				pool.submit(() -> {
					TaskRef taskref = null;

					CliRepair clirepair = new CliRepair(ctx, dirEntryWithDomain.domainUid, dirEntry, true, dry);
					try {
						if (dirEntry.value.archived) {
							clirepair.repair();
						}

						try {
							taskref = doXfer(logId, dirEntry, dirEntryWithDomain.domainUid, targetServer.uid);
						} catch (Exception e) {
							ctx.error("Unable to queue xfer for {}: {}", logId, e.getMessage());
							e.printStackTrace(System.err);
						}
						if (taskref != null) {
							trackTask(logId, taskref);
						}
					} finally {
						clirepair.close();
						q.remove(dirEntry); // NOSONAR
					}
				});
			} catch (Exception e) {
				ctx.error("direntry xfer failed: {}", e.getMessage());
				e.printStackTrace(System.err);
			}
		}
		ctx.info("All users xfer queued or terminated. Waiting for completion... (can take hours)");
		pool.shutdown();
		try {
			pool.awaitTermination(8, TimeUnit.HOURS);
		} catch (InterruptedException e) {
		}
	}

	private void trackTask(String logId, TaskRef taskref) {
		while (true) {
			CompletableFuture<Void> fut = new CompletableFuture<>();

			ITask taskApi = ctx.longRequestTimeoutAdminApi().instance(ITask.class, taskref.id);
			if (taskApi == null) {
				ctx.error("[{}] task id {} is not present in core, was the server restarted?", logId, taskref.id);
				return;
			}
			TaskStatus s = taskApi.status();
			if (s.state.ended) {
				ctx.info("[{}] task id {} ended: {}", logId, taskref.id, s);
				return;
			}

			try {
				ReadStream<Buffer> reader = VertxStream.read(taskApi.log());
				reader.endHandler(x -> {
					ctx.info("[{}] task id {} ended...", logId, taskref.id);
					fut.complete(null);
				});
				reader.handler(JsonParser.newParser().objectValueMode().handler(event -> {
					JsonObject body = event.objectValue();
					String message = body.getString("message");
					if (message != null && !message.equals("")) {
						ctx.info("[{}]: {}", logId, message);
					}
					if (Boolean.TRUE.equals(body.getBoolean("end"))) {
						ctx.info("[{}] ended", logId);
						fut.complete(null);
					}
				}));
				reader.exceptionHandler(t -> {
					ctx.error("[{}] xfer error: {}", logId, t.getMessage());
					if (t.getMessage() == null) {
						t.printStackTrace(System.err);
					}
					fut.completeExceptionally(t);
				});
				ctx.info("[{}] waiting for task to end...", logId);
				try {
					fut.get(4, TimeUnit.HOURS);
					return;
				} catch (TimeoutException te) {
					ctx.error("[{}] xfer error: timeout waiting for task {}", logId, taskref.id);
					throw te;
				}
			} catch (Exception e) {
				ctx.error("[{}] task id {} failed for unknown reason: {}", logId, taskref.id, e.getMessage());
				e.printStackTrace(System.err);
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private TaskRef doXfer(String logId, ItemValue<DirEntry> dirEntry, String domainUid, String dataLocation) {
		IDirectory directoryService = ctx.adminApi().instance(IDirectory.class, domainUid);
		return directoryService.xfer(dirEntry.uid, dataLocation);
	}

	private Optional<ItemValue<Server>> getImapServer(String backendId) {
		if (backendId == null || backendId.isEmpty()) {
			return Optional.empty();
		}
		return imapServers.stream().filter(s -> backendId.equalsIgnoreCase(s.uid)
				|| backendId.equalsIgnoreCase(s.value.ip) || backendId.equalsIgnoreCase(s.value.name)).findFirst();
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
