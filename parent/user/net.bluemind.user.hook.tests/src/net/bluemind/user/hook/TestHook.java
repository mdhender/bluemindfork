package net.bluemind.user.hook;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;

public class TestHook extends DefaultUserHook {

	public static final CountDownLatch latch = new CountDownLatch(3);

	public TestHook() {
		System.out.println("Test hook created");
	}

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) {
		System.out.println("Created " + created.value.login);

		assertNotNull(context);
		assertNotNull(domainUid);
		assertNotNull(created);
		assertNotNull(created.value);
		latch.countDown();
	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> prev, ItemValue<User> current) {
		System.out.println("Updated " + prev.value.login);

		assertNotNull(prev);
		assertNotNull(domainUid);
		assertNotNull(prev);
		assertNotNull(prev.value);

		assertNotNull(current);
		assertNotNull(current.value);
		latch.countDown();
	}

	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) {
		System.out.println("Deleted " + deleted.value.login);

		assertNotNull(deleted);
		assertNotNull(domainUid);
		assertNotNull(deleted.value);
		latch.countDown();
	}

	@Override
	public boolean handleGlobalVirt() {
		return false;
	}

}
