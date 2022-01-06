package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.container.api.IInternalContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreContainerMetadata implements RestoreDomainType {

	private static final ValueReader<ItemValue<ContainerMetadata>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<ContainerMetadata>>() {
			});
	private final IServerTaskMonitor monitor;
	private final IServiceProvider target;

	public RestoreContainerMetadata(IServerTaskMonitor monitor, IServiceProvider target) {
		this.monitor = monitor;
		this.target = target;
	}

	@Override
	public String type() {
		return "containers_meta";
	}

	@Override
	public void restore(DataElement de) {
		ItemValue<ContainerMetadata> item = mrReader.read(new String(de.payload));
		ContainerMetadata metadata = item.value;
		IInternalContainerManagement mgmtApi = target.instance(IInternalContainerManagement.class,
				metadata.containerUid);
		switch (metadata.type) {
		case ACL:
			mgmtApi.setAccessControlList(metadata.acls, false);
			break;
		case SETTING:
			mgmtApi.setSettings(metadata.settings);
			break;
		}
		monitor.log("Container metadata '" + metadata.type + "' for '" + item.value.containerUid + "'");
	}
}