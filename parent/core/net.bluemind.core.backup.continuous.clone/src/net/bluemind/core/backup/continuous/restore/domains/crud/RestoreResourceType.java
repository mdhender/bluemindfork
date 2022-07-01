package net.bluemind.core.backup.continuous.restore.domains.crud;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.CloneException;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

public class RestoreResourceType extends CrudRestore<ResourceTypeDescriptor> {
	private final ValueReader<VersionnedItem<ResourceTypeDescriptor>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<ResourceTypeDescriptor>>() {
			});

	private final IServiceProvider target;

	public RestoreResourceType(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return "resourceTypes";
	}

	@Override
	protected ValueReader<VersionnedItem<ResourceTypeDescriptor>> reader() {
		return reader;
	}

	@Override
	protected IResourceTypes api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IResourceTypes.class, domain.uid);
	}

	@Override
	protected void delete(IRestoreCrudSupport<ResourceTypeDescriptor> api, RecordKey key, String uid) {
		IResources resourceApi = target.instance(IResources.class, domain.uid);
		List<String> existingResourceUids = resourceApi.byType(key.owner);
		existingResourceUids.forEach(resourceUid -> deleteResource(resourceUid, key, resourceApi));
		super.delete(api, key, uid);
	}

	private void deleteResource(String uid, RecordKey key, IResources resourceApi) {
		log.deleteChild(type(), key, uid);
		TaskRef taskRef = resourceApi.delete(uid);
		TaskStatus status = TaskUtils.wait(target, taskRef, log -> {
		});
		if (status.state.equals(TaskStatus.State.InError)) {
			String message = String.format("Failed to delete resource, uid:%s", uid);
			throw new CloneException(message);
		}
	}

}
