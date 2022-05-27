package net.bluemind.webappdata.service.internal;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.webappdata.api.IWebAppDataUids;

public class WebAppDataRepair implements ContainerRepairOp {

	@Override
	public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = IWebAppDataUids.containerUid(userUid);
		Runnable maintenance = () -> {
		};
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	@Override
	public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = IWebAppDataUids.containerUid(userUid);
		Runnable maintenance = () -> {

			ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid, containerUid, userUid,
					IWebAppDataUids.TYPE, domainUid, true);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
					.create(containerUid, descriptor);
		};

		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
