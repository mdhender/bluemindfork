package net.bluemind.core.backup.continuous.restore.domains;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

public class RestoreResourceType implements RestoreDomainType {

	private final ValueReader<ItemValue<ResourceTypeDescriptor>> typeReader = JsonUtils
			.reader(new TypeReference<ItemValue<ResourceTypeDescriptor>>() {
			});

	private final IServerTaskMonitor monitor;
	private ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreResourceType(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "resourceTypes";
	}

	@Override
	public void restore(DataElement de) {
		String payload = new String(de.payload);
		monitor.log("Processing resourceTypes:\n" + de.key + "\n" + payload);
		IResourceTypes resourceTypeApi = target.instance(IResourceTypes.class, domain.uid);
		if (de.payload.length > 0) {
			createOrUpdate(payload, resourceTypeApi);
		} else {
			delete(de.key, resourceTypeApi);
		}
	}

	private void createOrUpdate(String payload, IResourceTypes resourceTypeApi) {
		ItemValue<ResourceTypeDescriptor> resourceTypeItem = typeReader.read(payload);
		boolean exists = resourceTypeApi.get(resourceTypeItem.uid) != null;
		if (!exists) {
			resourceTypeApi.create(resourceTypeItem.uid, resourceTypeItem.value);
		} else {
			resourceTypeApi.update(resourceTypeItem.uid, resourceTypeItem.value);
		}
	}

	private void delete(RecordKey key, IResourceTypes resourceTypeApi) {
		try {
			IResources resourceApi = target.instance(IResources.class, domain.uid);
			List<String> existingResourceUids = resourceApi.byType(key.owner);
			existingResourceUids.forEach(uid -> deleteResource(uid, resourceApi));
			resourceTypeApi.delete(key.uid);
		} catch (Exception e) {
			monitor.log("Fails to delete resourceTypes: " + key);
		}
	}

	private void deleteResource(String uid, IResources resourceApi) {
		monitor.log("Deleting existing resource:" + uid + " before deleting resourceType");
		TaskRef taskRef = resourceApi.delete(uid);
		TaskStatus status = TaskUtils.wait(target, taskRef, log -> {
		});
		if (status.state.equals(TaskStatus.State.InError)) {
			monitor.log("Fail to delete resource:" + uid);
		}
	}
}
