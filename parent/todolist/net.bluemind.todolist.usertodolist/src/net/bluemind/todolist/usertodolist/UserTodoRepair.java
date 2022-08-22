package net.bluemind.todolist.usertodolist;

import java.util.Arrays;

import net.bluemind.core.container.api.ContainerSubscription;
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
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUserSubscription;

public class UserTodoRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyTodoContainer(domainUid, entry.entryUid, monitor, () -> {
		});

		String containerUid = getUserTodoListId(entry.entryUid);
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
		});

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
		}, containerUid);

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyTodoContainer(domainUid, entry.entryUid, monitor, () -> {

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

		String containerUid = getUserTodoListId(entry.entryUid);
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
			ContainerRepairUtil.setAsDefault(containerUid, context, monitor);
		});

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
			ContainerRepairUtil.subscribe(entry.entryUid, domainUid, container);
		}, containerUid);

	}

	private void verifyTodoContainer(String domainUid, String entryUid, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = getUserTodoListId(entryUid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	public static String getUserTodoListId(String userUid) {
		return ITodoUids.defaultUserTodoList(userUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
