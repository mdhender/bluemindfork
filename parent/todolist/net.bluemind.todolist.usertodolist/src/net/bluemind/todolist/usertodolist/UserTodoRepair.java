package net.bluemind.todolist.usertodolist;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.ContainerSubscription;
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
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUserSubscription;

public class UserTodoRepair implements ContainerRepairOp {

	private static final Logger logger = LoggerFactory.getLogger(UserTodoRepair.class);

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

		verifyDeviceContainer(domainUid, entry.entryUid, report, monitor, () -> {
			monitor.log("Todo container of user " + entry.entryUid + " is missing");
			logger.info("Todo container  of user {} is missing", entry.entryUid);
		});

	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

		verifyDeviceContainer(domainUid, entry.entryUid, report, monitor, () -> {
			monitor.log("Repairing todo container of user " + entry.entryUid);
			logger.info("Repairing todo container of user {}", entry.entryUid);

			String uid = getUserTodoListId(entry.entryUid);
			ContainerDescriptor todoList = ContainerDescriptor.create(uid, "$$mytasks$$", entry.entryUid,
					ITodoUids.TYPE, domainUid, true);

			IContainers containers = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);

			containers.create(uid, todoList);

			IContainerManagement manager = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainerManagement.class, uid);
			manager.setAccessControlList(Arrays.asList(AccessControlEntry.create(entry.entryUid, Verb.Write)));

			IUserSubscription userSubService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IUserSubscription.class, domainUid);
			userSubService.subscribe(entry.entryUid, Arrays.asList(ContainerSubscription.create(uid, true)));

		});

	}

	private void verifyDeviceContainer(String domainUid, String entryUid, DiagnosticReport report,
			IServerTaskMonitor monitor, Runnable maintenance) {

		String containerUid = getUserTodoListId(entryUid);
		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	public static String getUserTodoListId(String userUid) {
		return ITodoUids.defaultUserTodoList(userUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
