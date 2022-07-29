/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.domain.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsDefaultDomainValidatorTests {
	private String domainUid1 = "bm.lan";
	private String domainUid2 = "default.bm.lan";
	private BmTestContext domain1Admin;

	private BmTestContext admin0 = new BmTestContext(SecurityContext.SYSTEM);
	private DomainSettingsValidator validator = new DomainSettingsValidator();

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid1);
		PopulateHelper.createTestDomain(domainUid2);

		domain1Admin = BmTestContext.contextWithSession("adminSessionId", "testAdmin", domainUid1,
				BasicRoles.ROLE_ADMIN);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void checkDefaultDomain_domainAdmin() {
		checkDefaultDomain_commons(domain1Admin);

		Map<String, String> settings = new HashMap<>();

		// domain1 admin can only manage default-domain on his domain
		settings.put(DomainSettingsKeys.default_domain.name(), domainUid2);
		try {
			validator.create(domain1Admin, settings, domainUid2);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			validator.update(domain1Admin, Collections.emptyMap(), settings, domainUid2);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		// domain1 admin can't set alias from domain2 as default-domain on his domain
		settings.put(DomainSettingsKeys.default_domain.name(), domainUid2);
		try {
			validator.create(domain1Admin, settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			validator.update(domain1Admin, Collections.emptyMap(), settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void checkDefaultDomain_admin0() {
		checkDefaultDomain_commons(admin0);

		// admin0 can set default-domain on all domains
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.default_domain.name(), domainUid2);
		validator.create(admin0, settings, domainUid2);
		validator.update(admin0, Collections.emptyMap(), settings, domainUid2);

		// admin0 can't set alias from domain2 as default-domain on domain1
		settings.put(DomainSettingsKeys.default_domain.name(), domainUid2);
		try {
			validator.create(admin0, settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			validator.update(admin0, Collections.emptyMap(), settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	private void checkDefaultDomain_commons(BmTestContext context) {
		Map<String, String> settings = new HashMap<>();
		validator.create(context, settings, domainUid1);
		validator.update(context, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.default_domain.name(), null);
		validator.create(context, settings, domainUid1);
		validator.update(context, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.default_domain.name(), "");
		validator.create(context, settings, domainUid1);
		validator.update(context, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.default_domain.name(), "invalid");
		try {
			validator.create(context, settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			validator.update(context, Collections.emptyMap(), settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings.put(DomainSettingsKeys.default_domain.name(), domainUid1);
		validator.create(context, settings, domainUid1);
		validator.update(context, Collections.emptyMap(), settings, domainUid1);
	}
}
