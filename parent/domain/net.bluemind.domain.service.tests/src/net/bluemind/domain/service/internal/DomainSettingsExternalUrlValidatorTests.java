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
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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

public class DomainSettingsExternalUrlValidatorTests {

	private String domainUid1 = "bm.lan";
	private BmTestContext domain1Admin;
	private BmTestContext admin0 = new BmTestContext(SecurityContext.SYSTEM);
	private DomainSettingsValidator validator = new DomainSettingsValidator();

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid1);

		domain1Admin = BmTestContext.contextWithSession("adminSessionId", "testAdmin", domainUid1,
				BasicRoles.ROLE_ADMIN);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});

		launched.await();
	}

	@Test
	public void checkDomainUrl_admin0() {
		Map<String, String> settings = new HashMap<>();
		validator.create(admin0, settings, domainUid1);
		validator.update(admin0, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), null);
		validator.create(admin0, settings, domainUid1);
		validator.update(admin0, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), "");
		validator.create(admin0, settings, domainUid1);
		validator.update(admin0, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		validator.create(admin0, settings, domainUid1);
		validator.update(admin0, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), "cpt");
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

	@Test
	public void checkDomainUrl_domainAdmin() {
		Map<String, String> settings = new HashMap<>();
		validator.create(domain1Admin, settings, domainUid1);
		validator.update(domain1Admin, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), null);
		validator.create(domain1Admin, settings, domainUid1);
		validator.update(domain1Admin, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), "");
		validator.create(domain1Admin, settings, domainUid1);
		validator.update(domain1Admin, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		try {
			validator.create(domain1Admin, settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}

		try {
			validator.update(domain1Admin, Collections.emptyMap(), settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}

		settings.put(DomainSettingsKeys.external_url.name(), "cpt");
		try {
			validator.create(domain1Admin, settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}

		try {
			validator.update(domain1Admin, Collections.emptyMap(), settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}
	}
}
