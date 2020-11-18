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
package net.bluemind.system.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class HttpProxyHookSanitizorTest {
	@Test
	public void sanitizeNullPrevious() throws ServerFault {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		try {
			proxySanitizor.sanitize(null, new HashMap<String, String>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void sanitizeNullModifications() {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();

		try {
			proxySanitizor.sanitize(systemConf, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void noModifications() {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), "false");
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), "3128");

		Map<String, String> modifications = new HashMap<String, String>();

		proxySanitizor.sanitize(systemConf, modifications);
		assertTrue(modifications.isEmpty());
	}

	@Test
	public void sanitizeDefineDefaultNoPrevious() throws ServerFault {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<>();

		Map<String, String> modifications = new HashMap<>();
		proxySanitizor.sanitize(systemConf, modifications);

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_enabled.name()));
		assertFalse(Boolean.parseBoolean(modifications.get(SysConfKeys.http_proxy_enabled.name())));

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_port.name()));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));
	}

	@Test
	public void sanitizeDefineDefaultNullOrEmptyPrevious() throws ServerFault {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), null);
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), null);

		Map<String, String> modifications = new HashMap<>();
		proxySanitizor.sanitize(systemConf, modifications);

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_enabled.name()));
		assertFalse(Boolean.parseBoolean(modifications.get(SysConfKeys.http_proxy_enabled.name())));

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_port.name()));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));

		systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), "");
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), "");

		modifications = new HashMap<>();
		proxySanitizor.sanitize(systemConf, modifications);

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_enabled.name()));
		assertFalse(Boolean.parseBoolean(modifications.get(SysConfKeys.http_proxy_enabled.name())));

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_port.name()));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));
	}

	@Test
	public void sanitizeDefineDefaultNullOrEmptyPreviousEmptyModification() throws ServerFault {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), null);
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), null);

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), " ");
		modifications.put(SysConfKeys.http_proxy_port.name(), " ");
		proxySanitizor.sanitize(systemConf, modifications);

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_enabled.name()));
		assertFalse(Boolean.parseBoolean(modifications.get(SysConfKeys.http_proxy_enabled.name())));

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_port.name()));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));

		systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), "");
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), "");

		modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), " ");
		modifications.put(SysConfKeys.http_proxy_port.name(), " ");
		proxySanitizor.sanitize(systemConf, modifications);

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_enabled.name()));
		assertFalse(Boolean.parseBoolean(modifications.get(SysConfKeys.http_proxy_enabled.name())));

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_port.name()));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));
	}

	@Test
	public void sanitizeInvalidPort() throws ServerFault {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_port.name(), "invalid");

		proxySanitizor.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(SysConfKeys.http_proxy_port.name()));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));
	}

	@Test
	public void sanitizeValues() {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), "true");
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), "8888");

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), " invalid ");
		modifications.put(SysConfKeys.http_proxy_port.name(), " invalid ");
		modifications.put(SysConfKeys.http_proxy_hostname.name(), " hostname ");
		modifications.put(SysConfKeys.http_proxy_login.name(), " login ");
		modifications.put(SysConfKeys.http_proxy_password.name(), " password ");
		modifications.put(SysConfKeys.http_proxy_exceptions.name(), " exceptions ");

		proxySanitizor.sanitize(systemConf, modifications);

		assertFalse(Boolean.parseBoolean(modifications.get(SysConfKeys.http_proxy_enabled.name())));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));
		assertEquals("hostname", modifications.get(SysConfKeys.http_proxy_hostname.name()));
		assertEquals("login", modifications.get(SysConfKeys.http_proxy_login.name()));
		assertEquals("password", modifications.get(SysConfKeys.http_proxy_password.name()));
		assertEquals(" exceptions ", modifications.get(SysConfKeys.http_proxy_exceptions.name()));
	}

	@Test
	public void sanitizeNullValues() {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), "true");
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), "8888");

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), null);
		modifications.put(SysConfKeys.http_proxy_port.name(), null);
		modifications.put(SysConfKeys.http_proxy_hostname.name(), null);
		modifications.put(SysConfKeys.http_proxy_login.name(), null);
		modifications.put(SysConfKeys.http_proxy_password.name(), null);
		modifications.put(SysConfKeys.http_proxy_exceptions.name(), null);

		proxySanitizor.sanitize(systemConf, modifications);

		assertFalse(Boolean.parseBoolean(modifications.get(SysConfKeys.http_proxy_enabled.name())));
		assertEquals("3128", modifications.get(SysConfKeys.http_proxy_port.name()));
		assertNull(modifications.get(SysConfKeys.http_proxy_hostname.name()));
		assertNull(modifications.get(SysConfKeys.http_proxy_login.name()));
		assertNull(modifications.get(SysConfKeys.http_proxy_password.name()));
		assertNull(modifications.get(SysConfKeys.http_proxy_exceptions.name()));
	}

	@Test
	public void sanitizeLoginPasswword() {
		HttpProxyHook proxySanitizor = new HttpProxyHook();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<>();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), "true");
		systemConf.values.put(SysConfKeys.http_proxy_port.name(), "8888");

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), null);
		modifications.put(SysConfKeys.http_proxy_port.name(), null);
		modifications.put(SysConfKeys.http_proxy_hostname.name(), null);
		modifications.put(SysConfKeys.http_proxy_exceptions.name(), null);
		modifications.put(SysConfKeys.http_proxy_password.name(), "password");

		for (String login : Arrays.asList("", "  ")) {
			modifications.put(SysConfKeys.http_proxy_login.name(), login);
			proxySanitizor.sanitize(systemConf, modifications);

			assertEquals("", modifications.get(SysConfKeys.http_proxy_login.name()));
		}

		modifications.put(SysConfKeys.http_proxy_login.name(), "login");
		modifications.put(SysConfKeys.http_proxy_password.name(), null);

		for (String passwd : Arrays.asList("", "  ")) {
			modifications.put(SysConfKeys.http_proxy_password.name(), passwd);
			proxySanitizor.sanitize(systemConf, modifications);

			assertEquals("", modifications.get(SysConfKeys.http_proxy_password.name()));
		}

		modifications.put(SysConfKeys.http_proxy_login.name(), "login");
		modifications.put(SysConfKeys.http_proxy_password.name(), "password");
		proxySanitizor.sanitize(systemConf, modifications);

		assertEquals("login", modifications.get(SysConfKeys.http_proxy_login.name()));
		assertEquals("password", modifications.get(SysConfKeys.http_proxy_password.name()));
	}
}
