package net.bluemind.core.container.persistence.tests;

import java.util.List;

import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;

public class TestHook implements IAclHook {

	public static int count;

	public TestHook() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		count++;
	}

}
