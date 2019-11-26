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
package net.bluemind.im.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.im.persistence.IMStore;

public class IMStoreTests {

	private IMStore store;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		store = new IMStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testRoster() throws Exception {
		String jabberId = "jabberId";
		String data = "data";

		String roster = store.getRoster(jabberId);
		assertNull(roster);

		store.setRoster(jabberId, data);

		roster = store.getRoster(jabberId);
		assertEquals(data, roster);

		data = "updated roster";
		store.setRoster(jabberId, data);

		roster = store.getRoster(jabberId);
		assertEquals(data, roster);
	}

}
