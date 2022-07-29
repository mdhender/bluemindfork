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

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsExternalUrlValidatorTests {

	private String domainUid1 = "bm.lan";
	private String domainUid2 = "bm2.lan";
	private BmTestContext domain1Admin;
	private BmTestContext domain1System;
	private BmTestContext admin0 = new BmTestContext(SecurityContext.SYSTEM);
	private DomainSettingsValidator validator = new DomainSettingsValidator();
	private String globalExternalUrl = "global.ext.url.tld";
	private String domainExternalUrl = "domain.ext.url.tld";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid1);
		PopulateHelper.createTestDomain(domainUid2);

		domain1Admin = BmTestContext.contextWithSession("adminSessionId", "testAdmin", domainUid1,
				BasicRoles.ROLE_ADMIN);

		domain1System = BmTestContext.contextWithSession("systemSessionId1", "testSystem1", domainUid1,
				BasicRoles.ROLE_SYSTEM_MANAGER);

		Map<String, String> globalSettings = new HashMap<>();
		globalSettings.put(SysConfKeys.external_url.name(), globalExternalUrl);
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISystemConfiguration.class)
				.updateMutableValues(globalSettings);

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.external_url.name(), domainExternalUrl);
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid2)
				.set(domainSettings);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
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

	@Test
	public void checkDomainUrl_alreadyusedasglobal() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.ext.tld");
		validator.create(domain1System, settings, domainUid1);
		validator.update(domain1System, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), globalExternalUrl);
		try {
			validator.create(domain1System, settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings.put(DomainSettingsKeys.external_url.name(), globalExternalUrl);
		try {
			validator.update(domain1System, Collections.emptyMap(), settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void checkDomainUrl_alreadyused() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.ext.tld");
		validator.create(domain1System, settings, domainUid1);
		validator.update(domain1System, Collections.emptyMap(), settings, domainUid1);

		settings.put(DomainSettingsKeys.external_url.name(), domainExternalUrl);
		try {
			validator.create(domain1System, settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings.put(DomainSettingsKeys.external_url.name(), domainExternalUrl);
		try {
			validator.update(domain1System, Collections.emptyMap(), settings, domainUid1);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}
}
