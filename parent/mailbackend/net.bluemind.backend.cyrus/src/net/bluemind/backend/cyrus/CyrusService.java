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
package net.bluemind.backend.cyrus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.backend.cyrus.internal.files.Annotations;
import net.bluemind.backend.cyrus.internal.files.CyrusPartitions;
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
import net.bluemind.imap.QuotaInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.User;

public class CyrusService {

	private static final Logger logger = LoggerFactory.getLogger(CyrusService.class);

	private final String backendAddress;
	private final INodeClient nodeClient;
	private final ItemValue<Server> backend;

	public CyrusService(String backendAddress) throws ServerFault {
		this(serverByAddress(backendAddress));
	}

	private static ItemValue<Server> serverByAddress(String backendAddress) {
		IServer serversApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		Optional<ItemValue<Server>> found = serversApi.allComplete().stream()
				.filter(srv -> backendAddress.equals(srv.value.ip) || backendAddress.equals(srv.value.fqdn))
				.findFirst();
		return found.orElseThrow(() -> ServerFault.notFound("server with address '" + backendAddress + "' is missing"));
	}

	public ItemValue<Server> server() {
		return backend;
	}

	public CyrusService(ItemValue<Server> srv) throws ServerFault {
		this.backend = srv;
		logger.debug("===== CyrusService for uid: {}, addr: {} =====", backend.uid, backend.value.address());
		this.backendAddress = srv.value.address();
		nodeClient = NodeActivator.get(backendAddress);

	}

	public void reload() throws ServerFault {
		logger.info("Attempting cyrus restart on {}", backendAddress);

		ExitList cyrusRestartOp = NCUtils.exec(nodeClient, "service bm-cyrus-imapd restart", 90, TimeUnit.SECONDS);
		if (cyrusRestartOp.getExitCode() != 0) {
			String output = cyrusRestartOp.stream().collect(Collectors.joining("; "));
			throw new ServerFault("Cyrus restart failed (output: " + output + ")");
		}
		new NetworkHelper(backendAddress).waitForListeningPort(1143, 30, TimeUnit.SECONDS);
	}

	public void reloadSds() throws ServerFault {
		logger.info("Attempting sds restart on {}", backendAddress);
		ExitList sdsRestartOp = NCUtils.exec(nodeClient, "service bm-sds-proxy restart", 90, TimeUnit.SECONDS);
		if (sdsRestartOp.getExitCode() != 0) {
			String output = sdsRestartOp.stream().collect(Collectors.joining("; "));
			throw new ServerFault("Sds restart failed (output: " + output + ")");
		}
		new NetworkHelper(backendAddress).waitForListeningPort(8091, 30, TimeUnit.SECONDS);
	}

	public CyrusPartition createPartition(String domainUid) throws ServerFault {
		CyrusPartition cp = CyrusPartition.forServerAndDomain(backend, domainUid);
		String partition = cp.name;

		NCUtils.execNoOut(nodeClient, "mkdir /var/spool/cyrus/data/" + partition, 1, TimeUnit.SECONDS);
		NCUtils.execNoOut(nodeClient, "chown cyrus:mail /var/spool/cyrus/data/" + partition, 1, TimeUnit.SECONDS);
		NCUtils.execNoOut(nodeClient, "mkdir /var/spool/cyrus/meta/" + partition, 1, TimeUnit.SECONDS);
		NCUtils.execNoOut(nodeClient, "chown cyrus:mail /var/spool/cyrus/meta/" + partition, 1, TimeUnit.SECONDS);
		NCUtils.execNoOut(nodeClient, "mkdir -p /var/spool/bm-hsm/cyrus-archives/" + partition, 1, TimeUnit.SECONDS);
		NCUtils.execNoOut(nodeClient, "chown cyrus:mail /var/spool/bm-hsm/cyrus-archives/" + partition, 1,
				TimeUnit.SECONDS);
		return cp;
	}

	public void refreshPartitions(List<String> domains) throws ServerFault {
		CyrusPartitions partitions = new CyrusPartitions(nodeClient);

		for (String domainUid : domains) {
			String partition = CyrusPartition.forServerAndDomain(backend, domainUid).name;
			partitions.add(partition, "/var/spool/cyrus/data/" + partition, "/var/spool/cyrus/meta/" + partition,
					"/var/spool/bm-hsm/cyrus-archives/" + partition);
		}
		partitions.write();
	}

	public void refreshAnnotations() throws ServerFault {
		Annotations anno = new Annotations(nodeClient);
		anno.write();
	}

	public void createBox(String boxName, String domainUid) throws ServerFault {
		CyrusPartition realPartition = CyrusPartition.forServerAndDomain(backend, domainUid);
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault(
						"createMbox failed for '" + boxName + "'. Login as admin0 failed, server " + backendAddress);
			}
			CreateMailboxResult result = sc.createMailbox(boxName, realPartition.name);
			logger.info("MAILBOX create: {} for '{}' on partition '{}'", result.isOk() ? "OK" : result.getMessage(),
					boxName, realPartition.name);

			if (!result.isOk()) {
				if (!result.getMessage().contains("NO Mailbox already exists")) {
					logger.error(
							"createMailbox failed for mbox '" + boxName + "', server said: " + result.getMessage());
					throw new ServerFault(
							"createMbox failed for '" + boxName + "'. server msg: " + result.getMessage());
				} else {
					logger.info("mbox " + boxName + " already exists, that's fine.");
				}
			}
			if (boxName.startsWith("user/")) {
				// we want shared seen flags for user mailboxes
				boolean annotated = sc.setMailboxAnnotation(boxName, "/vendor/cmu/cyrus-imapd/sharedseen",
						ImmutableMap.of("value.shared", "true"));
				if (!annotated) {
					logger.warn("Mailbox {} annotation for sharedseen FAILURE.", boxName);
				} else {
					logger.info("Mailbox {} annotation for sharedseen SUCCESS.", boxName);
				}
			}
		} catch (IMAPException e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(
					"error during mailbox [" + boxName + ":" + realPartition.name + "] creation " + e.getMessage());
		}
	}

	/**
	 * Delete Cyrus mailbox and subfolders<br>
	 * 
	 * @param boxName
	 * @param partition
	 * @throws ServerFault
	 */
	public void deleteBox(String boxName, String partition) throws ServerFault {
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault(
						"deleteBox failed for '" + boxName + "'. Login as admin0 failed, server " + backendAddress);
			}

			CreateMailboxResult result = sc.deleteMailboxHierarchy(boxName);
			logger.info("MAILBOX delete: {} for '{}' on partition '{}'", result.isOk() ? "OK" : result.getMessage(),
					boxName, partition);

			if (!result.isOk()) {
				if (!sc.select(boxName)) {
					// GEM-77
					// Mailbox does not exists, do not throw exception
					logger.warn("deleteBox failed: mailbox {} does not exist", boxName);
					return;
				}
				logger.error("deleteMailbox failed for mbox '" + boxName + "', server said: " + result.getMessage());
				throw new ServerFault("deleteMailbox failed for '" + boxName + "'. server msg: " + result.getMessage());
			}

		} catch (IMAPException e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(
					"error during mailbox [" + boxName + ":" + partition + "] deletion " + e.getMessage());
		}
	}

	public void renameBox(String pboxName, String boxName) throws ServerFault {
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault(
						"rename " + pboxName + " failed. " + "Login as admin0 failed, server " + backendAddress);
			}

			logger.info("rename {} to {}", pboxName, boxName);
			if (!sc.rename(pboxName, boxName)) {
				throw new ServerFault("rename " + pboxName + " failed");
			}
		} catch (IMAPException e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault("error during settings acl on [" + boxName + "]: " + e.getMessage());
		}
	}

	public void xfer(String boxName, String domainUid, ItemValue<Server> dest) throws ServerFault {
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault("error during transfert mailbox " + boxName + " to " + dest.value.address() + ". "
						+ "Login as admin0 failed, server " + backendAddress);
			}
			CyrusPartition partition = CyrusPartition.forServerAndDomain(dest, domainUid);
			logger.info("xfer {} to {} (partition {})", boxName, dest.value.address(), partition);
			if (!sc.xfer(boxName, dest.value.address(), partition.name)) {
				throw new ServerFault("error during transfert mailbox " + boxName + " to " + dest.value.address());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault("error during settings acl on [" + boxName + "]: " + e.getMessage());
		}
	}

	public void setAcl(String mailbox, Map<String, Acl> acl) {
		CyrusAclService.get(backendAddress).setAcl(mailbox, acl);
	}

	/**
	 * @param boxName eg. user/john@bm.lan
	 * @param quota   unit is KB
	 * @throws ServerFault
	 */
	public void setQuota(String boxName, int quota) throws ServerFault {
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault("error during setQuota on [" + boxName + "]: " + "Login as admin0 failed, server "
						+ backendAddress);
			}

			logger.info("set quota for {}: {}", boxName, quota);

			if (!sc.setQuota(boxName, quota)) {
				throw new ServerFault("setQuota failed");
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault("error during setQuota on [" + boxName + "]: " + e.getMessage());
		}
	}

	public Integer getUnSeenMessages(String domainUid, ItemValue<User> user) throws ServerFault {
		try (Sudo pass = Sudo.forUser(user, domainUid);
				StoreClient sc = new StoreClient(backendAddress, 1143, user.value.login + "@" + domainUid,
						pass.context.getSessionId())) {
			sc.login();
			return sc.getUnseen("INBOX");
		}
	}

	/**
	 * @param mailbox
	 * @return
	 * @throws ServerFault
	 */
	public QuotaInfo getQuota(String mailbox) throws ServerFault {
		QuotaInfo qi = null;
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault("error during getQuota on [" + mailbox + "]: " + "Login as admin0 failed, server "
						+ backendAddress);
			}
			logger.info("get quota for {}", mailbox);

			qi = sc.quota(mailbox);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault("error during getQuota on [" + mailbox + "]: " + e.getMessage());
		}

		return qi;
	}

	public boolean boxExist(String mailbox) throws ServerFault {
		try (StoreClient sc = new StoreClient(backendAddress, 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new ServerFault("error during boxExist on [" + mailbox + "]: " + "Login as admin0 failed");
			}
			logger.info("is {} exist", mailbox);

			return sc.isExist(mailbox);
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault("error during boxExist on [" + mailbox + "]: " + e.getMessage());
		}
	}

	public void reset() throws ServerFault {
		NCUtils.execNoOut(nodeClient, "/usr/share/bm-cyrus/resetCyrus.sh", 30, TimeUnit.SECONDS);
		NCUtils.execNoOut(nodeClient, "rm -rf /var/lib/cyrus/sync", 30, TimeUnit.SECONDS);
	}
}
