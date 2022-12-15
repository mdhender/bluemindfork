/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsValidatorLangTests {

	private String domainUid1;
	private BmTestContext admin0 = new BmTestContext(SecurityContext.SYSTEM);

	private DomainSettingsValidator validator = new DomainSettingsValidator();

	@Before
	public void before() throws Exception {
		domainUid1 = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createTestDomain(domainUid1, esServer);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void langChecks() {
		Map<String, String> settings = new HashMap<>();
		validator.create(admin0, settings, domainUid1);

		try {
			settings.put(DomainSettingsKeys.lang.name(), null);
			validator.create(admin0, settings, domainUid1);
			fail();
		} catch (Exception e) {

		}

		try {
			settings.put(DomainSettingsKeys.lang.name(), "");
			validator.create(admin0, settings, domainUid1);
			fail();
		} catch (Exception e) {

		}

		try {
			settings.put(DomainSettingsKeys.lang.name(), "nonono");
			validator.create(admin0, settings, domainUid1);
			fail();
		} catch (Exception e) {

		}

		String[] allowed = new String[] { "de", "en", "es", "fr", "it", "pl", "sl", "zh", "hu" };
		for (String lang : allowed) {
			settings.put(DomainSettingsKeys.lang.name(), lang);
			validator.create(admin0, settings, domainUid1);
		}

	}

}
