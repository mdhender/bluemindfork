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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.service.internal.SanitizorHook;
import net.bluemind.system.service.internal.ValidatorHook;

public class SystemConfigurationTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		ValidatorHook.throwException = false;
	}

	@Test
	public void testGetValues() {
		try {
			SystemConf conf = service(SecurityContext.SYSTEM).getValues();
			assertNotNull(conf);
			assertNotNull(conf.values);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetValuesAsAnonymous() {
		try {
			service(SecurityContext.ANONYMOUS).getValues();
			fail("only admin0 can call this service");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void updateToEmptyOrNullValue() throws ServerFault {
		Map<String, String> values = new HashMap<>();
		values.put("test", "check");
		values.put("test2", "check");
		service(SecurityContext.SYSTEM).updateMutableValues(values);

		values.put("test", "");
		service(SecurityContext.SYSTEM).updateMutableValues(values);

		SystemConf conf = service(SecurityContext.SYSTEM).getValues();
		assertEquals("", conf.values.get("test"));

		values.put("test", "check");
		values.put("test2", null);
		service(SecurityContext.SYSTEM).updateMutableValues(values);

		conf = service(SecurityContext.SYSTEM).getValues();
		assertFalse(conf.values.containsKey("test2"));
		assertNull(conf.values.get("test2"));
	}

	@Test
	public void testUpdateValues() throws ServerFault {
		Map<String, String> values = new HashMap<>();
		values.put("test", "check");
		service(SecurityContext.SYSTEM).updateMutableValues(values);

		SystemConf conf = service(SecurityContext.SYSTEM).getValues();
		assertNotNull(conf);
		assertNotNull(conf.values);
		assertEquals("check", conf.values.get("test"));

		values = new HashMap<>();
		values.put("test2", "check2");
		service(SecurityContext.SYSTEM).updateMutableValues(values);

		conf = service(SecurityContext.SYSTEM).getValues();
		assertNotNull(conf);
		assertNotNull(conf.values);
		assertEquals("check2", conf.values.get("test2"));

		// check is still there
		assertEquals("check", conf.values.get("test"));
	}

	@Test
	public void testUpdateValuesSanitizor() throws ServerFault {
		Map<String, String> values = new HashMap<>();
		values.put("test", "check");
		values.put(SanitizorHook.PARAMETER, "check");
		service(SecurityContext.SYSTEM).updateMutableValues(values);

		SystemConf conf = service(SecurityContext.SYSTEM).getValues();
		assertNotNull(conf);
		assertNotNull(conf.values);
		assertEquals("check", conf.values.get("test"));
		assertEquals(SanitizorHook.SANITIZED_VALUE, conf.values.get(SanitizorHook.PARAMETER));

		values = new HashMap<>();
		values.put(SanitizorHook.PARAMETER, null);
		service(SecurityContext.SYSTEM).updateMutableValues(values);
		conf = service(SecurityContext.SYSTEM).getValues();
		assertNull(conf.values.get(SanitizorHook.PARAMETER));
	}

	@Test
	public void testUpdateValuesNotAdmin0() throws ServerFault {
		try {
			service(SecurityContext.ANONYMOUS).updateMutableValues(new HashMap<String, String>());
			fail("only admin0 can call this service");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testUpdateValuesValidatorThrow() throws ServerFault {
		ValidatorHook.throwException = true;
		try {
			service(SecurityContext.SYSTEM).updateMutableValues(new HashMap<String, String>());
			fail("should have failed because of validator");
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testUpdateValuesInvalidParameter() throws ServerFault {
		try {
			service(SecurityContext.SYSTEM).updateMutableValues(null);
			fail("should have failed");
		} catch (ServerFault e) {

		}
	}

	private ISystemConfiguration service(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(ISystemConfiguration.class);

	}
}
