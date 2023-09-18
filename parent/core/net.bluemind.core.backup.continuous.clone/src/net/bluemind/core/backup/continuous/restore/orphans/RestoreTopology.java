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
import net.bluemind.server.api.TagDescriptor;
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

	private record ServerWithDataElement(ItemValue<Server> server, DataElement de) {
	}

	public Map<String, PromotingServer> restore(IServerTaskMonitor monitor, List<DataElement> servers) {
		ValueReader<ItemValue<Server>> topoReader = JsonUtils.reader(new TypeReference<ItemValue<Server>>() {
		});
		IServer topoApi = target.instance(IServer.class, "default");
		AtomicBoolean shouldResetElasticsearch = new AtomicBoolean();
		List<PromotingServer> touched = new LinkedList<>();

		/*
		 * We can have multiple versions of servers in the stream, first, we need to get
		 * the last server version
		 */

		Map<String, ServerWithDataElement> aggregatedServers = servers.stream()
				.filter(de -> Server.class.getCanonicalName().equals(de.key.valueClass))
				.map(de -> new ServerWithDataElement(topoReader.read(new String(de.payload)), de))
				.collect(Collectors.toMap(sde -> sde.server().uid, sde -> sde, (sde1, sde2) -> sde2));

		/*
		 * We need to build a list of server ordered, core first otherwise, we endup not
		 * pushing core IP in IptablesHook.
		 */
		List<ItemValue<Server>> allServersOrdered = aggregatedServers.values().stream().map(sde -> {
			ItemValue<Server> leader = topoReader.read(new String(sde.de().payload));
			ItemValue<Server> srv = sde.server();
			srv.value.ip = topologyMapping.ipAddressForUid(srv.uid, srv.value.ip);
			PromotingServer ps = new PromotingServer();
			ps.leader = leader;
			ps.clone = srv;
			touched.add(ps);
			return srv;
		}).sorted((a, b) -> Long.compare(a.value.tags.contains(TagDescriptor.bm_core.getTag()) ? -1 : a.internalId,
				b.value.tags.contains(TagDescriptor.bm_core.getTag()) ? -1 : b.internalId)).toList();

		allServersOrdered.forEach(srv -> {
			TaskRef srvTask;
			srv.value.ip = topologyMapping.ipAddressForUid(srv.uid, srv.value.ip);
			ItemValue<Server> exist = topoApi.getComplete(srv.uid);
			if (exist != null) {
				logger.info("UPDATE SRV {}", srv);
				try {
					srvTask = topoApi.update(srv.uid, srv.value);
					if (srv.value.tags.contains(TagDescriptor.bm_es.getTag())
							&& !exist.value.tags.contains(TagDescriptor.bm_es.getTag())) {
						shouldResetElasticsearch.set(true);
					}
				} catch (ServerFault sf) {
					srvTask = null;
					logger.warn("ServerUpdate is not possible: {}", sf.getMessage());
				}
			} else {
				logger.info("CREATE SRV {}", srv);
				srvTask = topoApi.create(srv.uid, srv.value);
				if (srv.value.tags.contains(TagDescriptor.bm_es.getTag())) {
					shouldResetElasticsearch.set(true);
				}
			}
			LogStatus logStatus = logStreamWait(srvTask);
			monitor.log(srv.uid + ": " + logStatus.log);
			if (!logStatus.success()) {
				throw new ServerFault("Unable to create server " + srv + " task " + srvTask.id + " failed");
			}
		});
		if (shouldResetElasticsearch.get()) {
			monitor.log("Reset ES indexes...");
			do {
				Optional<ItemValue<Server>> exist = Topology.get().anyIfPresent(TagDescriptor.bm_es.getTag());
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
		target.instance(IInstallation.class).resetAuditLogClient();

		Map<String, PromotingServer> serverByUid = touched.stream()
				.collect(Collectors.toMap(ps -> ps.leader.uid, ps -> ps, (ps1, ps2) -> ps2));

		monitor.progress(1, "Dealt with topology");
		return serverByUid;
	}

	private static record LogStatus(String log, boolean success) {
	}

	public LogStatus logStreamWait(TaskRef ref) {
		if (ref == null) {
			return new LogStatus("[nothing to do]", true);
		}
		ITask taskApi = target.instance(ITask.class, ref.id + "");
		String logs = GenericStream.streamToString(taskApi.log());
		return new LogStatus(logs, taskApi.status().state.succeed);
	}

}
