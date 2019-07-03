package net.bluemind.group.hook;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import net.bluemind.core.api.fault.ServerFault;

public class TestHook implements IGroupHook {

	public static final CountDownLatch latch = new CountDownLatch(5);

	public TestHook() {
		System.out.println("Test hook created");
	}

	@Override
	public void onGroupCreated(GroupMessage created) {
		System.out.println("Created " + created.group.value.name);

		assertNotNull(created.container);
		assertNotNull(created.group);
		assertNotNull(created.group.value);
		latch.countDown();
	}

	@Override
	public void onGroupUpdated(GroupMessage previous, GroupMessage current) throws ServerFault {
		System.out.println("Updated " + previous.group.value.name);

		assertNotNull(previous.container);
		assertNotNull(previous.group);
		assertNotNull(previous.group.value);

		assertNotNull(current.container);
		assertNotNull(current.group);
		assertNotNull(current.group.value);
		latch.countDown();
	}

	@Override
	public void onGroupDeleted(GroupMessage previous) throws ServerFault {
		System.out.println("Deleted " + previous.group.value.name);

		assertNotNull(previous.container);
		assertNotNull(previous.group);
		assertNotNull(previous.group.value);
		latch.countDown();
	}

	@Override
	public void onAddMembers(GroupMessage group) {
		System.out.println("Add member " + group.group.value.name);

		assertNotNull(group.container);
		assertNotNull(group.group);
		assertNotNull(group.group.value);
		assertNotNull(group.members);
		latch.countDown();
	}

	@Override
	public void onRemoveMembers(GroupMessage group) {
		System.out.println("Remove member " + group.group.value.name);

		assertNotNull(group.container);
		assertNotNull(group.group);
		assertNotNull(group.group.value);
		assertNotNull(group.members);
		latch.countDown();
	}
}
