package net.bluemind.tag.hooks;

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
import net.bluemind.tag.api.ITagUids;
import net.bluemind.user.api.IUser;

public class UserTagRepair implements ContainerRepairOp {

	private static final Logger logger = LoggerFactory.getLogger(UserTagRepair.class);

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

		verifyTagsContainer(domainUid, entry.entryUid, report, monitor, () -> {
			monitor.log("Tag container of user " + entry.entryUid + " is missing");
			logger.info("Tag container  of user {} is missing", entry.entryUid);
		});

	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

		verifyTagsContainer(domainUid, entry.entryUid, report, monitor, () -> {
			monitor.log("Repairing tag container of user " + entry.entryUid);
			logger.info("Repairing tag container of user {}", entry.entryUid);

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

	private void verifyTagsContainer(String domainUid, String entryUid, DiagnosticReport report,
			IServerTaskMonitor monitor, Runnable maintenance) {

		String containerUid = getTagsContainerUid(entryUid);
		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	private String getTagsContainerUid(String userUid) {
		return ITagUids.TYPE + "_" + userUid;
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
