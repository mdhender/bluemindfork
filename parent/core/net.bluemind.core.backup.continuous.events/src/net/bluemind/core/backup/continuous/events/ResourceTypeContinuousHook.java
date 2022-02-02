package net.bluemind.core.backup.continuous.events;

import net.bluemind.core.rest.BmContext;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.hook.IResourceTypeHook;

public class ResourceTypeContinuousHook
		implements IResourceTypeHook, ContinuousContenairization<ResourceTypeDescriptor> {

	@Override
	public String type() {
		return "resourceTypes";
	}

	@Override
	public void onCreate(BmContext context, String domainUid, String uid, ResourceTypeDescriptor resourceType) {
		save(domainUid, domainUid, uid, resourceType, true);
	}

	@Override
	public void onUpdate(BmContext context, String domainUid, String uid, ResourceTypeDescriptor resourceType) {
		save(domainUid, domainUid, uid, resourceType, false);
	}

	@Override
	public void onDelete(BmContext context, String domainUid, String uid, ResourceTypeDescriptor previousResourceType) {
		delete(domainUid, domainUid, uid, previousResourceType);
	}
}
