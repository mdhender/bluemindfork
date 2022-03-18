/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.replication.testhelper;

import java.io.ByteArrayInputStream;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.syncclient.mgmt.api.ISyncClientMgmt;
import net.bluemind.backend.cyrus.syncclient.mgmt.api.ISyncClientObserver;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class CyrusReplicationHelper {
	private static final Logger logger = LoggerFactory.getLogger(CyrusReplicationHelper.class);

	private final String cyrusIp;
	private final CompletableFuture<Void> start;
	private final CompletableFuture<Void> stop;
	private final ISyncClientObserver obs;
	private final ISyncClientMgmt syncClient;

	private ItemValue<Server> server;

	private static final Executor observersPool = Executors.newCachedThreadPool();

	/**
	 * Defaults to <code>junit</code> channel
	 * 
	 * @param cyrusIp
	 */
	public CyrusReplicationHelper(String cyrusIp) {
		this(cyrusIp, "junit");
	}

	public CyrusReplicationHelper(String cyrusIp, String channel) {
		this.cyrusIp = cyrusIp;
		start = new CompletableFuture<>();
		stop = new CompletableFuture<>();
		this.obs = new ISyncClientObserver() {

			@Override
			public void replicationStopped() {
				if (!stop.isDone()) {
					logger.info("CYRUS REPLICATION STOPPED.");
					stop.complete(null);
				}
			}

			@Override
			public void replicationStarted(boolean isRestart) {
				if (!start.isDone()) {
					logger.info("CYRUS REPLICATION STARTED.");
					start.complete(null);
				}
			}

			@Override
			public void log(String s) {
				logger.info("REPLICATION: {}", s);
			}
		};

		syncClient = ISyncClientMgmt.builder()//
				.vertx(VertxPlatform.getVertx())//
				.cyrusBackendAddress(cyrusIp)//
				.replicationChannel(channel)//
				.observer(obs)//
				.observersExecutor(observersPool)//
				.build();
	}

	public void installReplication() {
		logger.info("Installing replication on Cyrus at {}", cyrusIp);
		INodeClient nc = NodeActivator.get(cyrusIp);
		byte[] imapConf = nc.read("/etc/imapd.conf");
		String imapStr = new String(imapConf).replace("\r", "");
		int idx = imapStr.indexOf("#SYNCCONF");
		if (idx > 0) {
			imapStr = imapStr.substring(0, idx - 1);
		}

		// see
		// https://www.cyrusimap.org/dev/imap/reference/manpages/configs/imapd.conf.html
		StringBuilder replConf = new StringBuilder("\n#SYNCCONF\n");
		replConf.append("sync_log: 1").append('\n');
		replConf.append("sync_log_chain: 1").append('\n');
		replConf.append("sync_log_channels: junit").append('\n');
		replConf.append("junit_sync_authname: admin0").append('\n');
		replConf.append("junit_sync_password: admin").append('\n');
		replConf.append("junit_sync_realm: admin").append('\n');
		replConf.append("junit_sync_repeat_interval: 0").append('\n');
		replConf.append("junit_sync_host: ").append(getMyIpAddress()).append('\n');
		replConf.append("junit_sync_port: 2501").append('\n');
		replConf.append("junit_sync_try_imap: 0").append('\n');
		// because our cyrus configuration in docker does not match what we package
		replConf.append("annotation_definitions: /etc/cyrus-annotations").append('\n');
		addConversationsSettings(replConf);
		replConf.append("mailbox_default_options: 4\n");
		String updatedConf = imapStr + replConf.toString();
		nc.writeFile("/etc/imapd.conf", new ByteArrayInputStream(updatedConf.getBytes()));
		CyrusService cs = new CyrusService(cyrusIp);
		cs.refreshAnnotations();
		cs.reload();
		this.server = cs.server();
	}

	private static void addConversationsSettings(StringBuilder conf) {
		conf.append("conversations: 1").append("\n");
		conf.append("conversations_db: twoskip").append("\n");
		conf.append("conversations_expire_after: 9000d").append("\n");
		conf.append("conversations_max_thread: 100").append("\n");
	}

	public ItemValue<Server> server() {
		return server;
	}

	public static String getMyIpAddress() {
		String ret = "127.0.0.1";
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for (InterfaceAddress ia : addresses) {
					if (ia.getBroadcast() == null) {
						// ipv6
						continue;
					}
					String tmp = ia.getAddress().getHostAddress();
					if (!tmp.startsWith("127")) {
						return tmp;
					}
				}
			}
		} catch (SocketException e) {
			// yeah yeah
		}
		return ret;
	}

	public CompletableFuture<Void> startReplication() {
		if (!start.isDone()) {
			syncClient.startRollingReplication();
		}
		return start;
	}

	public CompletableFuture<Void> stopReplication() {
		if (!stop.isDone()) {
			syncClient.stopRollingReplication();
		}
		return stop;
	}

}
