/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.cyrus;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.impl.VertxThreadFactory;

import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.imap.Acl;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.service.common.DefaultFolder;

public abstract class CyrusAclService {
	protected final String backendAddress;
	protected static final Logger logger = LoggerFactory.getLogger(CyrusAclService.class);
	public static final int MAX_TASK_COUNT = 10;
	private static final ExecutorService executer = new ThreadPoolExecutor(MAX_TASK_COUNT, MAX_TASK_COUNT, 15L,
			TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new VertxThreadFactory("bm-acl"));

	private CyrusAclService(String backendAddress) {
		this.backendAddress = backendAddress;
	}

	public abstract void setAcl(String mailbox, Map<String, Acl> acl);

	public static CyrusAclService get(String backendAddress) {
		if (productionMode()) {
			return async(backendAddress);
		} else {
			return sync(backendAddress);
		}
	}

	public static CyrusAclService sync(String backendAddress) {
		return new SynchronousAclService(backendAddress);
	}

	public static CyrusAclService async(String backendAddress) {
		return new AsynchronousAclService(backendAddress);
	}

	private static boolean productionMode() {
		return !testMode();
	}

	private static boolean testMode() {
		for (StackTraceElement element : Arrays.asList(Thread.currentThread().getStackTrace())) {
			if (element.getClassName().startsWith("org.junit.")) {
				return true;
			}
		}
		return false;
	}

	protected void setAclTask(String mailbox, Map<String, Acl> acl) throws ServerFault {

		if (MigrationPhase.migrationPhase) {
			logger.info("Skipping setAcl of mailbox {}, system is in migration phase", mailbox);
			return;
		}

		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault("error during settings acl on [" + mailbox + "]: "
						+ "Login as admin0 failed, server " + backendAddress);
			}
			ListResult mailboxes = sc.listSubFoldersMailbox(mailbox);
			// add root
			mailboxes.add(new ListInfo(mailbox, true));
			for (ListInfo mb : mailboxes) {
				Map<String, Acl> current = sc.listAcl(mb.getName());

				// delete no more present subject
				current.entrySet().stream().filter(e -> {
					Acl currentAcl = acl.get(e.getKey());
					return currentAcl == null || currentAcl == Acl.NOTHING;
				}).forEach(e -> {
					try {
						sc.deleteAcl(mb.getName(), e.getKey());
					} catch (IMAPException e1) {
						throw new ServerFault(e1);
					}
				});

				acl.entrySet().stream().filter(e -> e.getValue() != Acl.NOTHING).map(e -> {
					if (!e.getKey().equals("admin0")) {
						Set<String> defaultFolders = DefaultFolder.MAILSHARE_FOLDERS_NAME;
						if (mailbox.startsWith("user/")) {
							defaultFolders = DefaultFolder.USER_FOLDERS_NAME;
						}

						for (String defaultFolder : defaultFolders) {
							if (mb.getName().equals(mailbox.replace("@", "/" + defaultFolder + "@"))
									&& e.getValue().isX()) {
								Acl newAcl = new Acl(e.getValue().toString());
								newAcl.setX(false);
								return new AbstractMap.SimpleEntry<>(e.getKey(), newAcl);
							}
						}
					}

					return e;
				}).filter(entry -> {
					// filter out unmodified acl
					Acl existing = current.get(entry.getKey());
					return existing == null || existing != entry.getValue();
				}).forEach(e -> {
					try {
						sc.setAcl(mb.getName(), e.getKey(), e.getValue());
					} catch (IMAPException e1) {
						throw new ServerFault(e1);
					}
				});
			}

		} catch (Exception e) {
			logger.error("Error while setting acls on {}", mailbox, e);
		}
	}

	private static class SynchronousAclService extends CyrusAclService {

		private SynchronousAclService(String backendAddress) {
			super(backendAddress);
		}

		@Override
		public void setAcl(String mailbox, Map<String, Acl> acl) {
			super.setAclTask(mailbox, acl);
		}

	}

	private static class AsynchronousAclService extends CyrusAclService {

		private AsynchronousAclService(String backendAddress) {
			super(backendAddress);
		}

		@Override
		public void setAcl(String mailbox, Map<String, Acl> acl) {
			executer.execute(() -> {
				synchronized (mailbox.intern()) {// NOSONAR
					super.setAclTask(mailbox, acl);
					logger.info("ACLs of {} updated.", mailbox);
				}
			});
		}

	}

}
