package net.bluemind.exchange.mapi.notifications;

import java.util.List;

import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;

public class FlatHierarchyAclHook implements IAclHook {

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {

		IInternalContainersFlatHierarchy flatHierarchyApi = context.su().provider()
				.instance(IInternalContainersFlatHierarchy.class, container.domainUid, container.owner);
		flatHierarchyApi.list().stream() //
				.filter(nodeItem -> nodeItem.value.containerUid.startsWith("mapi:IPM_SUBTREE")) //
				.findFirst() //
				.ifPresent(nodeItem -> flatHierarchyApi.touch(nodeItem.uid));
	}

}
