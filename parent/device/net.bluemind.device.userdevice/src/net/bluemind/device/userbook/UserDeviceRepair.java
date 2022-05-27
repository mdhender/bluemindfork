package net.bluemind.device.userbook;

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
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.directory.api.DirEntry;

public class UserDeviceRepair implements ContainerRepairOp {

	private static final String TYPE = "device";

	@Override
	public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDeviceContainer(domainUid, entry.entryUid, monitor, () -> {
		});

	}

	@Override
	public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDeviceContainer(domainUid, entry.entryUid, monitor, () -> {

			ContainerDescriptor descriptor = ContainerDescriptor.create(TYPE + ":" + entry.entryUid, TYPE,
					entry.entryUid, TYPE, domainUid, true);
			IContainers service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			service.create(descriptor.uid, descriptor);

			IContainerManagement cm = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainerManagement.class, descriptor.uid);
			cm.setAccessControlList(Arrays.asList(AccessControlEntry.create(entry.entryUid, Verb.All)));

		});

	}

	private void verifyDeviceContainer(String domainUid, String entryUid, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = TYPE + ":" + entryUid;
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
