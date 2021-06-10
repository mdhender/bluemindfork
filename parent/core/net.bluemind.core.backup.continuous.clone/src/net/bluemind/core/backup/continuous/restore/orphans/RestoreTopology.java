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
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
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
		IServer topoApi = target.instance(IServer.class, installationId);
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
			target.instance(IInstallation.class).resetIndexes();
		}
		monitor.progress(1, "Dealt with topology");
		List<ItemValue<Server>> exist = topoApi.allComplete();
		return Optional.ofNullable(exist).orElse(touched).stream()
				.collect(Collectors.toMap(iv -> iv.uid, iv -> iv, (iv1, iv2) -> iv2));
	}

	public String logStreamWait(TaskRef ref) {
		if (ref == null) {
			return "[nothing to do]";
		}
		ITask taskApi = target.instance(ITask.class, ref.id + "");
		return GenericStream.streamToString(taskApi.log());
	}

}
