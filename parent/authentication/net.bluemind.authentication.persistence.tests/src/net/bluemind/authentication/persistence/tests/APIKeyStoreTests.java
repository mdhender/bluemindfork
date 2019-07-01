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
package net.bluemind.authentication.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.authentication.api.APIKey;
import net.bluemind.authentication.persistence.APIKeyStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class APIKeyStoreTests {

	private APIKeyStore store1;
	private APIKeyStore store2;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		SecurityContext context1 = new SecurityContext(null, "subject", Arrays.<String> asList(),
				Arrays.<String> asList(), null);

		store1 = new APIKeyStore(JdbcTestHelper.getInstance().getDataSource(), context1);

		SecurityContext context2 = new SecurityContext(null, "another-subject", Arrays.<String> asList(),
				Arrays.<String> asList(), null);

		store2 = new APIKeyStore(JdbcTestHelper.getInstance().getDataSource(), context2);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void createAndGet() throws ServerFault {

		APIKey apikey = apikey();

		store1.create(apikey);

		APIKey fetched = store1.get(apikey.sid);
		assertNotNull(fetched);
		assertEquals(apikey.sid, fetched.sid);
		assertEquals(apikey.displayName, fetched.displayName);

		fetched = store1.get("random");
		assertNull(fetched);

		fetched = store2.get(apikey.sid);
		assertNull(fetched);

	}

	@Test
	public void listApiKeys() throws ServerFault {
		List<APIKey> list = store1.list();
		assertEquals(0, list.size());

		APIKey apikey = apikey();

		store1.create(apikey);
		list = store1.list();
		assertEquals(1, list.size());

		APIKey fetched = list.get(0);
		assertNotNull(fetched);
		assertEquals(apikey.sid, fetched.sid);
		assertEquals(apikey.displayName, fetched.displayName);

		List<APIKey> list2 = store2.list();
		assertEquals(0, list2.size());
	}

	@Test
	public void createAndDelete() throws ServerFault {
		APIKey apikey = apikey();

		store1.create(apikey);

		APIKey fetched = store1.get(apikey.sid);
		assertNotNull(fetched);

		store2.delete(apikey.sid);
		fetched = store1.get(apikey.sid);
		assertNotNull(fetched);

		store1.delete(apikey.sid);
		fetched = store1.get(apikey.sid);
		assertNull(fetched);
	}

	@Test
	public void deleteAll() throws ServerFault {
		APIKey apikey = apikey();
		store1.create(apikey);

		APIKey apikey2 = apikey();
		store1.create(apikey2);

		List<APIKey> list = store1.list();
		assertEquals(2, list.size());

		store2.deleteAll();
		list = store1.list();
		assertEquals(2, list.size());

		store1.deleteAll();
		list = store1.list();
		assertEquals(0, list.size());
	}

	private APIKey apikey() {
		APIKey apikey = new APIKey();
		apikey.displayName = "key-" + System.currentTimeMillis();
		apikey.sid = UUID.randomUUID().toString();
		return apikey;
	}

}
