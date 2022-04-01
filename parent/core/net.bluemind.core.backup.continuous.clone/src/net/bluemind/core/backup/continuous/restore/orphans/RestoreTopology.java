/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.orphans;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.TopologyMapping;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.IInstallation;

public class RestoreTopology {

	private static final Logger logger = LoggerFactory.getLogger(RestoreTopology.class);

	private final IServiceProvider target;
	private final TopologyMapping topologyMapping;

	public class PromotingServer {
		public ItemValue<Server> leader;
		public ItemValue<Server> clone;
	}

	public RestoreTopology(IServiceProvider target, TopologyMapping topologyMapping) {
		this.target = target;
		this.topologyMapping = topologyMapping;
	}

	public Map<String, PromotingServer> restore(IServerTaskMonitor monitor, List<DataElement> servers) {
		ValueReader<ItemValue<Server>> topoReader = JsonUtils.reader(new TypeReference<ItemValue<Server>>() {
		});
		IServer topoApi = target.instance(IServer.class, "default");
		AtomicBoolean resetES = new AtomicBoolean();
		List<PromotingServer> touched = new LinkedList<>();
		servers.forEach(srvDE -> {
			if (!srvDE.key.valueClass.equals(Server.class.getCanonicalName())) {
				return;
			}
			String asStr = new String(srvDE.payload);
			ItemValue<Server> leader = topoReader.read(asStr);
			ItemValue<Server> srv = topoReader.read(asStr);
			PromotingServer ps = new PromotingServer();
			ps.leader = leader;
			ps.clone = srv;

			TaskRef srvTask;
			srv.value.ip = topologyMapping.ipAddressForUid(srv.uid, srv.value.ip);
			ItemValue<Server> exist = topoApi.getComplete(srv.uid);
			if (exist != null) {
				logger.info("UPDATE SRV {}", srv);
				try {
					srvTask = topoApi.update(srv.uid, srv.value);
					if (srv.value.tags.contains("bm/es") && !exist.value.tags.contains("bm/es")) {
						resetES.set(true);
					}
				} catch (ServerFault sf) {
					srvTask = null;
					logger.warn("ServerUpdate is not possible: {}", sf.getMessage());
				}
			} else {
				logger.info("CREATE SRV {}", srv);
				srvTask = topoApi.create(srv.uid, srv.value);
				if (srv.value.tags.contains("bm/es")) {
					resetES.set(true);
				}
			}
			touched.add(ps);
			String wait = logStreamWait(srvTask);
			monitor.log(srv.uid + ": " + wait);
		});
		if (resetES.get()) {
			monitor.log("Reset ES indexes...");
			do {
				Optional<ItemValue<Server>> exist = Topology.get().anyIfPresent("bm/es");
				if (exist.isPresent()) {
					break;
				} else {
					try {
						monitor.log("Waiting for bm/es to appear in topology...");
						Thread.sleep(200);
					} catch (Exception e) {
						Thread.currentThread().interrupt();
					}
				}
			} while (true);
			target.instance(IInstallation.class).resetIndexes();
		}

		Map<String, PromotingServer> serverByUid = touched.stream()
				.collect(Collectors.toMap(iv -> iv.leader.uid, iv -> iv, (iv1, iv2) -> iv2));

		monitor.progress(1, "Dealt with topology");
		return serverByUid;
	}

	public String logStreamWait(TaskRef ref) {
		if (ref == null) {
			return "[nothing to do]";
		}
		ITask taskApi = target.instance(ITask.class, ref.id + "");
		return GenericStream.streamToString(taskApi.log());
	}

}
