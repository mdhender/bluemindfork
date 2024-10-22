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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.serviceprovider.SPResolver;
import net.bluemind.system.api.ArchiveKind;

public class SpoolStoreFactory implements ISdsBackingStoreFactory {

	private static final Logger logger = LoggerFactory.getLogger(SpoolStoreFactory.class);
	private IServiceProvider serviceProvider;
	private List<ItemValue<Server>> backends;

	public SpoolStoreFactory() {
		this.serviceProvider = SPResolver.get().resolve(Token.admin0());
		this.backends = serviceProvider.instance(IServer.class, "default").allComplete().stream()
				.filter(ivs -> ivs.value.tags.contains(TagDescriptor.mail_imap.getTag())).collect(Collectors.toList());

		for (ItemValue<Server> b : backends) {
			setupFolders(b);
		}

	}

	private void setupFolders(ItemValue<Server> b) {
		List<String> suffix = new LinkedList<>();
		for (char c = 'a'; c <= 'f'; c++) {
			suffix.add("" + c);
		}
		for (char c = '0'; c <= '9'; c++) {
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

		for (String s : realSuffix) {
			nc.mkdirs("/var/spool/cyrus/data/by_hash/" + s);
			nc.mkdirs("/var/spool/bm-hsm/data/by_hash/" + s);
		}
	}

	@Override
	public ArchiveKind kind() {
		return ArchiveKind.Cyrus;
	}

	@Override
	public ISdsBackingStore create(Vertx vertx, JsonObject configuration, String dataLocation) {
		return new SpoolBackingStore(serviceProvider, Topology.get().datalocation(dataLocation));
	}

}
