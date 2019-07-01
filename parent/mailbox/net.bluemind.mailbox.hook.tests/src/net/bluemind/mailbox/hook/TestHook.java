package net.bluemind.mailbox.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;

public class TestHook implements IMailboxHook {

	public static final CountDownLatch latch = new CountDownLatch(4);

	public TestHook() {
		System.out.println("Test hook created");
	}

	@Override
	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> value)
			throws ServerFault {
		System.out.println("Created " + value.value.name);

		assertNotNull(context);
		assertNotNull(domainUid);

		assertFalse(domainUid.isEmpty());
		assertNotNull(value.uid);
		assertNotNull(value.value);
		latch.countDown();
	}

	@Override
	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previousValue,
			ItemValue<Mailbox> value) throws ServerFault {
		System.out.println("Created " + previousValue.value.name);

		assertNotNull(context);
		assertNotNull(domainUid);

		assertNotNull(previousValue.uid);
		assertNotNull(previousValue.value);

		assertNotNull(value.uid);
		assertNotNull(value.value);
		latch.countDown();
	}

	@Override
	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value)
			throws ServerFault {
		System.out.println("Deleted " + value.value.name);

		assertNotNull(context);
		assertNotNull(domainUid);

		assertNotNull(value.uid);
		assertNotNull(value.value);
		latch.countDown();
	}

	@Override
	public void onMailFilterChanged(BmContext context, String domainUid, ItemValue<Mailbox> value, MailFilter filter)
			throws ServerFault {
	}

	@Override
	public void onDomainMailFilterChanged(BmContext context, String domainUid, MailFilter filter)
			throws ServerFault {
		System.out.println("On domain mail filter changed: " + domainUid);

		assertNotNull(context);
		assertNotNull(domainUid);
		assertNotNull(filter);
		latch.countDown();
	}
}
