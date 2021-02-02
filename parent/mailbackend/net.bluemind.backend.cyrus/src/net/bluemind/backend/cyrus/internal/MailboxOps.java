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

import com.google.common.collect.ImmutableMap;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.Sudo;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.Acl;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public final class MailboxOps {

	private static final Logger logger = LoggerFactory.getLogger(MailboxOps.class);

	private MailboxOps() {
	}

	public static String getMailboxPrefix(ItemValue<Mailbox> mailbox) {
		return (mailbox.value.type == Type.user ? "user/" : "") + mailbox.value.name;
	}

	public static void setAcls(ItemValue<Mailbox> owner, String domain, Map<String, Acl> acls) throws ServerFault {
		ItemValue<Server> location = Topology.getIfAvailable().map(t -> {
			ItemValue<Server> backend = t.datalocation(owner.value.dataLocation);
			logger.info("(fast) resolved {} => {}", owner.value.dataLocation, backend);
			return backend;
		}).orElseGet(() -> {
			try {
				IServer srv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
						InstallationId.getIdentifier());
				ItemValue<Server> backend = srv.getComplete(owner.value.dataLocation);
				logger.info("(slow) resolved {} => {}", owner.value.dataLocation, backend);
				return backend;
			} catch (ServerFault sf) {
				throw sf;
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		});

		new CyrusService(location)
				.setAcl((owner.value.type == Type.user ? "user/" : "") + owner.value.name + "@" + domain, acls);
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
						addSharedSeenAnnotation(sc, defaultFolder.name);
						sc.subscribe(defaultFolder.name);
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
						addSharedSeenAnnotation(sc, folder);
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

	public static void addSharedSeenAnnotation(StoreClient sc, String folder) {
		boolean annotated = sc.setMailboxAnnotation(folder, "/vendor/cmu/cyrus-imapd/sharedseen",
				ImmutableMap.of("value.shared", "true"));
		if (!annotated) {
			logger.warn("Mailbox {} annotation for sharedseen FAILURE.", folder);
		} else {
			logger.info("Mailbox {} annotation for sharedseen SUCCESS.", folder);
		}
	}
}
