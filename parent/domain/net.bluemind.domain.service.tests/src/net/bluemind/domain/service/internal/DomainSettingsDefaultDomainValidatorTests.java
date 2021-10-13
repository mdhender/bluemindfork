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

import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsDefaultDomainValidatorTests {

	private static final String DOMAIN_SETTINGS_KEY = DomainSettingsKeys.default_domain.name();

	private DomainSettings emptySettings;
	private String domainUid = "bm.lan";
	private String domainUidDefault = "default.bm.lan";

	private DomainSettingsValidator validator = new DomainSettingsValidator();

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.createTestDomain(domainUidDefault);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		emptySettings = new DomainSettings(domainUid, new HashMap<>());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testNullDomainDefaultDomain() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());

		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);

		ds.settings.put(DOMAIN_SETTINGS_KEY, null);
		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);
	}

	@Test
	public void testEmptyDomainDefaultDomain() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());
		ds.settings.put(DOMAIN_SETTINGS_KEY, "");

		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);
	}

	@Test
	public void testValidDomainDefaultDomain() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());
		ds.settings.put(DOMAIN_SETTINGS_KEY, domainUidDefault);

		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);
	}

	@Test
	public void testInvalidDomainDefaultDomain() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());
		ds.settings.put(DOMAIN_SETTINGS_KEY, "invalid");

		try {
			validator.create(ds.settings, domainUid);
			fail("invalid " + DOMAIN_SETTINGS_KEY);
		} catch (ServerFault sf) {

		}
	}

}
