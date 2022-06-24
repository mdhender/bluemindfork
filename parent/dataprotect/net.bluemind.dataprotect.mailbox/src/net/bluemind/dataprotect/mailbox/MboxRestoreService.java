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

package net.bluemind.dataprotect.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.Sudo;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.task.service.TaskUtils.ExtendedTaskStatus;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.BackupPath;
import net.bluemind.dataprotect.service.DPContextFactory;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.IDPContext.ITool;
import net.bluemind.dataprotect.service.IDPContext.IToolConfig;
import net.bluemind.dataprotect.service.IDPContext.IToolSession;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.IMAPRuntimeException;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.helper.ArchiveHelper;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class MboxRestoreService {
	private static final Logger logger = LoggerFactory.getLogger(MboxRestoreService.class);

	public enum Mode {
		Replace, Subfolder
	}

	private static final Set<String> defaultUserFolders = new HashSet<>(DefaultFolder.USER_FOLDERS_NAME);

	public MboxRestoreService() {
		defaultUserFolders.add("INBOX");
	}

	public static final SecurityContext as(String uid, String domainContainerUid) throws ServerFault {
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), uid, Arrays.<String>asList(),
				Arrays.<String>asList(), Collections.emptyMap(), domainContainerUid, "en", "MboxRestoreService.as");
		String sessionId = userContext.getSessionId();
		CacheBackingStore<SecurityContext> cache = Sessions.get();
		cache.put(sessionId, userContext);
		return userContext;
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
			logger.info("serverUid is Null");
			monitor.end(false, "no_backup", "{ \"status\": \"nobackup\" }");
			return;
		}

		monitor.begin(1, "restore started.");
		BoxFsFolders boxFsFolders = BoxFsFolders.build(domain, mbox, dpg);

		SystemConf sysconf = LocalSysconfCache.get();
		if (ArchiveHelper.isSdsArchiveKind(sysconf)) {
			try {
				restoreSds(domain, dpg, mbox, boxFsFolders, mode, monitor);
			} catch (ServerFault sf) {
				monitor.end(false, "finished", "{ \"status\": \"Server Error:" + sf.getMessage() + "\" }");
				throw sf;
			}
		} else {
			restoreRsync(domain, dpg, mailPart, mbox, boxFsFolders, mode, monitor);
		}
	}

	private void restoreRsync(ItemValue<Domain> domain, DataProtectGeneration dpg, PartGeneration mailPart,
			ItemValue<Mailbox> mbox, BoxFsFolders boxFsFolders, Mode mode, IServerTaskMonitor monitor)
			throws IMAPException {
		String serverUid = mailPart.server;
		IDPContext dpCtx = DPContextFactory.newContext(monitor);
		ITool restTool = dpCtx.tool();

		IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer srvApi = sp.instance(IServer.class, InstallationId.getIdentifier());
		ItemValue<Server> source = srvApi.getComplete(serverUid);
		IToolConfig conf = restTool.configure(source, "mail/imap", new HashSet<>());
		IToolSession session = restTool.newSession(conf);
		INodeClient nc = NodeActivator.get(source.value.address());

		switch (mode) {
		case Replace:
			boxFsFolders.allFolders().forEach(f -> {
				NCUtils.exec(nc, String.format("rm -fr '%s'", f));
			});
			session.restore(mailPart.id, boxFsFolders.allFolders());

			IDirEntryMaintenance demApi = sp.instance(IDirEntryMaintenance.class, domain.uid, mbox.uid);
			TaskRef tr = demApi.repair(new HashSet<>(Arrays.asList("mailboxAcls", "mailboxDefaultFolders")));

			ExtendedTaskStatus extStatus = TaskUtils.wait(sp, tr);
			extStatus.logs.stream().forEach(monitor::log);
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
			logger.error("Unsupported restore mode: {}", mode);
			monitor.end(false, "finished", "{ \"status\": \"Unsupported restore mode\" }");
			return;
		}

		// ensure mbox files are owned by cyrus:mail
		boxFsFolders.allFolders().forEach(f -> {
			logger.debug(String.format("Ensure cyrus:mail ownership on '%s' and sub-files", f));
			NCUtils.exec(nc, String.format("chown -R cyrus:mail '%s'", f));
		});

		String recon = "/usr/sbin/reconstruct -r -f -R -G -I " + BoxFsFolders.namespace(mbox) + mbox.value.name + "@"
				+ domain.uid;
		logger.info("Reconstruct command: {}", recon);
		ExitList el = NCUtils.exec(nc, recon);
		for (String msg : el) {
			logger.info("RECONSTRUCT: {}", msg);
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

	/**
	 * Returns one user with write right on the mailshare
	 * 
	 * @param domainUid
	 * @param mbox
	 * @return
	 */
	private String getMailshareWriter(String domainUid, ItemValue<Mailbox> mbox) {
		IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IContainerManagement mailboxAclsService = sp.instance(IContainerManagement.class, "mailbox:acls-" + mbox.uid);
		List<String> writers = mailboxAclsService.getAccessControlList().stream()
				.filter(entity -> entity.verb.can(Verb.Write)).map(entry -> entry.subject).collect(Collectors.toList());
		if (writers.isEmpty()) {
			return null;
		}
		IDirectory dir = sp.instance(IDirectory.class, domainUid);
		List<ItemValue<DirEntry>> entries = dir.getMultiple(writers);
		Optional<ItemValue<DirEntry>> dirEntry = entries.stream().filter(de -> de.value.kind == Kind.USER).findFirst();
		String entryUid = null;
		if (!dirEntry.isPresent()) {
			IGroup groupService = sp.instance(IGroup.class, domainUid);
			List<ItemValue<DirEntry>> groups = entries.stream().filter(de -> de.value.kind == Kind.GROUP)
					.collect(Collectors.toList());
			for (int i = 0; i < groups.size(); i++) {
				ItemValue<DirEntry> g = groups.get(i);
				List<Member> members = groupService.getExpandedUserMembers(g.uid);
				if (!members.isEmpty()) {
					entryUid = members.get(0).uid;
					break;
				}
			}
		} else {
			entryUid = dirEntry.get().uid;
		}
		return entryUid;
	}

	private void restoreSds(ItemValue<Domain> domain, DataProtectGeneration dpg, ItemValue<Mailbox> mbox,
			BoxFsFolders boxFsFolders, Mode mode, IServerTaskMonitor monitor) {
		BmContext live;
		Set<String> defaultFolders;
		boolean isMailshare = mbox.value.type == Type.mailshare;
		String inboxName = isMailshare ? mbox.value.name : "INBOX";

		if (isMailshare) {
			defaultFolders = new HashSet<>(DefaultFolder.MAILSHARE_FOLDERS_NAME.stream()
					.map(f -> mbox.value.name + "/" + f).collect(Collectors.toList()));
			defaultFolders.add(mbox.value.name);
			// Find another used with write permissions
			String entryUid = getMailshareWriter(domain.uid, mbox);
			logger.info("Found entry with write permissions on {}: {}", mbox, entryUid);
			if (entryUid == null || entryUid.isEmpty()) {
				monitor.end(false, "finished",
						"{ \"status\": \"Unable to find a user with write permissions on mailshare " + mbox.value.name
								+ "\" }");
				throw new ServerFault("Unable to find a user with write permissions on mailshare " + mbox.value.name);
			}
			live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext()
					.su("sds-restore-" + UUID.randomUUID().toString(), entryUid, domain.uid);
		} else {
			defaultFolders = new HashSet<>(defaultUserFolders);
			live = ServerSideServiceProvider.getProvider(as(mbox.uid, domain.uid)).getContext();
		}

		IMailboxFolders foldersService = live.getServiceProvider().instance(IMailboxFoldersByContainer.class,
				IMailReplicaUids.subtreeUid(domain.uid, mbox));
		IServer srvApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		List<PartGeneration> parts = dpg.parts;
		PartGeneration corepart = parts.stream().filter(p -> "sds".equals(p.datatype)).findFirst().orElseThrow(() -> {
			logger.error("unable to find backup part 'sds'");
			return new ServerFault("Unable to find backup part 'sds'");
		});

		ItemValue<Server> source = srvApi.getComplete(mbox.value.dataLocation);
		ItemValue<Server> coreServer = Topology.get().core();
		Path jsonpath = Paths.get(BackupPath.get(coreServer, "bm/core"), String.valueOf(corepart.id),
				"var/backups/bluemind/sds");
		CyrusSdsBackupMailbox sdsBackupMailbox;
		RestoreSdsMailbox rsm;
		try {
			rsm = new RestoreSdsMailbox(jsonpath);
		} catch (IOException e) {
			logger.error("error while reading sds backup index: {}", e.getMessage(), e);
			monitor.end(false, "unable to restore mailbox " + mbox, e.getMessage());
			return;
		}
		try {
			sdsBackupMailbox = rsm.getMailbox(mbox.uid);
		} catch (IOException | ParseException e) {
			logger.error("error while reading json: {}", e.getMessage(), e);
			monitor.end(false, "unable to restore mailbox " + mbox, e.getMessage());
			return;
		}

		if (sdsBackupMailbox == null) {
			monitor.end(false, "unable to restore mailbox", null);
			return;
		}

		// clean / remove folders before injecting backup
		if (mode == Mode.Replace) {
			// We need to remove child firsts
			List<ItemValue<MailboxFolder>> liveFolders = foldersService.all(); //
			liveFolders.sort((a, b) -> Long.valueOf(a.value.fullName.chars().filter(ch -> ch == '/').count())
					.compareTo(b.value.fullName.chars().filter(ch -> ch == '/').count()));
			Collections.reverse(liveFolders);
			for (ItemValue<MailboxFolder> liveFolder : liveFolders) {
				if (!defaultFolders.contains(liveFolder.value.fullName)) {
					try {
						logger.info("Removing {}", liveFolder);
						foldersService.deleteById(liveFolder.internalId);
					} catch (ServerFault sf) {
						logger.error("Unable to remove folder {}: {}", liveFolder.value.fullName, sf.getMessage());
					}
				} else {
					try {
						if (isMailshare && !liveFolder.value.fullName.startsWith(mbox.value.name)) {
							// Just a "useless" safety, we don't want to clear the write user mailbox by
							// error.
							continue;
						}
						logger.info("Removing messages of {}", liveFolder);
						foldersService.removeMessages(liveFolder.internalId);
					} catch (ServerFault sf) {
						logger.error("Unable to remove messages from folder {}: {}", liveFolder.value.fullName,
								sf.getMessage());
					}
				}
			}
		}

		// Now, restore the folders using bluemind api
		// We went to sort "INBOX" first
		List<CyrusSdsBackupFolder> folders = sdsBackupMailbox.getFolders();

		// sort by hierarchy depth
		folders.sort((a, b) -> Long.valueOf(a.fullNameWithoutInbox(inboxName).chars().filter(ch -> ch == '/').count())
				.compareTo(b.fullNameWithoutInbox(inboxName).chars().filter(ch -> ch == '/').count()));

		for (CyrusSdsBackupFolder folder : folders) {
			MailboxFolder createdfolder = new MailboxFolder();
			// restoredFolderName is "restored-blibla" and folder is the "source folder"
			// (from the backup)
			if (mode == Mode.Subfolder) {
				createdfolder.name = folder.name().equals(inboxName) ? boxFsFolders.restoreFolderName : folder.name();
				String fullName = boxFsFolders.restoreFolderName + (!folder.fullNameWithoutInbox(inboxName).isEmpty()
						? ("/" + folder.fullNameWithoutInbox(inboxName))
						: "").replace("//", "/");
				if (isMailshare) {
					createdfolder.fullName = mbox.value.name + "/" + fullName;
				} else {
					createdfolder.fullName = fullName;
				}
			} else if (mode == Mode.Replace) {
				createdfolder.fullName = folder.fullName();
				createdfolder.name = folder.name();
			} else {
				logger.error("Unsupported restore mode: {}", mode);
				monitor.end(false, "finished", "{ \"status\": \"Unsupported restore mode\" }");
				return;
			}
			if (!defaultFolders.contains(createdfolder.fullName)) {
				logger.info("Create folder: {}", createdfolder);
				monitor.log("Create folder: {}", createdfolder);
				try {
					ItemIdentifier createAck = foldersService.createBasic(createdfolder);
					logger.debug("live folder {} createack: {}", createdfolder, createAck);
				} catch (ServerFault sf) {
					logger.error("unable to create folder '{}': {}", createdfolder, sf.getMessage());
					monitor.log("unable to create folder '{}': {}", createdfolder, sf.getMessage());
					continue;
				}
			} else {
				logger.debug("{} is a defaultFolder, so it will not be created", createdfolder);
			}

			if (mbox.value.type == Type.mailshare) {
				try (StoreClient sc = new StoreClient(source.value.address(), 1143, "admin0", Token.admin0())) {
					if (!sc.login()) {
						throw new ServerFault("Error logging in as admin0");
					}
					restoreSdsMessages(live, sc, folder, createdfolder.fullName + "@" + domain.uid, monitor);
				}
			} else {
				try (Sudo sudo = Sudo.forLogin(mbox.value.name, domain.uid)) {
					try (StoreClient sc = new StoreClient(source.value.address(), 1143,
							mbox.value.name + "@" + domain.uid, sudo.context.getSessionId())) {
						if (!sc.login()) {
							throw new ServerFault("error logging in to " + source.value.address() + ":1143 as "
									+ mbox.value.name + "@" + domain.uid);
						}
						restoreSdsMessages(live, sc, folder, createdfolder.fullName, monitor);
					}
				}
			}
		}
		monitor.end(true, "finished", "{ \"status\": \"not_implemented\" }");
		logger.info("ending task with mon {}", monitor);
	}

	private void restoreSdsMessages(BmContext context, StoreClient sc, CyrusSdsBackupFolder folder, String mboxFolder,
			IServerTaskMonitor monitor) {
		MessageBodyObjectStore sds = new MessageBodyObjectStore(context);

		FlagsList flags = new FlagsList();
		flags.add(Flag.BMARCHIVED);
		flags.add(Flag.SEEN);
		long allMessagesCount = folder.messageCount();
		monitor.log("Restoring {} messages of folder {}", allMessagesCount, folder.fullName());
		AtomicLong restorationCounter = new AtomicLong(0);
		List<List<CyrusSdsBackupMessage>> partitioned = Lists.partition(folder.messages(), 32);
		partitioned.stream().forEach(msgList -> {
			Path[] paths = sds
					.mopen(msgList.stream().map(msg -> msg.guid).collect(Collectors.toList()).toArray(new String[0]));
			int idx = 0;
			for (Path p : paths) {
				// msgList and paths must be in the same order, this is critical
				CyrusSdsBackupMessage msg = msgList.get(idx++);
				try (InputStream instream = Files.newInputStream(p)) {
					int added = sc.append(mboxFolder, instream, flags, msg.date);
					if (added <= 0) {
						logger.error("Unable to inject message {}", p);
					} else {
						long currentCount = restorationCounter.incrementAndGet();
						if (currentCount == allMessagesCount || (currentCount % 1000) == 0) {
							monitor.log("Restored {}/{} messages", currentCount, allMessagesCount);
						}
					}
				} catch (IOException e) {
					logger.error("unable to open downloaded message {}", p, e);
				} catch (IMAPRuntimeException e) {
					logger.error("imap append error: {}", e.getMessage());
				} finally {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
					}
				}
				monitor.log("Restored {}/{} messages", restorationCounter.get(), allMessagesCount);
			}
		});

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
		IToolConfig conf = restTool.configure(source, "mail/archive", new HashSet<>());
		IToolSession session = restTool.newSession(conf);

		Set<String> toRestore = new HashSet<>();
		toRestore.add("/var/spool/bm-hsm/snappy/user/" + d.uid + "/" + mbox.uid);

		session.restore(mailPart.get().id, toRestore);
	}

	private void restoreFsFolders(IToolSession session, String rootPath, Set<String> path, INodeClient nc,
			int mailPartId) {
		logger.info("restore fs folder {}", rootPath);
		NCUtils.exec(nc, String.format("mkdir -p '%s'", rootPath));
		NCUtils.exec(nc, String.format("chown cyrus:mail '%s'", rootPath));
		NCUtils.exec(nc, String.format("chmod 700 '%s'", rootPath));
		path.forEach(f -> session.restoreOneFolder(mailPartId, f, rootPath));
	}

}
