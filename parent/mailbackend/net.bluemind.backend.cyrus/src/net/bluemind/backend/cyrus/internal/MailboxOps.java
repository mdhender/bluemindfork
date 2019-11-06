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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.Sudo;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.Acl;
import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.NodeUtils;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.User;

public final class MailboxOps {
	private static final Logger logger = LoggerFactory.getLogger(MailboxOps.class);

	/**
	 * @param domain
	 * @param srv
	 * @param mailboxUid
	 */
	public static void create(String domainUid, ItemValue<Server> srv, String mailboxUid) {
		try (StoreClient sc = new StoreClient(srv.value.address(), 1143, "admin0", Token.admin0())) {
			sc.login();
			String boxName = Uids.mailboxToCyrus(mailboxUid);
			CyrusPartition partition = CyrusPartition.forServerAndDomain(srv, domainUid);
			CreateMailboxResult ok = sc.createMailbox(boxName, partition.name);
			logger.info("MAILBOX create: {} for '{}' on partition '{}'", ok.isOk() ? "OK" : ok.getMessage(), boxName,
					partition.name);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * @param domain
	 * @param srv
	 * @param mailboxUid
	 *            mailbox container uid
	 */
	public static void annotate(Server srv, String mailboxUid) {
		try (StoreClient sc = new StoreClient(srv.address(), 1143, "admin0", Token.admin0())) {
			sc.login();
			String cyrusBox = Uids.mailboxToCyrus(mailboxUid);
			if (mailboxUid.startsWith("mailbox_user/")) {
				sc.setAnnotation(
						"\"" + cyrusBox + "\" \"/vendor/cmu/cyrus-imapd/sharedseen\" (\"value.shared\" \"true\")");
			} else if (mailboxUid.startsWith("mailbox_")) {
				sc.setAnnotation("\"" + cyrusBox + "\" \"/vendor/cmu/cyrus-imapd/sieve\" (\"value.shared\" \""
						+ cyrusBox + ".sieve\")");

			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void rename(ItemValue<Server> srv, String prevBox, String newBox) {
		try (StoreClient sc = new StoreClient(srv.value.address(), 1143, "admin0", Token.admin0())) {
			sc.login();
			logger.info("RENAMING {} to {}", prevBox, newBox);
			boolean result = sc.rename(prevBox, newBox);
			logger.info("RENAME {} -> {}: {}", prevBox, newBox, result);
		} catch (IMAPException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void restartCyrus(IServer service, String serverUid) throws ServerFault {
		NodeUtils.exec(service, serverUid, "service bm-cyrus-imapd restart");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static String getMailboxPrefix(ItemValue<Mailbox> mailbox) {
		return (mailbox.value.type == Type.user ? "user/" : "") + mailbox.value.name;
	}

	public static void setAcls(ItemValue<Mailbox> owner, String domain, Map<String, Acl> acls) throws ServerFault {
		String location = null;
		try {
			IServer srv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
					InstallationId.getIdentifier());
			location = srv.getComplete(owner.value.dataLocation).value.address();
			logger.info("resolved {} => {}", owner.value.dataLocation, location);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		new CyrusService(location)
				.setAcl((owner.value.type == Type.user ? "user/" : "") + owner.value.name + "@" + domain, acls);
	}

	public static void xfer(String domainUid, User owner, Server prev, ItemValue<Server> dest) {
		try (StoreClient sc = new StoreClient(prev.address(), 1143, "admin0", Token.admin0())) {
			sc.login();
			sc.xfer("user/" + owner.login + "@" + domainUid, dest.value.address(),
					CyrusPartition.forServerAndDomain(dest, domainUid).name);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * @param container
	 * @param srv
	 * @param boxContainer
	 * @return
	 */
	public static List<String> createUserFolders(String domainUid, Server srv, String login,
			Set<DefaultFolder> folders) {
		List<String> created = new LinkedList<>();
		try (Sudo sudo = Sudo.forLogin(login, domainUid);
				StoreClient sc = new StoreClient(srv.address(), 1143, login + "@" + domainUid,
						sudo.context.getSessionId())) {
			logger.debug("Sudo returned '{}' for {}", sudo.context.getSessionId(), login);

			if (sc.login()) {
				for (DefaultFolder defaultFolder : folders) {
					if (sc.create(defaultFolder.name, defaultFolder.specialuse)) {
						created.add(defaultFolder.name);
						sc.subscribe(defaultFolder.name);

						immutableFolder(sc, defaultFolder.name);
					} else {
						logger.error("Fail to create {} for login {} ", defaultFolder.name, login);
					}
				}
			} else {
				logger.error(" *** Fail to login {}, {}, {}", srv.address(), login, sudo.context.getSessionId());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("user imap folders of {}@{} initialized : {}", login, domainUid, created);
		return created;
	}

	/**
	 * @param container
	 * @param srv
	 * @param boxContainer
	 * @return
	 */
	public static List<String> createMailshareFolders(String domainUid, Server srv, String mailshareName,
			Set<DefaultFolder> folders) {
		List<String> created = new LinkedList<>();
		try (StoreClient sc = new StoreClient(srv.address(), 1143, "admin0", Token.admin0())) {
			if (sc.login()) {
				for (DefaultFolder f : folders) {
					String folder = mailshareName + "/" + f.name + "@" + domainUid;
					if (sc.create(folder)) {
						created.add(f.name);

						immutableFolder(sc, folder);
					} else {
						created.add(f.name);
						logger.error("Fail to create folder {} for mailshare {} ", f.name, mailshareName);
					}
				}
			} else {
				logger.error(" *** Fail to login as admin0");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("user imap folders of {}@{} initialized : {}", mailshareName, domainUid, created);
		return created;
	}

	protected static void immutableFolder(StoreClient sc, String f) throws IMAPException {
		sc.listAcl(f).entrySet().stream().filter(e -> e.getValue().isX() && !e.getKey().equals("admin0")).forEach(e -> {
			e.getValue().setX(false);
			try {
				sc.setAcl(f, e.getKey(), e.getValue());
			} catch (IMAPException imape) {
				logger.error(String.format("Unable to set %s as immutable folder %s", f, imape.getMessage()), imape);
			}
		});
	}
}
