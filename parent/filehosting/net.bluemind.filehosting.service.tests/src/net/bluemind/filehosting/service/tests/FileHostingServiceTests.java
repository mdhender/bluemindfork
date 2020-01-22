/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.filehosting.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.filehosting.api.Configuration;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class FileHostingServiceTests {

	private String domainUid;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		domainUid = "fhtest" + System.currentTimeMillis() + ".loc";
		PopulateHelper.createDomain(domainUid);
	}

	@After
	public void tearDown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testEmptyConfigShouldReturnDefaults() {

		IFileHosting fh = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IFileHosting.class,
				domainUid);

		Configuration configuration = fh.getConfiguration();

		assertEquals(365, configuration.retentionTime);
		assertEquals(0, configuration.maxFilesize);

	}

	@SuppressWarnings("serial")
	@Test
	public void testDomainConfig() {

		IFileHosting fh = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IFileHosting.class,
				domainUid);

		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		settings.set(new HashMap<String, String>() {
			{
				put(GlobalSettingsKeys.filehosting_max_filesize.name(), "1024");
				put(GlobalSettingsKeys.filehosting_retention.name(), "30");
			}
		});

		Configuration configuration = fh.getConfiguration();

		assertEquals(30, configuration.retentionTime);
		assertEquals(1024, configuration.maxFilesize);

	}

	@SuppressWarnings("serial")
	@Test
	public void testGlobalConfig() {

		IFileHosting fh = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IFileHosting.class,
				domainUid);

		IGlobalSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		settings.set(new HashMap<String, String>() {
			{
				put(GlobalSettingsKeys.filehosting_max_filesize.name(), "2048");
				put(GlobalSettingsKeys.filehosting_retention.name(), "300");
			}
		});

		Configuration configuration = fh.getConfiguration();

		assertEquals(300, configuration.retentionTime);
		assertEquals(2048, configuration.maxFilesize);

	}

	@SuppressWarnings("serial")
	@Test
	public void testDomainConfigShouldOverwriteGlobalConfig() {

		IFileHosting fh = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IFileHosting.class,
				domainUid);

		IGlobalSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		settings.set(new HashMap<String, String>() {
			{
				put(GlobalSettingsKeys.filehosting_max_filesize.name(), "500");
				put(GlobalSettingsKeys.filehosting_retention.name(), "400");
			}
		});

		IDomainSettings settingsDomain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		settingsDomain.set(new HashMap<String, String>() {
			{
				put(GlobalSettingsKeys.filehosting_max_filesize.name(), "350");
				put(GlobalSettingsKeys.filehosting_retention.name(), "700");
			}
		});

		Configuration configuration = fh.getConfiguration();

		assertEquals(700, configuration.retentionTime);
		assertEquals(350, configuration.maxFilesize);

	}

	@SuppressWarnings("serial")
	@Test
	public void testDomainConfigMaxFilesizeCannotExceedGlobalConfig() {
		IGlobalSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		settings.set(new HashMap<String, String>() {
			{
				put(GlobalSettingsKeys.filehosting_max_filesize.name(), "500");
			}
		});

		IDomainSettings settingsDomain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		try {
			settingsDomain.set(new HashMap<String, String>() {
				{
					put(GlobalSettingsKeys.filehosting_max_filesize.name(), "800");
				}
			});
			fail();
		} catch (ServerFault e) {
			assertTrue(e instanceof ServerFault);
			assertTrue(e.getMessage().contains(
					"Domain specific value for Filehosting (Max File size) cannot be greater than system value"));
		}

	}

}
