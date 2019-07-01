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
package net.bluemind.filehosting.filesystem.service.internal.dataprotect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.service.DPContextFactory;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.IDPContext.ITool;
import net.bluemind.dataprotect.service.IDPContext.IToolConfig;
import net.bluemind.dataprotect.service.IDPContext.IToolSession;
import net.bluemind.dataprotect.service.IRestoreActionProvider;
import net.bluemind.filehosting.filesystem.service.internal.FileSystemFileHostingService;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class FileHostingRestoreProvider implements IRestoreActionProvider {

	private static final Logger logger = LoggerFactory.getLogger(FileHostingRestoreProvider.class);

	@Override
	public TaskRef run(final RestoreOperation op, final DataProtectGeneration backup, final Restorable item)
			throws ServerFault {

		ITasksManager tsk = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITasksManager.class);
		IServerTask toRun = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				if (backup == null) {
					throw new NullPointerException("DataProtectGeneration can't be null");
				}
				String log = "Filehosting restore for domain " + item.domainUid + " started.";
				logger.info(log);
				monitor.begin(1, log);

				IDPContext dpCtx = DPContextFactory.newContext(monitor);
				ITool restTool = dpCtx.tool();

				String serverUid = null;
				PartGeneration part = null;
				for (PartGeneration pg : backup.parts) {
					if ("bm/core".equals(pg.tag)) {
						serverUid = pg.server;
						part = pg;
						break;
					}
				}
				if (serverUid == null) {
					monitor.end(false, "no_backup", "{ \"status\": \"nobackup\" }");
					return;
				}
				IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
				IServer srvApi = sp.instance(IServer.class, InstallationId.getIdentifier());
				ItemValue<Server> source = srvApi.getComplete(serverUid);
				IToolConfig conf = restTool.configure(source, "bm/core", new HashSet<String>());
				IToolSession session = restTool.newSession(conf);
				String target = session.tmpDirectory();
				Set<String> folders = new HashSet<>();
				folders.add(FileSystemFileHostingService.DEFAULT_STORE_PATH);
				session.restore(part.id, folders, target);

				// move to the final location
				INodeClient nc = NodeActivator.get(source.value.address());
				Set<String> expanded = new HashSet<>();
				try {
					collectFiles(nc, expanded,
							target + FileSystemFileHostingService.DEFAULT_STORE_PATH + "/" + item.domainUid);
				} catch (Exception e) {
					logger.error(
							"On " + target + FileSystemFileHostingService.DEFAULT_STORE_PATH + ": " + e.getMessage(),
							e);
				}

				for (String filepath : expanded) {
					logger.info("Restoring path {}", expanded);
					int lastSlash = filepath.lastIndexOf('/');
					String parent = filepath.substring(0, lastSlash);
					parent = parent.substring(target.length());
					NCUtils.exec(nc, "mkdir -p " + parent);
					String theMove = "mv " + filepath + " " + parent;
					logger.warn("***** " + theMove);
					NCUtils.exec(nc, theMove);
				}
				monitor.end(false, "finished", "{ \"status\": \"not_implemented\" }");
			}

		};
		return tsk.run(toRun);
	}

	public void collectFiles(INodeClient nc, Set<String> expanded, String path) throws ServerFault {
		List<FileDescription> fd = nc.listFiles(path);
		for (FileDescription f : fd) {
			String absolute = path + "/" + f.getName();
			if (!f.isDirectory()) {
				expanded.add(absolute);
			} else {
				collectFiles(nc, expanded, absolute);
			}
			logger.info("Expanded to {}", absolute);
		}
	}

	@Override
	public List<RestoreOperation> operations() {
		RestoreOperation restore = new RestoreOperation();
		restore.identifier = "restore.filehosting";
		restore.translations = ImmutableMap.of("en", "Restore Filehosting data", "fr",
				"Restaurer pièces jointes détacheés");
		restore.kind = RestorableKind.DOMAIN;
		restore.requiredTag = "filehosting/data";
		return Arrays.asList(new RestoreOperation[] { restore });
	}

}
