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
package net.bluemind.backend.cyrus.internal;

import java.io.ByteArrayInputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import net.bluemind.backend.cyrus.CyrusAclService;
import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.MigrationPhase;
import net.bluemind.backend.cyrus.Sudo;
import net.bluemind.backend.cyrus.internal.files.Annotations;
import net.bluemind.backend.cyrus.internal.files.Cyrus;
import net.bluemind.backend.cyrus.internal.files.CyrusHsm;
import net.bluemind.backend.cyrus.internal.files.CyrusProxyPassword;
import net.bluemind.backend.cyrus.internal.files.CyrusReplication;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.imap.Acl;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.AnnotationList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.NameSpaceInfo;
import net.bluemind.imap.QuotaInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.mailbox.service.common.DefaultFolder.Status;
import net.bluemind.mailbox.service.hook.IMailboxEventConsumer;
import net.bluemind.mailbox.service.internal.DbAclToCyrusAcl;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class CyrusMailboxesStorage implements IMailboxesStorage {

	private Logger logger = LoggerFactory.getLogger(CyrusMailboxesStorage.class);
	private final static List<IMailboxEventConsumer> consumers = getHooks();

	private static List<IMailboxEventConsumer> getHooks() {
		RunnableExtensionLoader<IMailboxEventConsumer> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensions("net.bluemind.mailbox.storage", "hook", "hook", "impl");
	}

	public CyrusMailboxesStorage() {
	}

	@Override
	public void create(BmContext context, String domainUid, ItemValue<Mailbox> mbox) throws ServerFault {

		logger.info("mailbox created, going to configure it in cyrus");

		if (mbox.value.routing == Mailbox.Routing.external) {
			logger.info("mailbox is routing == external. No cyrus mbox needed for {}", mbox.uid);
			return;
		}

		if (mbox.value.dataLocation == null) {
			logger.warn("no datalocation for {}", mbox.uid);
			return;
		}

		ItemValue<Server> srvItem = getServer(context, mbox.value.dataLocation);
		CyrusService cyrus = new CyrusService(srvItem.value.address());
		String boxName = boxname(mbox.value, domainUid);

		switch (mbox.value.type) {
		case user: {

			cyrus.createBox(boxName, domainUid);
			boxCreated(context, domainUid, mbox);

			Map<String, Acl> acl = new HashMap<>();
			acl.put("admin0", Acl.ALL);
			acl.put(mbox.uid + "@" + domainUid, Acl.ALL);
			CyrusAclService.sync(srvItem.value.address()).setAcl(boxName, acl);

			List<String> createdFolders = MailboxOps.createUserFolders(domainUid, srvItem.value, mbox.value.name,
					DefaultFolder.USER_FOLDERS);
			foldersCreated(context, domainUid, mbox, createdFolders);
			// MailboxOps.annotate(srv, boxContainer);
		}
			break;
		case group:
		case resource:
		case mailshare: {
			cyrus.createBox(boxName, domainUid);
			boxCreated(context, domainUid, mbox);

			Map<String, Acl> acl = new HashMap<>();
			acl.put("admin0", Acl.ALL);
			acl.put("anyone", Acl.POST);
			CyrusAclService.sync(srvItem.value.address()).setAcl(boxName, acl);

			List<String> createdFolders = MailboxOps.createMailshareFolders(domainUid, srvItem.value, mbox.value.name,
					DefaultFolder.MAILSHARE_FOLDERS);
			foldersCreated(context, domainUid, mbox, createdFolders);
		}
			break;
		default:
			throw new ServerFault("not handled mailbox type: " + mbox.value.type);
		}

		if (mbox.value.routing == Mailbox.Routing.none) {
			logger.info("mailbox is routing == none. set quota to 1 for {}", mbox.uid);
			cyrus.setQuota(boxName, 1);
		}

		if (mbox.value.quota != null) {
			cyrus.setQuota(boxName, mbox.value.quota);
		}
	}

	public void update(BmContext context, String domainUid, ItemValue<Mailbox> previousValue, ItemValue<Mailbox> value)
			throws ServerFault {

		logger.info("mailbox update {}", previousValue.uid);

		Mailbox prev = previousValue.value;
		Mailbox cur = value.value;

		if (previousValue.value.equals(value.value)) {
			logger.debug("no changes for mailbox {} ", value.uid);
			return;
		}

		// external routing, do nothing
		if (!prev.routing.managed() && !cur.routing.managed()) {
			return;
		}

		// mail is no more managed by Blue Mind
		if (prev.routing.managed() && !cur.routing.managed()) {
			// FIXME delete cyrus acl
			return;
		}

		// switch from not managed by Blue Mind to something managed: it is a
		// create from cyrus pov
		if (!prev.routing.managed() && cur.routing.managed()) {
			create(context, domainUid, value);
			return;
		}

		final String mailboxDatalocation = previousValue.value.dataLocation != null ? previousValue.value.dataLocation
				: value.value.dataLocation;
		final CyrusService cyrus = new CyrusService(getServer(context, mailboxDatalocation).value.address());

		// cur.routing.managed == true
		// mailbox was renamed
		if (!prev.name.equals(cur.name)) {
			logger.info("mailbox {} was renamed {} => {}", value.uid, prev.name, cur.name);

			switch (value.value.type) {
			case group:
			case user:
			case resource:
			case mailshare: {

				logger.debug("{} mbox [{}] rename", value.value.type, value.value.name);
				break;
			}
			default:
				throw new ServerFault("not handled mailbox type: " + value.value.type);
			}

			String pboxName = boxname(prev, domainUid);

			String boxName = boxname(cur, domainUid);

			cyrus.renameBox(pboxName, boxName);

			Map<String, Acl> acl = new HashMap<>();
			acl.put("admin0", Acl.ALL);
			acl.put(cur.name + "@" + domainUid, Acl.ALL);
			cyrus.setAcl(boxName, acl);

			if (cur.routing == Mailbox.Routing.none) {
				cyrus.setQuota(boxName, 1);
			}

			if (prev.routing == Mailbox.Routing.none && cur.routing == Mailbox.Routing.internal) {
				cyrus.setQuota(boxName, 0);
			}

			return;
		}

		// Ensure managed mailbox exist in cyrus
		if (cur.routing.managed() && !mailboxExist(context, domainUid, cur)) {
			create(context, domainUid, value);
			return;
		}

		// mailbox was moved
		if (prev.dataLocation != null && !cur.dataLocation.equals(prev.dataLocation)) {
			logger.info("Move {} MAILBOX from {} to {}", cur.name, prev.dataLocation, cur.dataLocation);

			switch (value.value.type) {
			case group:
			case user:
			case resource:
			case mailshare: {

				logger.debug("{} mbox [{}] move", value.value.type, value.value.name);
				break;
			}
			default:
				throw new ServerFault("not handled mailbox type: " + value.value.type);
			}
			String boxName = boxname(cur, domainUid);

			ItemValue<Server> dest = getServer(context, cur.dataLocation);

			cyrus.xfer(boxName, domainUid, dest);

			if (cur.routing == Mailbox.Routing.none) {
				cyrus.setQuota(boxName, 1);
			}

			if (prev.routing == Mailbox.Routing.none && cur.routing == Mailbox.Routing.internal) {
				cyrus.setQuota(boxName, 0);
			}

			return;
		}

		// mailbox was updated from none to internal
		if (prev.routing == Mailbox.Routing.none && cur.routing == Mailbox.Routing.internal) {
			String boxName = boxname(cur, domainUid);
			cyrus.setQuota(boxName, 0);
			return;
		}

		// update quota
		String boxName = boxname(cur, domainUid);
		if (value.value.quota != null) {
			cyrus.setQuota(boxName, value.value.quota);
		} else {
			cyrus.setQuota(boxName, 0);
		}
	}

	@Override
	public boolean mailboxExist(BmContext context, String domainUid, Mailbox cur) throws ServerFault {
		ItemValue<Server> srvItem = getServer(context, cur.dataLocation);
		CyrusService cyrus = new CyrusService(srvItem.value.address());

		return cyrus.boxExist(boxname(cur, domainUid));
	}

	@Override
	public void delete(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {

		switch (value.value.type) {
		case group:
		case user:
		case resource:
		case mailshare: {

			logger.debug("{} mbox [{}] move", value.value.type, value.value.name);
			break;
		}
		default:
			throw new ServerFault("not handled mailbox type: " + value.value.type);
		}

		String boxName = boxname(value.value, domainUid);

		if (value.value.dataLocation == null) {
			logger.warn("group without physical mailbox");
			return;
		}
		new CyrusService(getServer(context, value.value.dataLocation).value.address()).deleteBox(boxName, domainUid);

	}

	private ItemValue<Server> getServer(BmContext context, String serverUid) throws ServerFault {
		ItemValue<Server> srvItem = context.su().provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(serverUid);

		if (srvItem == null) {
			throw new ServerFault("imap data location server: " + serverUid + " not found");
		}

		return srvItem;
	}

	@Override
	public void changeFilter(BmContext context, ItemValue<Domain> domain, ItemValue<Mailbox> mailbox, MailFilter filter)
			throws ServerFault {

		if (mailbox.value.routing == Mailbox.Routing.external) {
			logger.info("mailbox is routing == external. No cyrus mbox necessary for {}", mailbox.uid);
			return;
		}

		if (mailbox.value.routing == Mailbox.Routing.none && mailbox.value.dataLocation == null) {
			logger.error("mailbox is routing == none and doesnt have server allocated for {}", mailbox.uid);
			return;
		}
		SieveWriter sw = new SieveWriter();
		sw.write(mailbox, domain, filter);

	}

	private String boxname(Mailbox value, String domainUid) {
		return value.type.cyrAdmPrefix + value.name + "@" + domainUid;
	}

	@Override
	public void changeDomainFilter(BmContext context, String domainUid, MailFilter filter) throws ServerFault {

		SieveWriter sw = new SieveWriter();
		sw.write(domainUid, filter);

	}

	@Override
	public void createDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {

		// create partition

		CyrusService cyrus = new CyrusService(server.value.address());
		cyrus.createPartition(value.uid);
		cyrus.refreshPartitions(getMailImapDomains(context, server.uid));

		// update cyrus admins
		new CyrusAdmins(context.su().provider().instance(IServer.class, "default"), server.uid).write();

		// IServer service = context.provider().instance(IServer.class,
		// "default");
		// update imapd conf to be sure (not necessary)
		// Imapd imapdConf = new Imapd(service, server.uid);
		// imapdConf.setPermanentToken(Token.admin0());
		// imapdConf.write();
		cyrus.reload();
		logger.info("**** Partition created for {}", value.uid);

	}

	@Override
	public void deleteDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {

		CyrusService cyrus = new CyrusService(server.value.address());

		// update cyrus admins
		new CyrusAdmins(context.su().provider().instance(IServer.class, "default"), server.uid).write();

		cyrus.reload();

		logger.info("Un-assign " + server.uid + " as a mail/imap backend");
	}

	@Override
	public void initialize(BmContext context, ItemValue<Server> server) throws ServerFault {

		SystemConf sysConf = context.su().provider().instance(ISystemConfiguration.class).getValues();
		int maxChild = getMaxChild(sysConf);
		int retention = getRetention(sysConf);
		// append our configuration
		IServer service = context.su().provider().instance(IServer.class, "default");

		Annotations annotationsConf = new Annotations(service, server.uid);
		annotationsConf.write();

		CyrusReplication replication = new CyrusReplication(service, server.uid);
		replication.write();

		rewriteCyrusConfiguration(server.uid);

		new CyrusProxyPassword(service, server.uid).write();
		new CyrusAdmins(service, server.uid).write();

		Cyrus cyrusConf = new Cyrus(service, server.uid, maxChild, retention);
		cyrusConf.write();

		CyrusService cyrus = new CyrusService(server.value.address());
		cyrus.refreshPartitions(getMailImapDomains(context, server.uid));
		cyrus.reloadSds();
		cyrus.reload();
	}

	private List<String> getMailImapDomains(BmContext context, String serverUid) {
		List<String> domains = new LinkedList<>();
		List<Assignment> asses = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getServerAssignments(serverUid);
		for (Assignment a : asses) {
			if ("mail/imap".equals(a.tag)) {
				domains.add(a.domainUid);
			}
		}
		return domains;
	}

	@Override
	public Integer getUnreadMessagesCount(String domainUid, ItemValue<User> user) throws ServerFault {
		// FIXME Wooot !
		ItemValue<Server> server = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(user.value.dataLocation);
		return new CyrusService(server.value.address()).getUnSeenMessages(domainUid, user);
	}

	/**
	 * @param context
	 * @param boxName
	 * @param createdFolders
	 * @throws ServerFault
	 */
	public void foldersCreated(BmContext context, String domainUid, ItemValue<Mailbox> mbox,
			List<String> createdFolders) throws ServerFault {
		for (String folder : createdFolders) {
			for (IMailboxEventConsumer consumer : consumers) {
				try {
					consumer.onMailFolderCreated(context, domainUid, mbox, folder);
				} catch (ServerFault e) {
					if (logger.isDebugEnabled()) {
						logger.error("error during dispatch foldersCreated ", e);
					} else {
						logger.error("error during dispatch foldersCreated : {}", e.getMessage());
					}
				}
			}
		}
	}

	private void boxCreated(BmContext context, String domainUid, ItemValue<Mailbox> mbox) {
		for (IMailboxEventConsumer consumer : consumers) {
			try {
				consumer.onTopLevelFolderCreated(context, domainUid, mbox);
			} catch (ServerFault e) {
				if (logger.isDebugEnabled()) {
					logger.error("error during dispatch boxCreated ", e);
				} else {
					logger.error("error during dispatch boxCreated : {}", e.getMessage());
				}
			}
		}
	}

	@Override
	public List<MailFolder> listFolders(BmContext context, String domainUid, ItemValue<Mailbox> mailbox)
			throws ServerFault {
		ItemValue<Server> server = getServer(context, mailbox.value.dataLocation);

		if (mailbox.value.type == Type.user) {
			return listUserFolders(context, domainUid, mailbox);
		} else {
			return listSimpleFolders(domainUid, mailbox, server);
		}
	}

	@Override
	public List<MailFolder> checkAndRepairHierarchy(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) throws ServerFault {
		ItemValue<Server> server = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(mailbox.value.dataLocation);

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault(String.format("Fail to login: admin0", domainUid));
			}
			List<MailFolder> ret = new ArrayList<MailFolder>();

			Set<String> done = new HashSet<String>();
			ListResult lr = sc.listSubFoldersMailbox(boxname(mailbox.value, domainUid));
			// list returns /a, /a/b, /a/b/c
			// reverse the list to /a/b/c, /a/b, /a so we only check /a/b/c
			// and skip /a/b and/a
			Collections.reverse(lr);
			long start = System.currentTimeMillis();
			for (ListInfo li : lr) {

				if (done.contains(li.getName())) {
					continue;
				}

				// extract folder path from mailboxname
				// mailshare mailbox name: MAILBOX_NAME/FOLDER_PATH@DOMAIN
				// user mailbox name: user/MAILBOX_NAME/FOLDER_PATH@DOMAIN
				String folderPath = li.getName();
				final StringBuilder mailboxBuilder = new StringBuilder();
				if (mailbox.value.type == Type.user) {
					mailboxBuilder.append("user/" + mailbox.value.name + "/");
					folderPath = folderPath.substring(("user/" + mailbox.value.name).length());
				} else {
					mailboxBuilder.append(mailbox.value.name + "/");
					folderPath = folderPath.substring(mailbox.value.name.length());
				}
				folderPath = folderPath.substring(1, folderPath.length() - ("@" + domainUid).length());

				// reconstruct each folder
				Splitter.on("/").split(folderPath).forEach(path -> {
					mailboxBuilder.append(path);
					String mailboxName = mailboxBuilder.toString() + "@" + domainUid;

					done.add(mailboxName);
					try {
						if (!sc.select(mailboxName)) {
							logger.error("{} does not exist. create it", mailboxName);
							MailFolder f = new MailFolder();
							f.name = mailboxBuilder.toString();
							f.type = MailFolder.Type.normal;
							f.rootUri = "imap://" + mailbox.uid;
							ret.add(f);
							if (repair) {
								if (!sc.create(mailboxName)) {
									logger.error("Fail to create {}", mailboxName);
								}
							} else {
								logger.info("need to repair {}", mailboxName);
							}
						}
					} catch (IMAPException e) {
						throw new ServerFault(e);
					}
					mailboxBuilder.append("/");
				});
			}

			logger.info("checkAndRepairHierarchy mbox {}@{}, {} folders, tooks {}ms", mailbox.uid, domainUid, lr.size(),
					System.currentTimeMillis() - start);

			return ret;
		}
	}

	@Override
	public void checkAndRepairQuota(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		long start = System.currentTimeMillis();
		ItemValue<Server> server = getServer(context, mailbox.value.dataLocation);

		String mailboxQuotaPath = "/var/lib/cyrus/domain" + "/" + getLetterPath(domainUid.charAt(0)) + "/" + domainUid
				+ "/quota/" + getLetterPath(mailbox.value.name.charAt(0)) + "/user."
				+ mailbox.value.name.replace(".", "^");

		INodeClient nodeClient = NodeActivator.get(server.value.address());
		NCUtils.execNoOut(nodeClient, "rm -f " + mailboxQuotaPath);

		String mailboxName = boxname(mailbox.value, domainUid);
		try {
			new CyrusService(server.value.address()).setQuota(mailboxName,
					mailbox.value.quota == null ? 0 : mailbox.value.quota);
		} catch (ServerFault sf) {
			logger.error("Error during fixing quota of {}", mailboxName);
			throw sf;
		}

		NCUtils.execNoOut(nodeClient, "/usr/sbin/quota -f -d " + domainUid + " "
				+ (mailbox.value.type == Type.user ? "user." : "") + mailbox.value.name.replace(".", "^"));
		logger.info("Fixing quota of {} success", mailboxName);

		logger.info("checkAndRepairQuota mbox {}@{}, tooks {}ms", mailbox.uid, domainUid,
				System.currentTimeMillis() - start);
	}

	@Override
	public void checkAndRepairFilesystem(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		long start = System.currentTimeMillis();
		ItemValue<Server> server = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(mailbox.value.dataLocation);

		String mailboxName = MailboxOps.getMailboxPrefix(mailbox) + "@" + domainUid;

		fixMailboxPerms(server, domainUid, mailbox, mailboxName);
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		ExitList result = NCUtils.exec(nodeClient, "/usr/sbin/reconstruct -r -f -R " + mailboxName);
		if (result.getExitCode() != 0) {
			logger.error("Error {} during cyrus reconstruct of {}", result.getExitCode(), mailboxName);
			result.forEach(r -> logger.error(r));
		} else {
			logger.info("Cyrus reconstruct success");
		}

		logger.info("checkAndRepairFilesystem mbox {}@{}, tooks {}ms", mailbox.uid, domainUid,
				System.currentTimeMillis() - start);
	}

	private void fixMailboxPerms(ItemValue<Server> server, String domainUid, ItemValue<Mailbox> mailbox,
			String mailboxName) {
		String shellName = "/tmp/cyrus-repair-" + System.currentTimeMillis() + ".sh";
		StringBuilder shell = new StringBuilder();
		shell.append("#!/bin/bash\n");
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		char domainLetterPath = getLetterPath(domainUid.charAt(0));
		char mailboxLetterPath = getLetterPath(mailbox.value.name.charAt(0));

		String mailboxSpoolPath = CyrusPartition.forServerAndDomain(server, domainUid).name + "/domain/"
				+ domainLetterPath + "/" + domainUid + "/" + (mailbox.value.type == Type.user ? mailboxLetterPath : "*")
				+ "/" + (mailbox.value.type == Type.user ? "user/" : "") + mailbox.value.name.replace(".", "^");
		shell.append("chown -R cyrus:mail /var/spool/cyrus/data/" + mailboxSpoolPath + " /var/spool/cyrus/meta/"
				+ mailboxSpoolPath + "\n");

		String cyrusDomainRoot = "/var/lib/cyrus/domain";
		List<String> mailboxLibPath = Arrays.asList(cyrusDomainRoot + "/" + domainLetterPath, //
				cyrusDomainRoot + "/" + domainLetterPath + "/" + domainUid, //
				cyrusDomainRoot + "/" + domainLetterPath + "/" + domainUid + "/quota", //
				cyrusDomainRoot + "/" + domainLetterPath + "/" + domainUid + "/quota/" + mailboxLetterPath, //
				cyrusDomainRoot + "/" + domainLetterPath + "/" + domainUid + "/user", //
				cyrusDomainRoot + "/" + domainLetterPath + "/" + domainUid + "/user/" + mailboxLetterPath);

		mailboxLibPath.forEach(path -> {
			shell.append("test -e " + path + " && echo \"Fixing " + path + " directory\" && chown cyrus:mail " + path
					+ " && chmod 700 " + path + "\n");
		});

		String mailboxSubFile = cyrusDomainRoot + "/" + domainLetterPath + "/" + domainUid + "/user/"
				+ mailboxLetterPath + "/" + mailbox.value.name + ".sub";
		shell.append("test -e " + mailboxSubFile + " && echo \"Fixing " + mailboxSubFile
				+ " directory\" && chown cyrus:mail " + mailboxSubFile + " && chmod 700 " + mailboxSubFile + "\n");

		String mailboxQuotaFile = cyrusDomainRoot + "/" + domainLetterPath + "/" + domainUid + "/quota/"
				+ mailboxLetterPath + "/" + (mailbox.value.type == Type.user ? "user." : "") + mailbox.value.name;
		shell.append("test -e " + mailboxQuotaFile + " && echo \"Fixing " + mailboxQuotaFile
				+ " directory\" && chown cyrus:mail " + mailboxQuotaFile + " && chmod 700 " + mailboxQuotaFile + "\n");

		nodeClient.writeFile(shellName, new ByteArrayInputStream(shell.toString().getBytes()));
		NCUtils.execNoOut(nodeClient, "chmod +x " + shellName);

		ExitList result = NCUtils.exec(nodeClient, shellName);
		logger.info("Fixing permission of Cyrus BAL {} success", mailboxName);
		result.forEach(r -> {
			if (!Strings.isNullOrEmpty(r)) {
				logger.info(r);
			}
		});

		NCUtils.execNoOut(nodeClient, "rm -f " + shellName);
	}

	private char getLetterPath(char letter) {
		if (!Character.isLetter(letter)) {
			letter = 'q';
		}

		return letter;
	}

	@Override
	public DefaultFolder.Status checkAndRepairDefaultFolders(BmContext context, String domainUid,
			ItemValue<Mailbox> mailbox, boolean repair) throws ServerFault {

		ItemValue<Server> server = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(mailbox.value.dataLocation);

		Status status = new DefaultFolder.Status();

		switch (mailbox.value.type) {
		case group:
		case resource:
		case mailshare:
			return checkAndRepairMailshareDefaultFolders(domainUid, mailbox, repair, server, status,
					DefaultFolder.MAILSHARE_FOLDERS);
		case user:
		default:
			return checkAndRepairUserDefaultFolders(domainUid, mailbox, repair, server, status,
					DefaultFolder.USER_FOLDERS);
		}

	}

	private Status checkAndRepairMailshareDefaultFolders(String domainUid, ItemValue<Mailbox> mailbox, boolean repair,
			ItemValue<Server> server, Status status, Set<DefaultFolder> folders) {

		try (StoreClient sc = new StoreClient(server.value.ip, 1143, "admin0", Token.admin0())) {
			return checkAndRepairDefaultFolders(domainUid, mailbox, repair, folders, server, status, sc);
		}
	}

	private Status checkAndRepairUserDefaultFolders(String domainUid, ItemValue<Mailbox> mailbox, boolean repair,
			ItemValue<Server> server, Status status, Set<DefaultFolder> folders) {
		try (Sudo sudo = Sudo.forLogin(mailbox.value.name, domainUid);
				StoreClient sc = new StoreClient(server.value.ip, 1143, mailbox.value.name + "@" + domainUid,
						sudo.context.getSessionId())) {
			return checkAndRepairDefaultFolders(domainUid, mailbox, repair, folders, server, status, sc);
		}
	}

	private Status checkAndRepairDefaultFolders(String domainUid, ItemValue<Mailbox> mailbox, boolean repair,
			Set<DefaultFolder> defaultFolders, ItemValue<Server> server, Status status, StoreClient sc) {

		if (!sc.login()) {
			throw new ServerFault(String.format("Unable to connect to IMAP server %s as %s", server.value.ip,
					mailbox.value.name + "@" + domainUid));
		}

		defaultFolders.stream().forEach(df -> {

			String mailboxName = getMailboxName(mailbox, domainUid, df.name);

			if (!sc.isExist(mailboxName)) {
				logger.info("[{}] '{}' missing.", mailbox.value.name, df.name);
				status.missing.add(df);
				return;
			}

			if (!Strings.isNullOrEmpty(df.specialuse)) {
				AnnotationList annotations = sc.getAnnotation(df.name);
				Annotation specialuseAnnotation = annotations.get("/specialuse");
				if (specialuseAnnotation == null || !df.specialuseEquals(specialuseAnnotation.valuePriv)) {
					status.invalidSpecialuse.add(df);
					return;
				}
			}
		});

		if (repair) {
			if (status.missing.size() != 0) {
				List<String> created = createMissingFolders(domainUid, server.value, mailbox, status.missing);

				status.fixed = status.missing.stream().filter(df -> created.contains(df.name))
						.collect(Collectors.toSet());
				status.missing = status.missing.stream().filter(df -> !created.contains(df.name))
						.collect(Collectors.toSet());
			}

			if (status.invalidSpecialuse.size() != 0) {
				INodeClient nodeClient = NodeActivator.get(server.value.address());

				Set<DefaultFolder> fixed = new HashSet<>();
				status.invalidSpecialuse.forEach(df -> {
					ExitList res = NCUtils.exec(nodeClient, String.format(
							"/usr/sbin/cvt_xlist_specialuse user/%s/%s@%s", mailbox.value.name, df.name, domainUid));
					if (res.getExitCode() == 0) {
						fixed.add(df);
						status.fixed.add(df);
					}
				});
				status.invalidSpecialuse.removeAll(fixed);

				NCUtils.exec(nodeClient, "chown cyrus:mail /var/lib/cyrus/*");
			}
		}

		return status;
	}

	private String getMailboxName(ItemValue<Mailbox> mailbox, String domainUid, String name) {
		switch (mailbox.value.type) {
		case group:
		case resource:
		case mailshare:
			return mailbox.uid + "/" + name + "@" + domainUid;
		case user:
		default:
			return name;
		}
	}

	private List<String> createMissingFolders(String domainUid, Server server, ItemValue<Mailbox> mailbox,
			Set<DefaultFolder> missing) {
		if (mailbox.value.type == Type.user) {
			return MailboxOps.createUserFolders(domainUid, server, mailbox.value.name, missing);
		}

		return MailboxOps.createMailshareFolders(domainUid, server, mailbox.value.name, missing);
	}

	@Override
	public List<MailFolder> checkAndRepairAcl(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			List<AccessControlEntry> acls, boolean repair) throws ServerFault {

		if (MigrationPhase.migrationPhase) {
			logger.info("Skipping checkAndRepairAcl of mailbox {}, system is in migration phase", mailbox);
			return new ArrayList<MailFolder>();
		}

		ItemValue<Server> server = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(mailbox.value.dataLocation);

		List<MailFolder> ret = new ArrayList<MailFolder>();
		Map<String, Acl> cyrusAcl = new DbAclToCyrusAcl(domainUid, acls, mailbox).get();

		try (StoreClient sc = new StoreClient(server.value.ip, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault("Fail to login: admin0");
			}
			String boxName = boxname(mailbox.value, domainUid);
			ListResult lr = sc.listSubFoldersMailbox(boxName);

			lr.add(new ListInfo(boxName, true));
			for (ListInfo li : lr) {
				Map<String, Acl> currentAcl = sc.listAcl(li.getName());

				currentAcl.entrySet().stream().filter(e -> {
					Acl a = cyrusAcl.get(e.getKey());
					return a == null || a == Acl.NOTHING;
				}).forEach(e -> {
					try {

						MailFolder f = new MailFolder();
						f.name = li.getName();
						f.type = MailFolder.Type.normal;
						f.rootUri = "imap://" + mailbox.uid;
						ret.add(f);

						if (repair) {
							sc.deleteAcl(li.getName(), e.getKey());
							logger.info("deleteACL mailbox {}, consumer {}", li.getName(), e.getKey());
						} else {
							logger.info("need to deleteACL mailbox {}, consumer {}", li.getName(), e.getKey());
						}

					} catch (IMAPException e1) {
						throw new ServerFault(e1);
					}
				});

				cyrusAcl.entrySet().stream().filter(e -> e.getValue() != Acl.NOTHING).map(e -> {
					if (!e.getKey().equals("admin0")) {
						Set<String> defaultFolders = DefaultFolder.MAILSHARE_FOLDERS_NAME;
						if (mailbox.value.type == Type.user) {
							defaultFolders = DefaultFolder.USER_FOLDERS_NAME;
						}

						for (String defaultFolder : defaultFolders) {
							if (li.getName().equals(boxName.replace("@", "/" + defaultFolder + "@"))
									&& e.getValue().isX()) {
								Acl newAcl = new Acl(e.getValue().toString());
								newAcl.setX(false);
								return new AbstractMap.SimpleEntry<>(e.getKey(), newAcl);
							}
						}
					}

					return e;
				}).filter(entry -> {
					Acl a = currentAcl.get(entry.getKey());
					return a == null || !a.equals(entry.getValue());
				}).forEach(e -> {
					try {

						MailFolder f = new MailFolder();
						f.name = li.getName();
						f.type = MailFolder.Type.normal;
						f.rootUri = "imap://" + mailbox.uid;
						ret.add(f);

						if (repair) {
							sc.setAcl(li.getName(), e.getKey(), e.getValue());
							logger.info("set missing ACL on mailbox {} for consumer {}, acl {}", li.getName(),
									e.getKey(), e.getValue());
						} else {
							logger.info("need to set missing ACL on mailbox {} for consumer {}", li.getName(),
									e.getKey());
						}
					} catch (IMAPException e1) {
						throw new ServerFault(e1);
					}
				});

			}

		} catch (IMAPException e) {
			throw new ServerFault(e);
		}

		return ret;
	}

	private List<MailFolder> listSimpleFolders(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> server)
			throws ServerFault {
		List<MailFolder> folders = new ArrayList<>();

		try (StoreClient sc = new StoreClient(server.value.ip, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault(String.format("Fail to login to imap server : %s", server.value.address()));
			}

			ListResult imapFolers = sc.listSubFoldersMailbox(mailbox.value.name + "@" + domainUid);
			MailFolder root = new MailFolder();

			root.type = MailFolder.Type.normal;
			String rootUri = "imap://" + mailbox.uid;
			root.rootUri = rootUri;
			root.name = "INBOX"; // implicit
									// folder
									// (root)
			folders.add(root);
			for (ListInfo imapFolder : imapFolers) {

				String name = imapFolder.getName();
				name = name.substring(mailbox.value.name.length());
				name = name.substring(1, name.length() - ("@" + domainUid).length());
				logger.debug("create folder for imap folder {}", name);
				MailFolder folder = new MailFolder();

				folder.type = MailFolder.Type.normal;
				folder.name = name;
				folder.rootUri = rootUri;
				folders.add(folder);
			}
		}

		return folders;

	}

	private List<MailFolder> listUserFolders(BmContext context, String domainUid, ItemValue<Mailbox> mailbox)
			throws ServerFault {

		ItemValue<User> user = context.provider().instance(IUser.class, domainUid).getComplete(mailbox.uid);
		ItemValue<Server> server = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(user.value.dataLocation);

		List<MailFolder> folders = new ArrayList<>();

		try (Sudo pass = Sudo.forUser(user, domainUid);
				StoreClient sc = new StoreClient(server.value.address(), 1143, user.value.login + "@" + domainUid,
						pass.context.getSessionId())) {
			if (!sc.login()) {
				throw new ServerFault(String.format("Fail to login: %s@%s", user.value.login, domainUid));
			}
			NameSpaceInfo ni = sc.namespace();
			String shared = ni.getMailShares().get(0);
			String others = ni.getOtherUsers().get(0);
			ListResult lr = sc.listAll();
			for (ListInfo li : lr) {
				String n = li.getName();
				if (li.isSelectable() && !n.startsWith(shared) && !n.startsWith(others)) {
					sc.select(li.getName());
					MailFolder folder = new MailFolder();
					folder.type = MailFolder.Type.normal;
					folder.name = n;
					folder.rootUri = "imap://" + mailbox.uid;
					folders.add(folder);
				}
			}
		} catch (IMAPException e) {
			throw new ServerFault(e);
		}

		return folders;
	}

	@Override
	public MailboxQuota getQuota(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		String box = boxname(value.value, domainUid);
		ItemValue<Server> server = getServer(context, value.value.dataLocation);

		QuotaInfo q = new CyrusService(server.value.address()).getQuota(box);
		MailboxQuota ret = new MailboxQuota();

		if (q.isEnable()) {
			ret.quota = q.getLimit();
		}
		ret.used = q.getUsage();
		return ret;
	}

	@Override
	public void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer) {

		logger.info("Move {} MAILBOX from {} to {}", mailbox.value.name, sourceServer.uid, dstServer.uid);

		String boxName = boxname(mailbox.value, domainUid);

		CyrusService cyrus = new CyrusService(sourceServer.value.address());
		CyrusService toCyrus = new CyrusService(dstServer.value.address());

		cyrus.xfer(boxName, domainUid, dstServer);

		try {
			// FIXME setQuota fail... ( dunno why : A1 NO Mailbox does not
			// exist)
			if (mailbox.value.routing == Mailbox.Routing.internal) {
				if (mailbox.value.quota != null) {
					toCyrus.setQuota(boxName, mailbox.value.quota);
				} else {
					toCyrus.setQuota(boxName, 0);
				}
			} else {
				toCyrus.setQuota(boxName, 1);
			}
		} catch (Exception e) {
			logger.error("setQuota failed", e);
		}
	}

	@Override
	public void rewriteCyrusConfiguration(String serverUid) {
		SystemConf sysConf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class).getValues();
		int maxChild = getMaxChild(sysConf);
		int retention = getRetention(sysConf);
		IServer service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				"default");
		CyrusHsm hsm = new CyrusHsm(service, serverUid);
		hsm.write();
		Cyrus cyrusConf = new Cyrus(service, serverUid, maxChild, retention);
		cyrusConf.write();
	}

	private int getRetention(SystemConf sysConf) {
		int retention = Cyrus.DEFAULT_RETENTION;
		if (sysConf.integerValue(SysConfKeys.cyrus_expunged_retention_time.name()) != null) {
			retention = sysConf.integerValue(SysConfKeys.cyrus_expunged_retention_time.name());
		}
		return retention;
	}

	private int getMaxChild(SystemConf sysConf) {
		int maxChild = Cyrus.DEFAULT_MAX_CHILD;
		if (sysConf.integerValue(SysConfKeys.imap_max_child.name()) != null) {
			maxChild = sysConf.integerValue(SysConfKeys.imap_max_child.name());
		}
		return maxChild;
	}

}
