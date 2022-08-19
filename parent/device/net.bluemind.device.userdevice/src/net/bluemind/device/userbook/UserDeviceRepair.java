package net.bluemind.device.userbook;

import java.util.Arrays;

import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.container.repair.ContainerRepairUtil;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;

public class UserDeviceRepair implements ContainerRepairOp {

	private static final String TYPE = "device";

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDeviceContainer(domainUid, entry.entryUid, monitor, () -> {
		});

		String containerUid = TYPE + ":" + entry.entryUid;
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
		});

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

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

		String containerUid = TYPE + ":" + entry.entryUid;
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
			ContainerRepairUtil.setAsDefault(containerUid, context, monitor);
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
