package net.bluemind.device.userbook;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;

public class UserDeviceRepair implements ContainerRepairOp {

	private static final Logger logger = LoggerFactory.getLogger(UserDeviceRepair.class);
	private static final String TYPE = "device";

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

		verifyDeviceContainer(domainUid, entry.entryUid, report, monitor, () -> {
			monitor.log("Device container of user " + entry.entryUid + " is missing");
			logger.info("Device container  of user {} is missing", entry.entryUid);
		});

	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

		verifyDeviceContainer(domainUid, entry.entryUid, report, monitor, () -> {
			monitor.log("Repairing device container of user " + entry.entryUid);
			logger.info("Repairing device container of user {}", entry.entryUid);

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

	private void verifyDeviceContainer(String domainUid, String entryUid, DiagnosticReport report,
			IServerTaskMonitor monitor, Runnable maintenance) {

		String containerUid = TYPE + ":" + entryUid;
		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
