package net.bluemind.core.backup.continuous.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.hook.IResourceTypeHook;

public class ResourceTypeContinuousHook implements IResourceTypeHook {

	private static final Logger logger = LoggerFactory.getLogger(ResourceTypeContinuousHook.class);

	@Override
	public void onCreate(BmContext context, String domainUid, String uid, ResourceTypeDescriptor resourceType) {
		save(context, domainUid, uid, resourceType);
	}

	@Override
	public void onUpdate(BmContext context, String domainUid, String uid, ResourceTypeDescriptor resourceType) {
		save(context, domainUid, uid, resourceType);
	}

	private void save(BmContext context, String domainUid, String uid, ResourceTypeDescriptor resourceType) {
		ContainerDescriptor metaDesc = descriptor(domainUid, uid);
		ItemValue<ResourceTypeDescriptor> iv = itemValue(uid, resourceType);
		DefaultBackupStore.store().<ResourceTypeDescriptor>forContainer(metaDesc).store(iv);
		logger.info("Saved resourceType for {}:{}", domainUid, uid);
	}

	@Override
	public void onDelete(BmContext context, String domainUid, String uid, ResourceTypeDescriptor previousResourceType) {
		ContainerDescriptor metaDesc = descriptor(domainUid, uid);
		ItemValue<ResourceTypeDescriptor> iv = itemValue(uid, previousResourceType);
		DefaultBackupStore.store().<ResourceTypeDescriptor>forContainer(metaDesc).delete(iv);
		logger.info("Deleted resourceType for {}:{}", domainUid, uid);
	}

	private ContainerDescriptor descriptor(String domainUid, String uid) {
		return ContainerDescriptor.create(uid + "_at_" + domainUid + "_resource_type", uid + " resource type", uid,
				"resourceTypes", domainUid, true);
	}

	private ItemValue<ResourceTypeDescriptor> itemValue(String uid, ResourceTypeDescriptor descriptor) {
		ItemValue<ResourceTypeDescriptor> iv = ItemValue.create(uid, descriptor);
		iv.internalId = iv.uid.hashCode();
		return iv;
	}
}
