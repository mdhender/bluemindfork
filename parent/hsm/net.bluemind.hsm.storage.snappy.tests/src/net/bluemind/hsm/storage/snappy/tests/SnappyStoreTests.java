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
package net.bluemind.hsm.storage.snappy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.hsm.storage.impl.SnappyStore;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.utils.FileUtils;

public class SnappyStoreTests {

	private SnappyStore store;

	@Before
	public void before() throws ServerFault {
		String nodeIp = new BmConfIni().get("bluemind/node-tests");
		store = new SnappyStore();
		store.open(NodeActivator.get(nodeIp));
	}

	@Test
	public void snappyTest() throws IOException {
		InputStream mailContent = getClass().getClassLoader().getResourceAsStream("data/test.eml");

		String mboxUid = UUID.randomUUID().toString();

		String hsmId = store.store("test.lan", mboxUid, mailContent);
		assertNotNull(hsmId);

		InputStream stored = store.peek("test.lan", mboxUid, hsmId, Integer.MAX_VALUE);
		assertNotNull(stored);

		String storedContent = FileUtils.streamString(stored, true);

		String expectedContent = FileUtils
				.streamString(getClass().getClassLoader().getResourceAsStream("data/test.eml"), true);

		assertEquals(expectedContent, storedContent);
	}
}
