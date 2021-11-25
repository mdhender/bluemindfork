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
package net.bluemind.videoconferencing.service.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractVideoConferencingTests {

	protected SecurityContext context;

	protected final String domainUid = "bm.lan";

	IDomainSettings domainSettings;
	ISystemConfiguration systemConfiguration;

	protected static final String DOMAIN_EXTERNAL_URL = "my.test.domain.external.url";
	protected static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";

	protected Map<String, String> setGlobalExternalUrl() {
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);
		return sysValues;
	}

	protected Map<String, String> setDomainExternalUrl() {
		Map<String, String> domainValues = new HashMap<>();
		domainValues.put(DomainSettingsKeys.external_url.name(), DOMAIN_EXTERNAL_URL);
		domainSettings.set(domainValues);
		return domainValues;
	}

	protected Map<String, String> deleteGlobalExternalUrl() {
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), null);
		systemConfiguration.updateMutableValues(sysValues);
		return sysValues;
	}

	protected Map<String, String> deleteDomainExternalUrl() {
		Map<String, String> domainValues = new HashMap<>();
		domainValues.put(DomainSettingsKeys.external_url.name(), null);
		domainSettings.set(domainValues);
		return domainValues;
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();
		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});

		launched.await();

		context = SecurityContext.SYSTEM;

		systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);

		domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class,
				domainUid);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

}
