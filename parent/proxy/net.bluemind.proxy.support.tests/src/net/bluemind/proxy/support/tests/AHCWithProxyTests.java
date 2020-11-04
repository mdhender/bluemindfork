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
package net.bluemind.proxy.support.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.Response;
import org.junit.Test;

import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.proxy.support.AHCWithProxy;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class AHCWithProxyTests {
	private final String HTTP_TEST_ROOT_URL = String.format("http://%s/", new BmConfIni().get("proxy"));
	private final String ALLOWED_URL = String.format("%sallowed", HTTP_TEST_ROOT_URL);
	private final String FORBIDDEN_URL = String.format("%sforbidden", HTTP_TEST_ROOT_URL);
	private final String NEEDAUTH_URL = String.format("%sneed-auth", HTTP_TEST_ROOT_URL);

	private final String AUTH_ALLOWEDUSER = "alloweduser";
	private final String AUTH_ALLOWEDUSERPASSWORD = "password";

	private Map<String, String> getProxyConf() {
		Map<String, String> proxyConf = new HashMap<>();
		proxyConf.put(SysConfKeys.http_proxy_enabled.name(), "true");
		proxyConf.put(SysConfKeys.http_proxy_hostname.name(), new BmConfIni().get(DockerContainer.PROXY.getName()));
		proxyConf.put(SysConfKeys.http_proxy_port.name(), "3128");

		return proxyConf;
	}

	@Test
	public void noProxy() throws InterruptedException, ExecutionException {
		for (String url : Arrays.asList(ALLOWED_URL, FORBIDDEN_URL, NEEDAUTH_URL)) {
			Response response = AHCWithProxy.build(new SystemConf()).prepareGet(url).execute().get();
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test
	public void proxyDisabled() throws InterruptedException, ExecutionException {
		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_enabled.name(), "false");

		for (String url : Arrays.asList(ALLOWED_URL, FORBIDDEN_URL, NEEDAUTH_URL)) {
			Response response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(url).execute().get();
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test
	public void invalidProxyHost() throws InterruptedException, ExecutionException {
		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_hostname.name(), "127.0.0.1");

		try {
			AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(ALLOWED_URL).execute().get();
			fail("Test must thrown an exception");
		} catch (ExecutionException ee) {
		}
	}

	@Test
	public void invalidProxyPort() throws InterruptedException, ExecutionException {
		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_port.name(), "9999");

		try {
			AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(ALLOWED_URL).execute().get();
			fail("Test must thrown an exception");
		} catch (ExecutionException ee) {
		}
	}

	@Test
	public void validProxy_noAuth_noExceptions() throws InterruptedException, ExecutionException {
		Response response = AHCWithProxy.build(SystemConf.create(getProxyConf())).prepareGet(ALLOWED_URL).execute()
				.get();
		assertEquals(200, response.getStatusCode());

		response = AHCWithProxy.build(SystemConf.create(getProxyConf())).prepareGet(FORBIDDEN_URL).execute().get();
		assertEquals(403, response.getStatusCode());

		response = AHCWithProxy.build(SystemConf.create(getProxyConf())).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(407, response.getStatusCode());
	}

	@Test
	public void validProxy_noAuth_exceptions() throws InterruptedException, ExecutionException {
		Response response = AHCWithProxy.build(SystemConf.create(getProxyConf())).prepareGet(NEEDAUTH_URL).execute()
				.get();
		assertEquals(407, response.getStatusCode());

		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_exceptions.name(), new BmConfIni().get("proxy"));

		response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(200, response.getStatusCode());

		String[] dstParts = new BmConfIni().get("proxy").split("\\.");
		proxyConf.put(SysConfKeys.http_proxy_exceptions.name(), dstParts[0] + "." + dstParts[1] + ".*");

		response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(200, response.getStatusCode());

		proxyConf.put(SysConfKeys.http_proxy_exceptions.name(), "other.dst");

		response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(407, response.getStatusCode());
	}

	@Test
	public void validProxy_auth_noException() throws InterruptedException, ExecutionException {
		Response response = AHCWithProxy.build(SystemConf.create(getProxyConf())).prepareGet(NEEDAUTH_URL).execute()
				.get();
		assertEquals(407, response.getStatusCode());

		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_login.name(), "user");
		proxyConf.put(SysConfKeys.http_proxy_password.name(), "password");

		response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(403, response.getStatusCode());

		proxyConf.put(SysConfKeys.http_proxy_login.name(), AUTH_ALLOWEDUSER);
		proxyConf.put(SysConfKeys.http_proxy_password.name(), AUTH_ALLOWEDUSERPASSWORD);

		response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(200, response.getStatusCode());
	}

	@Test
	public void validProxy_auth_exception() throws InterruptedException, ExecutionException {
		Response response = AHCWithProxy.build(SystemConf.create(getProxyConf())).prepareGet(NEEDAUTH_URL).execute()
				.get();
		assertEquals(407, response.getStatusCode());

		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_login.name(), "user");
		proxyConf.put(SysConfKeys.http_proxy_password.name(), "password");
		response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(403, response.getStatusCode());

		proxyConf.put(SysConfKeys.http_proxy_exceptions.name(), new BmConfIni().get("proxy"));
		response = AHCWithProxy.build(SystemConf.create(proxyConf)).prepareGet(NEEDAUTH_URL).execute().get();
		assertEquals(200, response.getStatusCode());
	}
}
