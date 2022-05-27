package net.bluemind.tag.hooks;

import java.util.Arrays;

import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.user.api.IUser;

public class UserTagRepair implements ContainerRepairOp {

	@Override
	public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyTagsContainer(domainUid, entry.entryUid, monitor, () -> {
		});

	}

	@Override
	public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyTagsContainer(domainUid, entry.entryUid, monitor, () -> {

			String displayName = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IUser.class, domainUid).getComplete(entry.entryUid).displayName;

			IContainers containers = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);

			ContainerDescriptor descriptor = new ContainerDescriptor();
			String containerUid = getTagsContainerUid(entry.entryUid);
			descriptor.uid = containerUid;
			descriptor.name = "tags of user " + displayName;
			descriptor.type = ITagUids.TYPE;
			descriptor.owner = entry.entryUid;
			descriptor.domainUid = domainUid;
			containers.create(containerUid, descriptor);

			IContainerManagement cm = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainerManagement.class, containerUid);

			cm.setAccessControlList(Arrays.asList(AccessControlEntry.create(entry.entryUid, Verb.All)));

		});

	}

	private void verifyTagsContainer(String domainUid, String entryUid, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = getTagsContainerUid(entryUid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	private String getTagsContainerUid(String userUid) {
		return ITagUids.TYPE + "_" + userUid;
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
