/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sds.store.cyrusspool;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ArchiveKind;

public class SpoolStoreFactory implements ISdsBackingStoreFactory {

	private static final Logger logger = LoggerFactory.getLogger(SpoolStoreFactory.class);
	private ServerSideServiceProvider prov;
	private List<ItemValue<Server>> backends;

	public SpoolStoreFactory() {
		this.prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		this.backends = Topology.get().nodes().stream().filter(s -> s.value.tags.contains("mail/imap"))
				.collect(Collectors.toList());

		for (ItemValue<Server> b : backends) {
			setupFolders(b);
		}

	}

	private void setupFolders(ItemValue<Server> b) {
		List<String> suffix = new LinkedList<>();
		for (char c = 'a'; c < 'f'; c++) {
			suffix.add("" + c);
		}
		for (char c = '0'; c < '9'; c++) {
			suffix.add("" + c);
		}
		int len = suffix.size();
		List<String> realSuffix = new ArrayList<>(len * len * len);
		for (int i = 0; i < suffix.size(); i++) {
			for (int j = 0; j < suffix.size(); j++) {
				realSuffix.add(suffix.get(i) + "/" + suffix.get(j));
			}
		}
		logger.info("Setting up directories on backend {}", b);
		INodeClient nc = NodeActivator.get(b.value.address());
		CountDownLatch cdl = new CountDownLatch(realSuffix.size() * 2);
		ProcessHandler ph = new ProcessHandler() {

			@Override
			public void log(String l, boolean isContinued) {
				// ok
			}

			@Override
			public void completed(int exitCode) {
				cdl.countDown();
			}

			@Override
			public void starting(String taskRef) {
				// ok
			}

		};
		for (String s : realSuffix) {
			nc.asyncExecute(ExecRequest.anonymousWithoutOutput("mkdir -p /var/spool/cyrus/data/by_hash/" + s), ph);
			nc.asyncExecute(ExecRequest.anonymousWithoutOutput("mkdir -p /var/spool/bm-hsm/data/by_hash/" + s), ph);
		}
		try {
			cdl.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public ArchiveKind kind() {
		return ArchiveKind.Cyrus;
	}

	@Override
	public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
		return new SpoolBackingStore(vertx, prov, backends);
	}

}
