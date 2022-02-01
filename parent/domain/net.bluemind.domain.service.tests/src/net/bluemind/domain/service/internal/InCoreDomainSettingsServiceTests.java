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
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class InCoreDomainSettingsServiceTests {

	private String testDomainUid;
	private ISystemConfiguration globalSettingsApi;
	private Map<String, String> globalSettings;

	private static final String DOMAIN_EXTERNAL_URL = "my.domain.external.url";
	private static final String GLOBAL_EXTERNAL_URL = "global.external.url";
	private static final String DOMAIN_DEFAULT_DOMAIN = "my.domain.default.domain";
	private static final String GLOBAL_DEFAULT_DOMAIN = "global.default.domain";
	private static final String DEFAULT_EXTERNAL_URL = "default.external.url";
	private static final String DEFAULT_DEFAULT_DOMAIN = "default.default.domain";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		testDomainUid = System.currentTimeMillis() + "test.lan";

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();
		PopulateHelper.createDomain(testDomainUid, "al.test.lan", DOMAIN_DEFAULT_DOMAIN);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		// initialize global settings
		globalSettingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		globalSettings = new HashMap<>();
		globalSettings.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		globalSettings.put(SysConfKeys.default_domain.name(), GLOBAL_DEFAULT_DOMAIN);
		globalSettingsApi.updateMutableValues(globalSettings);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IInCoreDomainSettings getSettingsService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IInCoreDomainSettings.class, testDomainUid);
	}

	@Test
	public void testGetExternalUrl_domain() {
		IInCoreDomainSettings settingsService = getSettingsService(SecurityContext.SYSTEM);
		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.external_url.name(), DOMAIN_EXTERNAL_URL);
		settingsService.set(domainSettings);

		Optional<String> url = settingsService.getExternalUrl();
		assertEquals(DOMAIN_EXTERNAL_URL, url.get());
	}

	@Test
	public void testGetDefaultDomain_domain() {
		IInCoreDomainSettings settingsService = getSettingsService(SecurityContext.SYSTEM);
		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.default_domain.name(), DOMAIN_DEFAULT_DOMAIN);
		settingsService.set(domainSettings);

		Optional<String> url = settingsService.getDefaultDomain();
		assertEquals(DOMAIN_DEFAULT_DOMAIN, url.get());
	}

	@Test
	public void testGetExternalUrl_global() {
		String url = getSettingsService(SecurityContext.SYSTEM).getExternalUrl()
				.orElseGet(() -> globalSettingsApi.getValues().values.get(SysConfKeys.external_url.name()));
		assertNotNull(url);
		assertEquals(GLOBAL_EXTERNAL_URL, url);
	}

	@Test
	public void testGetDefaultDomain_global() {
		String url = getSettingsService(SecurityContext.SYSTEM).getDefaultDomain()
				.orElseGet(() -> globalSettingsApi.getValues().values.get(SysConfKeys.default_domain.name()));
		assertNotNull(url);
		assertEquals(GLOBAL_DEFAULT_DOMAIN, url);
	}

	@Test
	public void testGetExternalUrl_default() {
		globalSettings.put(SysConfKeys.external_url.name(), null);
		globalSettingsApi.updateMutableValues(globalSettings);

		String url = getSettingsService(SecurityContext.SYSTEM).getExternalUrl()
				.orElseGet(() -> globalSettingsApi.getValues().values.getOrDefault(SysConfKeys.external_url.name(),
						DEFAULT_EXTERNAL_URL));
		assertNotNull(url);
		assertEquals(DEFAULT_EXTERNAL_URL, url);
	}

	@Test
	public void testGetDefaultDomain_default() {
		globalSettings.put(SysConfKeys.default_domain.name(), null);
		globalSettingsApi.updateMutableValues(globalSettings);

		String url = getSettingsService(SecurityContext.SYSTEM).getDefaultDomain()
				.orElseGet(() -> globalSettingsApi.getValues().values.getOrDefault(SysConfKeys.default_domain.name(),
						DEFAULT_DEFAULT_DOMAIN));
		assertNotNull(url);
		assertEquals(DEFAULT_DEFAULT_DOMAIN, url);
	}

}
