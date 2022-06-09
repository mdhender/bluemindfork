package net.bluemind.webappdata.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.webappdata.api.IWebAppDataUids;

public class WebAppDataRepair implements ContainerRepairOp {

	private static final Logger logger = LoggerFactory.getLogger(WebAppDataRepair.class);

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = IWebAppDataUids.containerUid(userUid);
		Runnable maintenance = () -> {
			String logMessage = "WebAppData container of user {} is missing";
			monitor.log(logMessage.replace("{}", userUid));
			logger.info(logMessage, userUid);
		};
		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = IWebAppDataUids.containerUid(userUid);
		Runnable maintenance = () -> {
			String logMessage = "Repairing WebAppData container of user {}";
			monitor.log(logMessage.replace("{}", userUid));
			logger.info(logMessage, userUid);

			ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid, containerUid, userUid,
					IWebAppDataUids.TYPE, domainUid, true);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
					.create(containerUid, descriptor);
		};

		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
