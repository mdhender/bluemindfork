/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.sync.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.sync.SyncableContainer;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.tests.BmTestContext;

public class SyncableContainerTests {

	@Test
	public void sync() throws ServerFault {
		BmTestContext ctx = new BmTestContext(
				new SecurityContext("test", "test", Arrays.<String> asList(), Arrays.<String> asList(), null));

		Container c = new Container();
		c.type = "dummy";

		SyncableContainer sc = new SyncableContainer(ctx);
		Map<String, String> synctTokens = new HashMap<>();
		synctTokens.put("Tata", "Suzanne");
		ContainerSyncResult ret = sc.sync(c, synctTokens, new NullTaskMonitor());
		assertEquals(42L, ret.status.nextSync.longValue());
		assertEquals("Suzanne", ret.status.syncTokens.get("Tata"));
		assertEquals(3, ret.added);
		assertEquals(4, ret.removed);
		assertEquals(5, ret.updated);

	}

}
