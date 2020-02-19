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

package net.bluemind.dataprotect.service.internal;

import java.io.ByteArrayInputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.RetentionPolicy;
import net.bluemind.dataprotect.service.BackupPath;
import net.bluemind.dataprotect.service.IBackupWorker;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.IDPContext.IToolConfig;
import net.bluemind.dataprotect.service.IDPContext.IToolSession;
import net.bluemind.dataprotect.service.tool.ToolBootstrap;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SysConfKeys;

public class SaveAllTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(SaveAllTask.class);
	private final BmContext ctx;
	private final DPService dps;
	private final PartGenerationIndex partGenerationIndex;

	private static final String backupRoot = "/var/backups/bluemind";
	private static final String backupTemp = backupRoot + "/temp";
	private static final String backupWork = backupRoot + "/work";

	private enum BackupStatus {
		OK(true, "Backup finished successfully"), //
		WARNING(true, "Backup finished with warnings"), //
		ERROR(false, "Backup finished with errors"), //
		INVALID_STATE(false, "/var/backups/bluemind/ is not suitable for backup.", "Backup finished with errors"), //
		POSTOPS_ERROR(true, "Post backup script ending with error.", "Post backup script ending with error");

		public final boolean state;
		public final String log;
		public final String result;

		private BackupStatus(boolean state, String log) {
			this.state = state;
			this.log = log;
			this.result = log;
		}

		private BackupStatus(boolean state, String log, String result) {
			this.state = state;
			this.log = log;
			this.result = result;
		}
	}

	public static class PartGenerationIndex {
		// Key: part.tag/part.server
		// Value: latest valid part
		private Map<String, PartGeneration> index;

		public PartGenerationIndex(DPService dps) {
			this(dps.getAvailableGenerations());
		}

		/**
		 * Keep latest part by part.tag/part.server among valid generations
		 * 
		 * @param generations
		 */
		public PartGenerationIndex(List<DataProtectGeneration> generations) {
			index = generations.stream().filter(generation -> generation.valid()).map(generation -> generation.parts)
					.flatMap(generationParts -> generationParts.stream())
					.collect(Collectors.groupingBy(this::getIndexKey)).entrySet().stream()
					.collect(Collectors.toMap(Entry::getKey,
							entry -> entry.getValue().stream().collect(
									Collectors.maxBy((part1, part2) -> part1.begin.before(part2.begin) ? -1 : 1))
									.get()));

		}

		private String getIndexKey(PartGeneration partGeneration) {
			logger.info(String.format("PartGeneration: tag:%s, srv:%s, begin:%s", partGeneration.tag,
					partGeneration.server, partGeneration.begin));
			return partGeneration.tag + "/" + partGeneration.server;
		}

		public PartGeneration get(PartGeneration partGeneration) {
			return index.get(getIndexKey(partGeneration));
		}

		public Set<String> getKeys() {
			return index.keySet();
		}
	}

	@SuppressWarnings("serial")
	private class InvalidParentGeneration extends Exception {
		public final String[] missingDirs;

		public InvalidParentGeneration(List<String> missingDirs) {
			this.missingDirs = missingDirs.toArray(new String[0]);
		}

		@Override
		public String getMessage() {
			return String.format("Missing directory from previous backup generation: %s",
					String.join(",", missingDirs));
		}

	}

	/**
	 * @param ctx
	 */
	public SaveAllTask(BmContext ctx, DPService dps) {
		this.ctx = ctx;
		this.dps = dps;

		partGenerationIndex = new PartGenerationIndex(dps);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		IDPContext dpCtx = new DPContext(monitor);

		BackupStatus backupStatus;
		try {
			backupStatus = backup(monitor, dpCtx);
		} finally {
			new CleanBackups(dpCtx).execute();
		}

		monitor.end(backupStatus.state, backupStatus.log, backupStatus.result);
	}

	private BackupStatus backup(IServerTaskMonitor monitor, IDPContext dpCtx) throws Exception {
		List<String> skipTags = getSkipTags();

		IServer serverApi = ctx.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> servers = serverApi.allComplete();

		logger.info("Backup starting for {} servers.", servers.size());
		monitor.begin(5, "Backup starting for " + servers.size() + " servers.");

		if (!checkIntegrity(monitor.subWork(1), serverApi, servers, skipTags)) {
			return BackupStatus.INVALID_STATE;
		}

		checkParentGeneration(monitor.subWork(1), servers, skipTags);

		BackupStatus backupStatus = doBackup(monitor.subWork(1), dpCtx, servers, skipTags);

		removeOldGenerations(monitor.subWork(1));

		backupStatus = runPostBackupLocalScript(monitor.subWork(1), servers, backupStatus);

		logger.info("Backup complete with status: {}", backupStatus);
		return backupStatus;
	}

	private List<String> getSkipTags() {
		ISystemConfiguration sysApi = ctx.provider().instance(ISystemConfiguration.class);
		List<String> skipTags = new ArrayList<>(sysApi.getValues().stringList(SysConfKeys.dpBackupSkipTags.name()));
		skipTags.add("mail/smtp-edge");
		for (String tag : skipTags) {
			logger.debug("Skipping backup of tag {}", tag);
		}
		return skipTags;
	}

	private BackupStatus doBackup(IServerTaskMonitor monitor, IDPContext dpCtx, List<ItemValue<Server>> servers,
			List<String> skipTags) throws Exception {
		InstallationVersion version = ctx.provider().instance(IInstallation.class).getVersion();
		VersionInfo versionInfo = VersionInfo.create(version.softwareVersion, version.versionName);
		DataProtectGeneration dpg = dps.getStore().newGeneration(versionInfo);

		BackupStatus backupStatus = BackupStatus.OK;

		logger.info("New backup generation {}", dpg.id);
		monitor.begin(servers.size(), "Starting backup on all servers");
		try {
			for (ItemValue<Server> server : servers) {
				logger.info("Starting backup on server {}", server.value.address());

				List<String> tags = new ArrayList<>(server.value.tags);
				for (String tag : skipTags) {
					tags.remove(tag);
				}

				backupStatus = doBackupByTags(monitor.subWork(1), dpCtx, server, tags, backupStatus, dpg);

				monitor.progress(1, String.format("Server backup %s done successfully", server.value.address()));
			}
		} catch (SQLException e) {
			logger.error("error during backup, now we do some cleanup before FAILING", e);
			cleanCurrentBackup(dpg, monitor);
			backupStatus = BackupStatus.ERROR;

			monitor.end(false, "Backup ending with errors", "");
		}

		if (backupStatus != BackupStatus.OK) {
			monitor.end(false, "Backup ending with errors", "");
		} else {
			monitor.end(true, "Backup ending successfully", "");
		}

		return backupStatus;
	}

	private BackupStatus doBackupByTags(IServerTaskMonitor monitor, IDPContext dpCtx, ItemValue<Server> serverToBackup,
			List<String> tags, BackupStatus backupStatus, DataProtectGeneration dpg) throws SQLException {
		monitor.begin(2L * tags.size(), String.format("Backup tags %s", String.join(",", tags)));

		for (String tag : tags) {
			logger.info("Looking up worker for {}", tag);

			List<IBackupWorker> workers = Workers.get().stream().filter(worker -> worker.supportsTag(tag))
					.collect(Collectors.toList());

			IServerTaskMonitor workerMonitor = monitor.subWork(1);
			workerMonitor.begin(workers.size(), String.format("Backup tag %s", tag));
			for (IBackupWorker worker : workers) {
				backupStatus = doBackupByTagByWorker(dpCtx, serverToBackup, backupStatus, dpg, tag, worker);
				workerMonitor.progress(1,
						String.format("Backup tag %s with worker %s ending", tag, worker.getClass().getSimpleName()));
			}
			workerMonitor.end(backupStatus == BackupStatus.OK, String.format("Backup of tag %s ending", tag), "");

			monitor.progress(1, String.format("Backup tag %s ending", tag));
		}

		monitor.end(backupStatus == BackupStatus.OK, String.format("Backup of tags %s ending", String.join(",", tags)),
				"");

		return backupStatus;
	}

	private BackupStatus doBackupByTagByWorker(IDPContext dpCtx, ItemValue<Server> serverToBackup,
			BackupStatus backupStatus, DataProtectGeneration dpg, String tag, IBackupWorker worker)
			throws SQLException {
		PartGeneration newPartGeneration = initNewPartGeneration(dpg, serverToBackup, tag, worker);
		PartGeneration prevPartGeneration = partGenerationIndex.get(newPartGeneration);

		PartAllocation partAllocation = new PartAllocation();
		partAllocation.previous = prevPartGeneration;
		partAllocation.next = newPartGeneration;

		newPartGeneration = backup(worker, dpCtx, partAllocation, serverToBackup);
		if (backupStatus != BackupStatus.ERROR && newPartGeneration.withErrors) {
			backupStatus = BackupStatus.ERROR;
		}
		if (backupStatus == BackupStatus.OK && newPartGeneration.withWarnings) {
			backupStatus = BackupStatus.WARNING;
		}

		dps.getStore().updatePart(newPartGeneration);

		return backupStatus;
	}

	private PartGeneration initNewPartGeneration(DataProtectGeneration dpg, ItemValue<Server> srv, String tag,
			IBackupWorker worker) throws SQLException {
		PartGeneration pg = new PartGeneration();
		pg.generationId = dpg.id;
		pg.tag = tag;
		pg.begin = new Date();
		pg.server = srv.uid;
		pg.datatype = worker.getDataType();
		pg.id = dps.getStore().newPart(pg.generationId, pg.tag, pg.server, pg.datatype);
		return pg;
	}

	private void checkParentGeneration(IServerTaskMonitor monitor, List<ItemValue<Server>> servers,
			List<String> skipTags) throws InvalidParentGeneration {
		monitor.begin(servers.size(), "Check parent backup generation");

		for (ItemValue<Server> server : servers) {
			for (String tag : server.value.tags.stream().filter(tag -> !skipTags.contains(tag))
					.collect(Collectors.toList())) {
				PartGeneration pg = new PartGeneration();
				pg.tag = tag;
				pg.server = server.uid;
				PartGeneration prev = partGenerationIndex.get(pg);

				if (prev == null) {
					continue;
				}

				List<String> missingDirs = checkParentGenerationParts(server, tag, prev);

				if (!missingDirs.isEmpty()) {
					monitor.end(false, String.format(
							"Part %s is invalid for parent backup generation on host: %s. Missing directory: %s", tag,
							server.value.address(), String.join(", ", missingDirs)), "");
					throw new InvalidParentGeneration(missingDirs);
				}
			}

			monitor.progress(1,
					String.format("Parent backup generation checked successfully on host: %s", server.value.address()));
		}

		monitor.end(true, "Parent backup generation checked successfully", "");
	}

	private List<String> checkParentGenerationParts(ItemValue<Server> server, String tag, PartGeneration prev) {
		List<String> missingDirs = new ArrayList<>();

		if (Workers.get().stream().anyMatch(worker -> worker.supportsTag(tag))) {
			String dir = BackupPath.get(server, tag) + "/" + prev.id + "/";

			String command = String.format("/usr/bin/test -d %s", dir);
			if (NCUtils.exec(NodeActivator.get(server.value.address()), command).getExitCode() != 0) {
				TaskUtils.wait(ctx.provider(), dps.forget(prev.generationId));
				missingDirs.add(dir);
			}
		}

		return missingDirs;
	}

	/**
	 * Remove current backup parts. Used when current backup fail to remove parts
	 * already done
	 * 
	 * @param dpg
	 * @param monitor
	 * @throws Exception
	 */
	private void cleanCurrentBackup(DataProtectGeneration dpg, IServerTaskMonitor monitor) throws Exception {
		DPService dpService = (DPService) ctx.su().provider().instance(IDataProtect.class);
		ForgetTask install = new ForgetTask(ctx, dpService, dpg);
		install.run(monitor.subWork(1));
	}

	/**
	 * Remove old backup generations keeping at least one valid
	 * 
	 * @param monitor
	 * @throws InterruptedException
	 */
	private void removeOldGenerations(IServerTaskMonitor monitor) {
		List<DataProtectGeneration> gens = dps.getAvailableGenerations().stream() //
				.filter(gen -> !gen.withErrors) //
				.collect(Collectors.toList());
		RetentionPolicy rp = dps.getRetentionPolicy();
		if (rp != null && rp.daily != null) {
			int toRm = gens.size() - rp.daily;
			if (toRm == gens.size()) {
				// keep at least one valid backup
				toRm--;
			}

			if (toRm > 0) {
				monitor.begin(toRm, String.format("Forgot %d old backup generations", toRm));
				for (int i = 0; i < toRm; i++) {
					TaskRef forgetTask = dps.forget(gens.get(i).id);
					TaskUtils.wait(ctx.provider(), forgetTask);
					monitor.progress(1, String.format("Forgot generation from: %s (ID: %d)", gens.get(i).protectionTime,
							gens.get(i).id));
				}
			}
		}

		monitor.end(true, "Old backup generations forgotten", "");
	}

	/**
	 * Run, if exists, post backup local script on each server
	 * 
	 * @param monitor
	 * @param servers
	 * @param backupStatus
	 * @return
	 */
	private BackupStatus runPostBackupLocalScript(IServerTaskMonitor monitor, List<ItemValue<Server>> servers,
			BackupStatus backupStatus) {
		if (backupStatus != BackupStatus.OK) {
			return backupStatus;
		}

		monitor.begin(servers.size(), "Running post backup local script");
		for (ItemValue<Server> server : servers) {
			if (!runPostBackupLocalScript(monitor, server.value.address())) {
				backupStatus = BackupStatus.POSTOPS_ERROR;
			}

			monitor.progress(1, String.format("Post backup local script run on %s", server.value.address()));
		}

		if (backupStatus != BackupStatus.POSTOPS_ERROR) {
			monitor.end(false, "Post backup local script run ending with errors", "");
		} else {
			monitor.end(true, "Post backup local script run ending successfully", "");
		}

		return backupStatus;
	}

	/**
	 * Run, if exists, post backup local script on server
	 * 
	 * @param monitor
	 * @param server
	 * @return
	 */
	private boolean runPostBackupLocalScript(IServerTaskMonitor monitor, String server) {
		String scriptPath = "/usr/bin/bm-post-full-backup.sh";
		try {
			INodeClient nc = NodeActivator.get(server);
			List<FileDescription> files = nc.listFiles(scriptPath);
			if (files != null && files.size() == 1) {
				monitor.log(String.format("Running %s on server %s", scriptPath, server));
				logger.info("Running {} on server {}", scriptPath, server);
				TaskRef tr = nc.executeCommand(scriptPath);

				ExitList out = NCUtils.waitFor(nc, tr);
				out.forEach(logLine -> {
					monitor.log(String.format("%s: %s", scriptPath, logLine));
					logger.info("{}: {}", scriptPath, logLine);
				});

				if (out.getExitCode() != 0) {
					monitor.log(String.format("Error: %s return error code %d on server %s", scriptPath,
							out.getExitCode(), server));
					return false;
				}
			}
		} catch (Exception e) {
			monitor.log(String.format("Error running post-backup script %s on server %s: %s", scriptPath, server,
					e.getMessage()));
			logger.warn(String.format("Error running post-backup script %s on server %s", scriptPath, server), e);
			return false;
		}

		return true;
	}

	private boolean checkIntegrity(IServerTaskMonitor monitor, IServer serverApi, List<ItemValue<Server>> servers,
			List<String> skipTags) {

		monitor.begin(servers.size(), String.format("Checking %s on each hosts", backupRoot));

		String fn = backupTemp + "/check-" + UUID.randomUUID().toString() + ".torm";
		ItemValue<Server> last = null;

		boolean validBackupStore = true;
		for (ItemValue<Server> server : servers) {
			List<Assignment> serverAssignments = serverApi.getServerAssignments(server.uid);
			if (serverAssignments.stream().allMatch(assignment -> skipTags.contains(assignment.tag))) {
				continue;
			}

			INodeClient nc = NodeActivator.get(server.value.ip);
			try {
				if (last == null) {
					NCUtils.execNoOut(nc, "rm -rf " + backupTemp + " " + backupWork);
					NCUtils.execNoOut(nc, "mkdir -p " + backupTemp + " " + backupWork);
					nc.writeFile(fn, new ByteArrayInputStream("YOU CAN SAFELY REMOVE THIS FILE".getBytes()));
				}

				last = server;

				if (!allowedMountPoint(monitor, server, nc) || !sharedDataStore(monitor, server, nc, fn)) {
					validBackupStore = false;
				}

				monitor.progress(1, String.format("%s checked on %s", backupRoot, server.value.address()));
			} catch (Exception e) {
				logger.error(e.getMessage());
				monitor.end(false, String.format("Unable to check %s on %s", backupRoot, server.value.address()), "KO");
				return false;
			}
		}

		if (last != null) {
			serverApi.submitAndWait(last.uid, "rm -f " + fn);
		}

		if (!validBackupStore) {
			monitor.end(validBackupStore, String.format("Invalid state for %s on at least one server", backupRoot),
					"KO");
		} else {
			monitor.end(validBackupStore, String.format("%s is ok on all servers", backupRoot), "OK");
		}

		return validBackupStore;
	}

	/**
	 * Check if backup volume is shared between all BlueMind nodes
	 * 
	 * @param monitor
	 * @param server
	 * @param nc
	 * @param fileName
	 * @return
	 */
	private boolean sharedDataStore(IServerTaskMonitor monitor, ItemValue<Server> server, INodeClient nc,
			String fileName) {
		boolean found = nc.listFiles(fileName).size() == 1;
		if (!found) {
			monitor.log(String.format("%s is not shared on %s", backupRoot, server.value.address()));
			return false;
		}

		return true;
	}

	/**
	 * Check if backup is not stored on a forbidden mount point
	 * 
	 * @param monitor
	 * @param server
	 * @param nc
	 * @return
	 */
	private boolean allowedMountPoint(IServerTaskMonitor monitor, ItemValue<Server> server, INodeClient nc) {
		String statCmd = "/usr/bin/stat --format '%m'";
		String backupMountPoint = backupRoot + "/";
		List<String> forbidenMountPoint = Arrays.asList("/", "/var", "/var/");

		ExitList output = NCUtils.exec(nc, statCmd + " " + backupMountPoint);
		if (output.size() != 1) {
			monitor.log(String.format("Invalid stat command output on server %s", server.value.address()));
			return false;
		}

		String mountPoint = output.get(0);
		try {
			// Check stat output is a path
			Paths.get(mountPoint);
		} catch (InvalidPathException | NullPointerException ex) {
			monitor.log(String.format("Invalid mount point %s for %s on server %s", mountPoint, backupRoot,
					server.value.address()));
			return false;
		}

		if (forbidenMountPoint.contains(mountPoint)) {
			monitor.log(String.format("Forbiden mount point %s for %s on server %s", mountPoint, backupRoot,
					server.value.address()));
			return false;
		}

		return true;
	}

	/**
	 * @param w
	 * @param dpCtx
	 * @param tag
	 * @param srv
	 * @return
	 * @throws ServerFault
	 */
	private PartGeneration backup(IBackupWorker w, IDPContext dpCtx, PartAllocation pa, ItemValue<Server> srv) {
		ToolBootstrap tool = new ToolBootstrap(dpCtx);
		w.prepareDataDirs(dpCtx, pa.next.tag, srv);
		Set<String> dirs = w.getDataDirs();
		IToolConfig config = tool.configure(srv, pa.next.tag, dirs);
		IToolSession session = tool.newSession(config);
		try {
			PartGeneration ret = session.backup(pa.previous, pa.next);
			ret.end = new Date();
			return ret;
		} finally {
			w.dataDirsSaved(dpCtx, pa.next.tag, srv);
		}
	}
}
