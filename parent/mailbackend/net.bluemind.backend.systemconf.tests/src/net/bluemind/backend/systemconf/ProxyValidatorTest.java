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
package net.bluemind.backend.systemconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.backend.systemconf.internal.ProxyValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class ProxyValidatorTest {
	private SystemConf getSystemConf() {
		Map<String, String> systemConf = new HashMap<>();
		systemConf.put(SysConfKeys.http_proxy_enabled.name(), "false");
		systemConf.put(SysConfKeys.http_proxy_hostname.name(), "hostname");
		systemConf.put(SysConfKeys.http_proxy_port.name(), "3128");
		systemConf.put(SysConfKeys.http_proxy_login.name(), "login");
		systemConf.put(SysConfKeys.http_proxy_password.name(), "password");
		systemConf.put(SysConfKeys.http_proxy_exceptions.name(), "e1,e2");

		return SystemConf.create(systemConf);
	}

	@Test
	public void invalidButDisabled() {
		SystemConf systemConf = getSystemConf();

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_port.name(), "invalid");

		new ProxyValidator().validate(systemConf, modifications);

		systemConf.values.remove(SysConfKeys.http_proxy_enabled.name());
		new ProxyValidator().validate(systemConf, modifications);
	}

	@Test
	public void hostname() {
		SystemConf systemConf = getSystemConf();

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_hostname.name(), "");

		// Invalid but disabled
		new ProxyValidator().validate(systemConf, modifications);

		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");

		// Invalid and enabled
		for (String invalidValues : Arrays.asList("", null)) {
			modifications.put(SysConfKeys.http_proxy_hostname.name(), invalidValues);
			try {
				new ProxyValidator().validate(systemConf, modifications);
				fail("Test must thrown an exception");
			} catch (ServerFault sf) {
				assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
				assertTrue(sf.getMessage().equalsIgnoreCase("Proxy hostname must be defined"));
			}
		}

		// Valid and enabled
		modifications.put(SysConfKeys.http_proxy_hostname.name(), "valid");
		new ProxyValidator().validate(systemConf, modifications);
	}

	@Test
	public void port() {
		SystemConf systemConf = getSystemConf();

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_port.name(), "");

		// Invalid but disabled
		new ProxyValidator().validate(systemConf, modifications);

		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");

		// Invalid and enabled
		for (String invalidValues : Arrays.asList("", "invalid", null)) {
			modifications.put(SysConfKeys.http_proxy_port.name(), invalidValues);
			try {
				new ProxyValidator().validate(systemConf, modifications);
				fail("Test must thrown an exception");
			} catch (ServerFault sf) {
				assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
				assertTrue(sf.getMessage().equalsIgnoreCase("Proxy port must be an integer"));
			}
		}

		for (String invalidValues : Arrays.asList("-12", "0", "65536")) {
			modifications.put(SysConfKeys.http_proxy_port.name(), invalidValues);
			try {
				new ProxyValidator().validate(systemConf, modifications);
				fail("Test must thrown an exception");
			} catch (ServerFault sf) {
				assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
				assertTrue(sf.getMessage().equalsIgnoreCase("Proxy port must be an integer between 1 and 65535"));
			}
		}

		// Valid and enabled
		modifications.put(SysConfKeys.http_proxy_port.name(), "12345");
		new ProxyValidator().validate(systemConf, modifications);
	}

	@Test
	public void loginNoPassword() {
		SystemConf systemConf = getSystemConf();
		systemConf.values.remove(SysConfKeys.http_proxy_password.name());

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");
		modifications.put(SysConfKeys.http_proxy_login.name(), "loginupd");

		try {
			new ProxyValidator().validate(systemConf, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains("Proxy password must be defined for login"));
			assertTrue(sf.getMessage().contains("loginupd"));
		}

		modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");

		try {
			new ProxyValidator().validate(systemConf, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains("Proxy password must be defined for login"));
			assertTrue(sf.getMessage().contains("login"));
		}
	}

	@Test
	public void passwordNoLogin() {
		SystemConf systemConf = getSystemConf();
		systemConf.values.remove(SysConfKeys.http_proxy_login.name());

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");
		modifications.put(SysConfKeys.http_proxy_password.name(), "passwordupd");

		try {
			new ProxyValidator().validate(systemConf, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains("Proxy login must be defined"));
		}

		modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");

		try {
			new ProxyValidator().validate(systemConf, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains("Proxy login must be defined"));
		}
	}

	@Test
	public void noLoginNoPassword() {
		SystemConf systemConf = getSystemConf();
		systemConf.values.remove(SysConfKeys.http_proxy_login.name());
		systemConf.values.remove(SysConfKeys.http_proxy_password.name());

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");

		new ProxyValidator().validate(systemConf, modifications);
	}

	@Test
	public void loginPassword() {
		SystemConf systemConf = getSystemConf();
		systemConf.values.remove(SysConfKeys.http_proxy_login.name());
		systemConf.values.remove(SysConfKeys.http_proxy_password.name());

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.http_proxy_enabled.name(), "true");
		systemConf.values.put(SysConfKeys.http_proxy_login.name(), "login");
		systemConf.values.put(SysConfKeys.http_proxy_password.name(), "password");

		new ProxyValidator().validate(systemConf, modifications);
	}
}
