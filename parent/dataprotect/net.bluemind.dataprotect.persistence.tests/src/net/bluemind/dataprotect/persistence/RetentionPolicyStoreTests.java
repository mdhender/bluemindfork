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
package net.bluemind.dataprotect.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.dataprotect.api.RetentionPolicy;

public class RetentionPolicyStoreTests {
	private RetentionPolicyStore rpStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		rpStore = new RetentionPolicyStore(JdbcTestHelper.getInstance().getDataSource());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGetAndUpdate() throws Exception {
		RetentionPolicy rp = new RetentionPolicy();
		rp.daily = 1;
		rp.weekly = 2;
		rp.monthly = 3;
		rpStore.update(rp);
		rp = rpStore.get();
		assertNotNull(rp);
		assertEquals((Integer) 1, rp.daily);
		assertEquals((Integer) 2, rp.weekly);
		assertEquals((Integer) 3, rp.monthly);

		rp = new RetentionPolicy();
		rp.daily = null;
		rp.weekly = null;
		rp.monthly = null;
		rpStore.update(rp);
		rp = rpStore.get();
		assertNotNull(rp);
		// 1 is the default if no value is set
		assertEquals((Integer) 1, rp.daily);
		assertEquals((Integer) 0, rp.weekly);
		assertEquals((Integer) 0, rp.monthly);
	}
}
