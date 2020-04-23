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

package net.bluemind.dataprotect.mailbox.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.DPContextFactory;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.IDPContext.ITool;
import net.bluemind.dataprotect.service.IDPContext.IToolConfig;
import net.bluemind.dataprotect.service.IDPContext.IToolSession;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.domain.api.Domain;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class MboxRestoreService {
	private static final Logger logger = LoggerFactory.getLogger(MboxRestoreService.class);

	public static enum Mode {
		Replace, Subfolder
	}

	/**
	 * @param dpg
	 * @param mbox
	 * @param mode
	 * @param monitor
	 * @throws ServerFault
	 * @throws IMAPException
	 */
	public void restore(DataProtectGeneration dpg, ItemValue<Mailbox> mbox, ItemValue<Domain> domain, Mode mode,
			IServerTaskMonitor monitor) throws ServerFault, IMAPException {
		if (dpg == null) {
			throw new NullPointerException("DataProtectGeneration can't be null");
		}
		monitor.begin(1, "restore started.");
		logger.info("RESTORE mode: {}, box: {}", mode, mbox.uid);
		Mailbox box = mbox.value;
		logger.info("BOX kind: " + box.type + ", name: " + box.name);

		IDPContext dpCtx = DPContextFactory.newContext(monitor);
		ITool restTool = dpCtx.tool();

		String serverUid = null;
		PartGeneration mailPart = null;
		for (PartGeneration pg : dpg.parts) {
			if ("mail/imap".equals(pg.tag) && mbox.value.dataLocation.equals(pg.server)) {
				serverUid = pg.server;
				mailPart = pg;
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
		IToolConfig conf = restTool.configure(source, "mail/imap", new HashSet<String>());
		IToolSession session = restTool.newSession(conf);

		BoxFsFolders boxFsFolders = BoxFsFolders.build(domain, mbox, dpg);

		INodeClient nc = NodeActivator.get(source.value.address());

		switch (mode) {
		case Replace:
			boxFsFolders.allFolders().forEach(f -> {
				NCUtils.exec(nc, String.format("rm -fr '%s'", f));
			});
			session.restore(mailPart.id, boxFsFolders.allFolders());

			IDirEntryMaintenance demApi = sp.instance(IDirEntryMaintenance.class, domain.uid, mbox.uid);
			TaskRef tr = demApi.repair(new HashSet<>(Arrays.asList("mailboxAcls", "mailboxDefaultFolders")));

			ITask taskApi = sp.instance(ITask.class, tr.id);
			TaskUtils.wait(sp, tr);
			taskApi.getCurrentLogs().stream().forEach(l -> monitor.log(l));
			break;
		case Subfolder:
			if (mbox.value.type == Type.mailshare) {
				try (StoreClient sc = new StoreClient(source.value.address(), 1143, "admin0", Token.admin0())) {
					sc.login(false);
					String partosh = CyrusPartition.forServerAndDomain(source.uid, domain.uid).name;
					sc.createMailbox(BoxFsFolders.fsLogin(mbox.value.name) + "/" + boxFsFolders.restoreFolderName + "@"
							+ domain.uid, partosh);
				}
			}

			int mailPartId = mailPart.id;

			restoreFsFolders(session, boxFsFolders.restoreDataRoot, boxFsFolders.dataPath, nc, mailPartId);
			restoreFsFolders(session, boxFsFolders.restoreMetaRoot, boxFsFolders.metaPath, nc, mailPartId);
			restoreFsFolders(session, boxFsFolders.restoreArchiveRoot, boxFsFolders.archivePath, nc, mailPartId);

			break;
		default:
			logger.error("Unsupported restore mode: " + mode);
			monitor.end(false, "finished", "{ \"status\": \"Unsupported restore mode\" }");
			return;
		}

		// ensure mbox files are owned by cyrus:mail
		boxFsFolders.allFolders().forEach(f -> {
			logger.debug(String.format("Ensure cyrus:mail ownership on '%s' and sub-files", f));
			NCUtils.exec(nc, String.format("chown -R cyrus:mail '%s'", f));
		});

		String recon = "/usr/sbin/reconstruct -r -f -R -G -I " + BoxFsFolders.namespace(mbox) + box.name + "@"
				+ domain.uid;
		logger.info("Reconstruct command: " + recon);
		ExitList el = NCUtils.exec(nc, recon);
		for (String e : el) {
			logger.info("RECONSTRUCT: " + e);
		}

		logger.info("[{}] Restore hsm for {}", mbox, dpg);
		restoreHsm(dpg, restTool, domain, mbox);

		IDirEntryMaintenance repairSupport = sp.instance(IDirEntryMaintenance.class, domain.uid, mbox.uid);
		Set<String> ops = repairSupport.getAvailableOperations().stream().map(mo -> mo.identifier)
				.collect(Collectors.toSet());
		TaskRef repairTask = repairSupport.repair(ops);
		TaskUtils.wait(sp, repairTask);

		monitor.end(true, "finished", "{ \"status\": \"not_implemented\" }");
		logger.info("ending task with mon {}", monitor);

	}

	private void restoreHsm(DataProtectGeneration dpg, ITool restTool, ItemValue<Domain> d, ItemValue<Mailbox> mbox) {
		IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer srvApi = sp.instance(IServer.class, InstallationId.getIdentifier());
		List<Assignment> assignments = srvApi.getAssignments(d.uid);

		Optional<Assignment> ass = assignments.stream().filter(a -> a.tag.equals("mail/archive")).findFirst();
		if (!ass.isPresent()) {
			logger.info("No mail/archive for domain {}", d.uid);
			return;
		}

		Optional<PartGeneration> mailPart = dpg.parts.stream()
				.filter(pg -> pg.tag.equals("mail/archive") && pg.server.equals(ass.get().serverUid)).findFirst();

		if (!mailPart.isPresent()) {
			logger.info("No PartGeneration for domain {}, tag mail/archive", d.uid);
			return;
		}

		ItemValue<Server> source = srvApi.getComplete(mailPart.get().server);
		IToolConfig conf = restTool.configure(source, "mail/archive", new HashSet<String>());
		IToolSession session = restTool.newSession(conf);

		Set<String> toRestore = new HashSet<String>();
		toRestore.add("/var/spool/bm-hsm/snappy/user/" + d.uid + "/" + mbox.uid);

		session.restore(mailPart.get().id, toRestore);
	}

	private void restoreFsFolders(IToolSession session, String rootPath, Set<String> path, INodeClient nc,
			int mailPartId) {
		logger.info("restore fs folder {}", rootPath);
		NCUtils.exec(nc, String.format("mkdir -p '%s'", rootPath));
		NCUtils.exec(nc, String.format("chown cyrus:mail '%s'", rootPath));
		NCUtils.exec(nc, String.format("chmod 700 '%s'", rootPath));
		path.forEach(f -> {
			session.restoreOneFolder(mailPartId, f, rootPath);
		});
	}

}
