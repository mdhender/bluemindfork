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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.authentication.api.RefreshToken;
import net.bluemind.authentication.persistence.UserRefreshTokenStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class UserRefreshTokenStoreTests {

	private UserRefreshTokenStore store1;
	private UserRefreshTokenStore store2;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext context1 = new SecurityContext(null, "subject", Arrays.<String>asList(),
				Arrays.<String>asList(), "bm.loc");

		store1 = new UserRefreshTokenStore(JdbcTestHelper.getInstance().getDataSource(), context1.getSubject());

		SecurityContext context2 = new SecurityContext(null, "another-subject", Arrays.<String>asList(),
				Arrays.<String>asList(), "bm.loc");

		store2 = new UserRefreshTokenStore(JdbcTestHelper.getInstance().getDataSource(), context2.getSubject());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void createAndGet() throws ServerFault {

		RefreshToken token = token("system1");
		store1.add(token);

		RefreshToken fetched = store1.get(token.systemIdentifier);
		assertNotNull(fetched);
		assertEquals(token.systemIdentifier, fetched.systemIdentifier);
		assertEquals(token.token, fetched.token);
		assertEquals(token.expiryTime, fetched.expiryTime);

		fetched = store1.get("random");
		assertNull(fetched);

		// !sic, keys are unique
		fetched = store2.get(token.systemIdentifier);
		assertNull(fetched);
	}

	@Test
	public void testExpirationDeletesEntry() throws ServerFault {
		RefreshToken token = token("system1");
		long d = LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC).toEpochMilli();
		token.expiryTime = new Date(d);
		store1.add(token);

		RefreshToken fetched = store1.get(token.systemIdentifier);
		assertNull(fetched);
	}

	@Test
	public void delete() throws ServerFault {
		RefreshToken token = token("system1");
		store1.add(token);
		store2.add(token);

		RefreshToken token2 = token("system2");
		store1.add(token2);
		store2.add(token2);

		assertNotNull(store1.get("system1"));
		assertNotNull(store1.get("system2"));
		assertNotNull(store2.get("system1"));
		assertNotNull(store2.get("system2"));

		store1.delete("system2");

		assertNotNull(store1.get("system1"));
		assertNull(store1.get("system2"));
		assertNotNull(store2.get("system1"));
		assertNotNull(store2.get("system2"));

		store2.delete("system1");

		assertNotNull(store1.get("system1"));
		assertNull(store1.get("system2"));
		assertNull(store2.get("system1"));
		assertNotNull(store2.get("system2"));
	}

	@Test
	public void deleteAll() throws ServerFault {
		RefreshToken token = token("system1");
		store1.add(token);
		store2.add(token);

		RefreshToken token2 = token("system2");
		store1.add(token2);
		store2.add(token2);

		assertNotNull(store1.get("system1"));
		assertNotNull(store1.get("system2"));
		assertNotNull(store2.get("system1"));
		assertNotNull(store2.get("system2"));

		store1.deleteAll();

		assertNull(store1.get("system1"));
		assertNull(store1.get("system2"));
		assertNotNull(store2.get("system1"));
		assertNotNull(store2.get("system2"));

		store2.deleteAll();

		assertNull(store1.get("system1"));
		assertNull(store1.get("system2"));
		assertNull(store2.get("system1"));
		assertNull(store2.get("system2"));
	}

	private RefreshToken token(String systemIdentifier) {
		RefreshToken token = new RefreshToken();
		token.systemIdentifier = systemIdentifier;
		token.token = UUID.randomUUID().toString();
		token.expiryTime = new Date(2040, 5, 5);
		return token;
	}

}
