package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.IDtoPreProcessor;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IInternalContainerManagement;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreContainerMetadata implements RestoreDomainType {

	private static final ValueReader<VersionnedItem<ContainerMetadata>> mrReader = JsonUtils
			.reader(new TypeReference<VersionnedItem<ContainerMetadata>>() {
			});
	private final RestoreLogger log;
	private final IServiceProvider target;
	private final List<IDtoPreProcessor<ContainerMetadata>> preProcs;
	private final RestoreState state;

	public RestoreContainerMetadata(RestoreLogger log, IServiceProvider target, RestoreState state) {
		this.log = log;
		this.target = target;
		this.state = state;
		this.preProcs = Arrays.asList(new ContainerMetadataUidFixup(state));
	}

	@Override
	public String type() {
		return "containers_meta";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		IContainers contApi = target.instance(IContainers.class);

		if (Operation.isDelete(key)) {
			String cid = new JsonObject(payload).getString("uid");
			cid = state.uidAlias(cid);
			BaseContainerDescriptor exist = contApi.getLightIfPresent(cid);
			if (exist != null) {
				try {
					log.delete(type(), key);
					contApi.delete(cid);
				} catch (Exception e) {
					log.monitor().warn("Failed to delete {}", e.getMessage());
				}
			}
			return;
		}

		VersionnedItem<ContainerMetadata> item = mrReader.read(payload);

		for (IDtoPreProcessor<ContainerMetadata> preProc : preProcs) {
			item = preProc.fixup(log, target, key, item);
		}

		ContainerMetadata metadata = item.value;

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
		log.set(type(), key);
		if (metadata.acls != null) {
			mgmtApi.setAccessControlList(metadata.acls, false);
		}
		if (metadata.settings != null) {
			mgmtApi.setSettings(metadata.settings);
		}
	}
}