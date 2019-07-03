/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.user.accounts.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IInternalUserExternalAccount;
import net.bluemind.user.api.IUserExternalAccount;
import net.bluemind.user.api.UserAccount;
import net.bluemind.user.api.UserAccountInfo;

public class UserAccountsServiceTests {

	private String domainUid;
	private String user1;
	private String user2;
	private String userNoRole;
	private SecurityContext userSecurityContext1;
	private SecurityContext userSecurityContext2;
	private SecurityContext userSecurityContextNoRole;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		domainUid = "dom" + System.currentTimeMillis() + ".test";

		PopulateHelper.initGlobalVirt();

		PopulateHelper.addDomain(domainUid);

		user1 = PopulateHelper.addUserWithRoles("user1", domainUid, BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT);
		user2 = PopulateHelper.addUserWithRoles("user2", domainUid, BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT);
		userNoRole = PopulateHelper.addUser("usernorole", domainUid);

		userSecurityContext1 = BmTestContext.contextWithSession("sid1" + System.currentTimeMillis(), "user1", domainUid,
				BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT).getSecurityContext();
		userSecurityContext2 = BmTestContext.contextWithSession("sid2" + System.currentTimeMillis(), "user2", domainUid,
				BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT).getSecurityContext();
		userSecurityContextNoRole = BmTestContext
				.contextWithSession("sid3" + System.currentTimeMillis(), "ususerNoRoleer2", domainUid)
				.getSecurityContext();

	}

	@Test
	public void testCreatingAUserAccount() {

		IUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IUserExternalAccount.class, domainUid, user1);
		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		accountService.create(externalSystemId, account);
		UserAccount userAccount = accountService.get(externalSystemId);

		assertEquals(account.login, userAccount.login);
		assertNull(userAccount.credentials);
		assertEquals(account.additionalSettings, userAccount.additionalSettings);

	}

	@Test
	public void testCreatingAUserAccountForOtherUsersShouldFail() {

		IUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IUserExternalAccount.class, domainUid, user1);
		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		accountService.create(externalSystemId, account);
		UserAccount userAccount = accountService.get(externalSystemId);

		assertEquals(account.login, userAccount.login);
		assertNull(userAccount.credentials);
		assertEquals(account.additionalSettings, userAccount.additionalSettings);

	}

	@Test
	public void testUpdatingAUserAccount() throws Exception {

		IUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IUserExternalAccount.class, domainUid, user1);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");
		accountService.create(externalSystemId, account);

		account.login = "myLogin-updated";
		account.credentials = "top-secret-updated";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1-updated", "value1-updated");
		account.additionalSettings.put("key2-updated", "value2-updated");
		accountService.update("xCloud", account);

		UserAccount userAccount = accountService.get(externalSystemId);

		assertEquals(account.login, userAccount.login);
		assertNull(userAccount.credentials);
		assertEquals(account.additionalSettings, userAccount.additionalSettings);
	}

	@Test
	public void testDeletingAUserAccount() throws Exception {
		IUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IUserExternalAccount.class, domainUid, user1);

		String externalSystemId1 = "xCloud1";
		String externalSystemId2 = "xCloud2";

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		assertNull(accountService.get(externalSystemId1));
		assertNull(accountService.get(externalSystemId2));

		accountService.create(externalSystemId1, account);
		accountService.create(externalSystemId2, account);

		assertNotNull(accountService.get(externalSystemId1));
		assertNotNull(accountService.get(externalSystemId2));

		accountService.delete(externalSystemId1);
		assertNull(accountService.get(externalSystemId1));
		assertNotNull(accountService.get(externalSystemId2));
	}

	@Test
	public void testDeletingAllUserAccounts() throws Exception {
		IUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IUserExternalAccount.class, domainUid, user1);

		String externalSystemId1 = "xCloud1";
		String externalSystemId2 = "xCloud2";

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		assertNull(accountService.get(externalSystemId1));
		assertNull(accountService.get(externalSystemId2));

		accountService.create(externalSystemId1, account);
		accountService.create(externalSystemId2, account);

		IUserExternalAccount accountService2 = ServerSideServiceProvider.getProvider(userSecurityContext2)
				.instance(IUserExternalAccount.class, domainUid, user2);

		accountService2.create(externalSystemId2, account);

		assertNotNull(accountService.get(externalSystemId1));
		assertNotNull(accountService.get(externalSystemId2));
		assertNotNull(accountService2.get(externalSystemId2));

		accountService.deleteAll();
		assertNull(accountService.get(externalSystemId1));
		assertNull(accountService.get(externalSystemId2));
		assertNotNull(accountService2.get(externalSystemId2));

	}

	@Test
	public void testGettingAllUserAccounts() throws Exception {
		IUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IUserExternalAccount.class, domainUid, user1);

		String externalSystemId1 = "xCloud1";
		String externalSystemId2 = "xCloud2";

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		assertNull(accountService.get(externalSystemId1));
		assertNull(accountService.get(externalSystemId2));

		accountService.create(externalSystemId1, account);
		accountService.create(externalSystemId2, account);

		IUserExternalAccount accountService2 = ServerSideServiceProvider.getProvider(userSecurityContext2)
				.instance(IUserExternalAccount.class, domainUid, user2);

		accountService2.create(externalSystemId2, account);

		assertNotNull(accountService.get(externalSystemId1));
		assertNotNull(accountService.get(externalSystemId2));
		assertNotNull(accountService2.get(externalSystemId2));

		List<UserAccountInfo> me = accountService.getAll();
		List<UserAccountInfo> other = accountService2.getAll();
		assertEquals(2, me.size());
		assertEquals(1, other.size());

	}

	@Test
	public void testGettingCredentials() throws Exception {
		IInternalUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IInternalUserExternalAccount.class, domainUid, user1);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		accountService.create(externalSystemId, account);

		assertEquals("top-secret", accountService.getCredentials(externalSystemId));

	}

	@Test
	public void testUpdatingNullCredentialsShouldLeavePasswordUntouched() throws Exception {
		IInternalUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IInternalUserExternalAccount.class, domainUid, user1);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		accountService.create(externalSystemId, account);

		account.credentials = null;
		accountService.update(externalSystemId, account);

		assertEquals("top-secret", accountService.getCredentials(externalSystemId));

	}

	@Test
	public void testUpdatingCredentials() throws Exception {
		IInternalUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IInternalUserExternalAccount.class, domainUid, user1);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		accountService.create(externalSystemId, account);

		account.credentials = "new";
		accountService.update(externalSystemId, account);

		assertEquals("new", accountService.getCredentials(externalSystemId));
	}

	@Test
	public void testManageExternalAccountsRole() throws Exception {
		IInternalUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContextNoRole)
				.instance(IInternalUserExternalAccount.class, domainUid, userNoRole);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		try {
			accountService.create(externalSystemId, account);
			fail("WAT, I can create an account without owning the required role");
		} catch (Exception e) {
		}
	}

	@Test
	public void testCreatingExternalAccountForOtherUserShouldFail() throws Exception {
		IInternalUserExternalAccount accountService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IInternalUserExternalAccount.class, domainUid, user2);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		try {
			accountService.create(externalSystemId, account);
			fail("WAT, I can create an account for another user");
		} catch (Exception e) {
		}
	}

}
