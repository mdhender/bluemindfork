/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.system.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.system.api.CustomLogo;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.persistence.UpgraderStore;
import net.bluemind.system.persistence.SystemConfStore;

public class InstallationTests {

	private BmContext testContext;
	private BmTestContext anonContext;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();
		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		anonContext = new BmTestContext(SecurityContext.ANONYMOUS);

		Map<String, String> values = new HashMap<>();
		values.put("db_version", "test");

		new SystemConfStore(JdbcActivator.getInstance().getDataSource()).update(values);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGetVersion() throws Exception {

		InstallationVersion version = service().getVersion();
		assertNotNull(version);

		assertTrue(version.softwareVersion.length() > 0);
		assertEquals("test", version.databaseVersion);

		try {
			anonContext.provider().instance(IInstallation.class).getVersion();
			fail();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testMarkSchemaAsUpgraded() throws Exception {

		service().markSchemaAsUpgraded();
		UpgraderStore svs = new UpgraderStore(JdbcActivator.getInstance().getDataSource());
		assertEquals(1, svs.getComponentsVersion().size());
		assertEquals(BMVersion.getVersion(), svs.getComponentsVersion().get(0).version);
	}

	@Test
	public void testCustomlogo() throws Exception {
		InstallationId.reload();
		ContainerStore cs = new ContainerStore(testContext, JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM);
		cs.create(Container.create("installation_resources", "installation_resources", "installation_resources",
				SecurityContext.SYSTEM.getSubject(), true));

		assertNull(service().getLogo());

		try {
			service().setLogo(getLogo());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		try {
			CustomLogo cl = service().getLogo();
			assertNotNull(cl);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		try {
			service().deleteLogo();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		assertNull(service().getLogo());
	}

	private byte[] getLogo() throws ServerFault {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("data/logo.png")) {
			byte[] ret = ByteStreams.toByteArray(in);
			return ret;
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Test
	public void testResetIndexes() throws Exception {
		try {
			anonContext.provider().instance(IInstallation.class).resetIndexes();
			fail("should not be callable by anonymous");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}
	
	private List<String> notifiedEmails = new ArrayList<String>(Arrays.asList("email@email.fr", "rfzefze@mail.com", "fzefzef@fzef.fr"));
	
	@Test
	public void testSetAndGetNotifiedEmails() {
		try {
			service().setSubscriptionContacts(notifiedEmails);
		} catch (ServerFault e) {
			System.out.println(e);
			fail("set notified users should have worked.");
		}
		assertTrue(service().getSubscriptionContacts().equals(notifiedEmails));
	}

	@Test
	public void testSetInvalidMailOnNotifiedEmails() {
		try {
			notifiedEmails.add("dzefze.dez");
			service().setSubscriptionContacts(notifiedEmails);
			fail("set notified users should have failed because of an invalid email.");
		} catch (ServerFault e) {
			System.out.println(e);
		}
	}

	private IInstallation service() throws ServerFault {
		return testContext.provider().instance(IInstallation.class);
	}
}
