/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.dataprotect.service.internal;

import java.io.ByteArrayInputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class ServersToBackup {
	private static final Logger logger = LoggerFactory.getLogger(ServersToBackup.class);

	private static final String backupRoot = "/var/backups/bluemind";
	private static final String backupTemp = backupRoot + "/temp";
	private static final String backupWork = backupRoot + "/work";

	private final BmContext ctx;
	public final List<ItemValue<Server>> servers;
	private final List<String> skipTags;
	private final Set<ItemValue<Server>> invalidServers = new HashSet<>();

	public static ServersToBackup build(BmContext ctx) {
		return new ServersToBackup(ctx,
				ctx.provider().instance(IServer.class, InstallationId.getIdentifier()).allComplete(),
				loadSkipTags(ctx));
	}

	private static List<String> loadSkipTags(BmContext ctx) {
		List<String> skipTags = new ArrayList<>(ctx.provider().instance(ISystemConfiguration.class).getValues()
				.stringList(SysConfKeys.dpBackupSkipTags.name()));
		skipTags.add(TagDescriptor.mail_smtp_edge.getTag());
		skipTags.add(TagDescriptor.bm_nginx_edge.getTag());

		for (String tag : skipTags) {
			logger.debug("Skipping backup of tag {}", tag);
		}

		return skipTags;
	}

	public ServersToBackup(BmContext ctx, List<ItemValue<Server>> servers, List<String> skipTags) {
		this.ctx = ctx;
		this.servers = filteroutSkippedTags(servers);
		this.skipTags = skipTags;
	}

	private List<ItemValue<Server>> filteroutSkippedTags(List<ItemValue<Server>> servers) {
		IServer serverApi = ctx.provider().instance(IServer.class, InstallationId.getIdentifier());
		return servers.stream().filter(ivs -> !(serverApi.getServerAssignments(ivs.uid).stream()
				.allMatch(assignment -> skipTags.contains(assignment.tag)))).toList();
	}

	public Set<String> getServerBackupTags(ItemValue<Server> server) {
		return server.value.tags.stream().filter(tag -> !skipTags.contains(tag)).collect(Collectors.toSet());
	}

	public Collection<ItemValue<Server>> checkIntegrity() {
		checkIntegrity(Optional.empty());
		return Collections.unmodifiableSet(invalidServers);
	}

	public boolean checkIntegrity(Optional<IServerTaskMonitor> monitor) {
		invalidServers.clear();
		monitor.ifPresent(m -> m.begin(servers.size(), String.format("Checking %s on each hosts", backupRoot)));

		String fn = backupTemp + "/check-" + UUID.randomUUID().toString() + ".torm";
		ItemValue<Server> last = null;

		IServer serverApi = ctx.provider().instance(IServer.class, InstallationId.getIdentifier());
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
					nc.mkdirs(backupTemp);
					nc.mkdirs(backupWork);
					nc.writeFile(fn, new ByteArrayInputStream("YOU CAN SAFELY REMOVE THIS FILE".getBytes()));
				}

				last = server;

				if (!System.getProperty("node.local.ipaddr", "nope").equals(server.value.ip)) {
					if (!allowedMountPoint(monitor, server, nc) || !sharedDataStore(monitor, server, nc, fn)) {
						invalidServers.add(server);
						validBackupStore = false;
					}
				} else {
					// TEST MODE ONLY
					Process p = Runtime.getRuntime()
							.exec("sudo chown -R " + System.getProperty("user.name") + " /var/backups/bluemind");
					p.waitFor(10, TimeUnit.SECONDS);

				}

				monitor.ifPresent(
						m -> m.progress(1, String.format("%s checked on %s", backupRoot, server.value.address())));
			} catch (Exception e) {
				logger.error(e.getMessage());
				monitor.ifPresent(m -> m.end(false,
						String.format("Unable to check %s on %s", backupRoot, server.value.address()), "KO"));
				return false;
			}
		}

		if (last != null) {
			serverApi.submitAndWait(last.uid, "rm -f " + fn);
		}

		if (!validBackupStore) {
			monitor.ifPresent(
					m -> m.end(false, String.format("Invalid state for %s on at least one server", backupRoot), "KO"));
		} else {
			monitor.ifPresent(m -> m.end(true, String.format("%s is ok on all servers", backupRoot), "OK"));
		}

		return validBackupStore;
	}

	/**
	 * Check if backup is not stored on a forbidden mount point
	 * 
	 * @param monitor
	 * @param server
	 * @param nc
	 * @return
	 */
	private boolean allowedMountPoint(Optional<IServerTaskMonitor> monitor, ItemValue<Server> server, INodeClient nc) {
		String statCmd = "/usr/bin/stat --format '%m'";
		String backupMountPoint = backupRoot + "/";
		List<String> forbidenMountPoint = Arrays.asList("/", "/var", "/var/");

		ExitList output = NCUtils.exec(nc, statCmd + " " + backupMountPoint);
		if (output.size() != 1) {
			monitor.ifPresent(
					m -> m.log(String.format("Invalid stat command output on server %s", server.value.address())));
			return false;
		}

		String mountPoint = output.get(0);
		try {
			// Check stat output is a path
			Paths.get(mountPoint);
		} catch (InvalidPathException | NullPointerException ex) {
			monitor.ifPresent(m -> m.log(String.format("Invalid mount point %s for %s on server %s", mountPoint,
					backupRoot, server.value.address())));
			return false;
		}

		if (forbidenMountPoint.contains(mountPoint)) {
			monitor.ifPresent(m -> m.log(String.format("Forbiden mount point %s for %s on server %s", mountPoint,
					backupRoot, server.value.address())));
			return false;
		}

		return true;
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
	private boolean sharedDataStore(Optional<IServerTaskMonitor> monitor, ItemValue<Server> server, INodeClient nc,
			String fileName) {
		boolean found = nc.listFiles(fileName).size() == 1;
		if (!found) {
			monitor.ifPresent(m -> m.log(String.format("%s is not shared on %s", backupRoot, server.value.address())));
			return false;
		}

		return true;
	}
}
