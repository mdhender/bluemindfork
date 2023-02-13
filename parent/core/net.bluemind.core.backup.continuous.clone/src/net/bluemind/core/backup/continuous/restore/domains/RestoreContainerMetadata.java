package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IInternalContainerManagement;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreContainerMetadata implements RestoreDomainType {

	private static final ValueReader<ItemValue<ContainerMetadata>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<ContainerMetadata>>() {
			});
	private final RestoreLogger log;
	private final IServiceProvider target;

	public RestoreContainerMetadata(RestoreLogger log, IServiceProvider target) {
		this.log = log;
		this.target = target;
	}

	@Override
	public String type() {
		return "containers_meta";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		ItemValue<ContainerMetadata> item = mrReader.read(payload);
		ContainerMetadata metadata = item.value;

		IContainers contApi = target.instance(IContainers.class);
		Optional<ContainerDescriptor> maybeHere = Optional.ofNullable(contApi.getIfPresent(metadata.contDesc.uid));
		if (!maybeHere.isPresent()) {
			BaseContainerDescriptor cd = metadata.contDesc;
			ContainerDescriptor fullCd = ContainerDescriptor.create(cd.uid, cd.name, cd.owner, cd.type, cd.domainUid,
					cd.defaultContainer, metadata.settings);
			contApi.create(cd.uid, fullCd);
			log.create(cd.type, key);
		}

		IInternalContainerManagement mgmtApi = target.instance(IInternalContainerManagement.class,
				metadata.contDesc.uid);
		log.set(type(), metadata.type.name(), key);
		switch (metadata.type) {
		case ACL:
			mgmtApi.setAccessControlList(metadata.acls, false);
			break;
		case SETTING:
			mgmtApi.setSettings(metadata.settings);
			break;
		}
	}
}