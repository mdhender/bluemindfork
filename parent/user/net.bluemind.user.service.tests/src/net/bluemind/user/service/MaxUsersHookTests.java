/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.user.service;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.MaxUsersHook;

public class MaxUsersHookTests {

	private String domainUid;
	private BmTestContext context;
	private IDomainSettings settingsService;
	private MaxUsersHook hook;

	@Before
	public void before() throws Exception {

		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.createTestDomain(domainUid, pipo);

		context = new BmTestContext(SecurityContext.SYSTEM);
		settingsService = ServerSideServiceProvider.getProvider(context).instance(IDomainSettings.class, domainUid);

		hook = new MaxUsersHook();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testBasicAccount_noLimit() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.domain_max_basic_account.name(), "");
		settingsService.set(settings);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.SIMPLE));
		} catch (ServerFault sf) {
			fail();
		}
	}

	@Test
	public void testBasicAccount_forbidden() {
		Map<String, String> settings = new HashMap<String, String>();
		settingsService.set(settings);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.SIMPLE));
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void testBasicAccount_0_forbidden() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.domain_max_basic_account.name(), "0");
		settingsService.set(settings);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.SIMPLE));
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void testBasicAccount_maxReached() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.domain_max_basic_account.name(), "1");
		settingsService.set(settings);

		PopulateHelper.addSimpleUser("simple", domainUid, Routing.none);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.SIMPLE));
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void testVisioAccount_noLimit() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.domain_max_fullvisio_accounts.name(), "");
		settingsService.set(settings);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.FULL_AND_VISIO));
		} catch (ServerFault sf) {
			fail();
		}
	}

	@Test
	public void testVisioAccount_forbidden() {
		Map<String, String> settings = new HashMap<String, String>();
		settingsService.set(settings);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.FULL_AND_VISIO));
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void testVisioAccount_0_forbidden() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.domain_max_fullvisio_accounts.name(), "0");
		settingsService.set(settings);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.FULL_AND_VISIO));
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void testVisioAccount_maxReached() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.domain_max_fullvisio_accounts.name(), "1");
		settingsService.set(settings);

		PopulateHelper.addVisioUser("fullandvisio", domainUid, Routing.internal);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.FULL_AND_VISIO));
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void testFullAccount_noLimit() {
		settingsService.set(Collections.emptyMap());

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.FULL));
		} catch (ServerFault sf) {
			fail();
		}
	}

	@Test
	public void testFullccount_maxReached() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.domain_max_users.name(), "1");
		settingsService.set(settings);

		PopulateHelper.addUser("user", domainUid, Routing.none);

		try {
			hook.beforeCreate(context, domainUid, null, defaultUser(AccountType.FULL));
			fail();
		} catch (ServerFault sf) {
		}
	}

	private User defaultUser(AccountType accountType) {
		User user = new User();
		user.accountType = accountType;
		return user;
	}
}
