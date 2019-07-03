package net.bluemind.server.hook;

import java.util.concurrent.CountDownLatch;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.server.api.Server;

public class TestHook implements IServerHook {

	public static final CountDownLatch latch = new CountDownLatch(9);

	public TestHook() {
		System.out.println("Test hook created");
	}

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> item) throws ServerFault {
		System.out.println("Created " + item.value.fqdn);
		latch.countDown();
	}

	@Override
	public void onServerUpdated(BmContext context, ItemValue<Server> previousValue, Server value) throws ServerFault {
		System.out.println("Updated " + previousValue.value.fqdn);
		latch.countDown();

	}

	@Override
	public void onServerDeleted(BmContext context, ItemValue<Server> itemValue) throws ServerFault {
		System.out.println("Deleted " + itemValue.value.fqdn);
		latch.countDown();

	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		System.out.println("Tagged " + itemValue.value.fqdn);
		latch.countDown();
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		System.out.println("Untagged " + itemValue.value.fqdn);
		latch.countDown();

	}

	@Override
	public void onServerAssigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain, String tag)
			throws ServerFault {
		System.out.println("Assign " + itemValue.uid + " to " + domain);
		// TODO Auto-generated method stub

	}

	@Override
	public void onServerUnassigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain, String tag)
			throws ServerFault {
		// nullpointer when itemValue is null
		System.out.println("Unassign " + itemValue.uid + " to " + domain);

	}

	@Override
	public void beforeCreate(BmContext context, String uid, Server server) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(BmContext context, String uid, Server server, Server previous) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void onServerPreUnassigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain,
			String tag) throws ServerFault {
		// TODO Auto-generated method stub

	}

}
