package net.bluemind.core.backup.continuous.restore.orphans;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

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
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.IInstallation;

public class RestoreTopology {

	private static final Logger logger = LoggerFactory.getLogger(RestoreTopology.class);

	private final String installationId;
	private final IServiceProvider target;
	private final TopologyMapping topologyMapping;

	public RestoreTopology(String installationId, IServiceProvider target, TopologyMapping topologyMapping) {
		this.installationId = installationId;
		this.target = target;
		this.topologyMapping = topologyMapping;
	}

	public Map<String, ItemValue<Server>> restore(IServerTaskMonitor monitor, List<DataElement> servers) {
		ValueReader<ItemValue<Server>> topoReader = JsonUtils.reader(new TypeReference<ItemValue<Server>>() {
		});
		logger.info("Get IServer API for installation {}", installationId);
		IServer topoApi = target.instance(IServer.class, "default");
		AtomicBoolean resetES = new AtomicBoolean();
		List<ItemValue<Server>> touched = new LinkedList<>();
		servers.forEach(srvDE -> {
			ItemValue<Server> srv = topoReader.read(new String(srvDE.payload));
			TaskRef srvTask;
			srv.value.ip = topologyMapping.ipAddressForUid(srv.uid, srv.value.ip);
			ItemValue<Server> exist = topoApi.getComplete(srv.uid);
			if (exist != null) {
				logger.info("UPDATE SRV {}", srv);
				srvTask = topoApi.update(srv.uid, srv.value);
				if (srv.value.tags.contains("bm/es") && exist != null && !exist.value.tags.contains("bm/es")) {
					resetES.set(true);
				}
			} else {
				logger.info("CREATE SRV {}", srv);
				srvTask = topoApi.create(srv.uid, srv.value);
				if (srv.value.tags.contains("bm/es")) {
					resetES.set(true);
				}
			}
			touched.add(srv);
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
		List<ItemValue<Server>> exist = topoApi.allComplete();

		Map<String, ItemValue<Server>> serverByUid = Optional.ofNullable(exist).orElse(touched).stream()
				.collect(Collectors.toMap(iv -> iv.uid, iv -> iv, (iv1, iv2) -> iv2));

		bumpAllContainerItemId(monitor, serverByUid.values());
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

	private void bumpAllContainerItemId(IServerTaskMonitor monitor, Collection<ItemValue<Server>> serverItems) {
		long seq = 1000000;
		monitor.log("Bumping container item id seq to for " + serverItems.size() + " server(s)");
		serverItems.stream().map(serverItem -> serverItem.value)
				.filter(server -> server.tags.contains(TagDescriptor.bm_pgsql.getTag())
						|| server.tags.contains(TagDescriptor.bm_pgsql_data.getTag()))
				.forEach(server -> {
					bumpContainerItemId(monitor, server, seq);
				});
	}

	private void bumpContainerItemId(IServerTaskMonitor monitor, Server server, long seq) {
		INodeClient nodeClient = NodeActivator.get(server.ip);
		List<String> dbNames = serverDbNames(server);
		dbNames.forEach(dbName -> {
			monitor.log("Bumping container item id seq to " + seq + " (server " + server.ip + " db " + dbName + ")");
			String bumpCmd = String.format(
					"PGPASSWORD=%s psql -h localhost -c \"select setval('t_container_item_id_seq', %d)\" %s %s", "bj",
					seq, dbName, "bj");
			String bumpCmdPath = "/tmp/dump-container-item-id-seq-" + System.nanoTime() + ".sh";
			nodeClient.writeFile(bumpCmdPath, new ByteArrayInputStream(bumpCmd.getBytes()));
			try {
				NCUtils.execNoOut(nodeClient, "chmod +x " + bumpCmdPath);
				ExitList el = NCUtils.exec(nodeClient, bumpCmdPath);
				for (String log : el) {
					if (!log.isEmpty()) {
						monitor.log(log);
					}
				}
			} finally {
				NCUtils.execNoOut(nodeClient, "rm -f " + bumpCmdPath);
			}
		});
	}

	private List<String> serverDbNames(Server server) {
		List<String> dbNames = new ArrayList<>();
		if (server.tags.contains(TagDescriptor.bm_pgsql.getTag())) {
			dbNames.add("bj");
		}
		if (server.tags.contains(TagDescriptor.bm_pgsql_data.getTag())) {
			dbNames.add("bj-data");
		}
		return dbNames;
	}

}
