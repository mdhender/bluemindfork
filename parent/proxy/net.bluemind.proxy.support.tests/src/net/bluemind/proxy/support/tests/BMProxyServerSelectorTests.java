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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.asynchttpclient.Realm.AuthScheme;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.uri.Uri;
import org.junit.Test;

import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.proxy.support.BMProxyServerSelector;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class BMProxyServerSelectorTests {
	private Map<String, String> getProxyConf() {
		Map<String, String> proxyConf = new HashMap<>();
		proxyConf.put(SysConfKeys.http_proxy_enabled.name(), "true");
		proxyConf.put(SysConfKeys.http_proxy_hostname.name(), new BmConfIni().get(DockerContainer.PROXY.getName()));
		proxyConf.put(SysConfKeys.http_proxy_port.name(), "3128");

		return proxyConf;
	}

	@Test
	public void noSystemConf() {
		assertNull(new BMProxyServerSelector(new SystemConf()).select(Uri.create("http://sample.org")));
	}

	@Test
	public void httpProxyDisabled() {
		Map<String, String> proxyConf = getProxyConf();

		proxyConf.put(SysConfKeys.http_proxy_enabled.name(), "false");
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));

		proxyConf.put(SysConfKeys.http_proxy_enabled.name(), "invalid");
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));

		proxyConf.put(SysConfKeys.http_proxy_enabled.name(), "");
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));

		proxyConf.put(SysConfKeys.http_proxy_enabled.name(), null);
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));

		proxyConf.remove(SysConfKeys.http_proxy_enabled.name());
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));
	}

	@Test
	public void httpProxyEnable_invalidHostname() {
		Map<String, String> proxyConf = getProxyConf();

		proxyConf.put(SysConfKeys.http_proxy_hostname.name(), "");
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));

		proxyConf.put(SysConfKeys.http_proxy_hostname.name(), null);
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));

		proxyConf.remove(SysConfKeys.http_proxy_hostname.name());
		assertNull(new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org")));
	}

	@Test
	public void httpProxyEnable_invalidPort() {
		Map<String, String> proxyConf = getProxyConf();

		proxyConf.put(SysConfKeys.http_proxy_port.name(), "");
		ProxyServer proxyServer = new BMProxyServerSelector(SystemConf.create(proxyConf))
				.select(Uri.create("http://sample.org"));
		assertEquals(3128, proxyServer.getPort());

		proxyConf.put(SysConfKeys.http_proxy_port.name(), "invalid");
		proxyServer = new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org"));
		assertEquals(3128, proxyServer.getPort());

		proxyConf.put(SysConfKeys.http_proxy_port.name(), null);
		proxyServer = new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org"));
		assertEquals(3128, proxyServer.getPort());

		proxyConf.remove(SysConfKeys.http_proxy_port.name());
		proxyServer = new BMProxyServerSelector(SystemConf.create(proxyConf)).select(Uri.create("http://sample.org"));
		assertEquals(3128, proxyServer.getPort());
	}

	@Test
	public void httpProxyEnable_noAuth_noExceptions() {
		ProxyServer proxyServer = new BMProxyServerSelector(SystemConf.create(getProxyConf()))
				.select(Uri.create("http://sample.org"));
		assertNotNull(proxyServer);
		assertEquals(new BmConfIni().get(DockerContainer.PROXY.getName()), proxyServer.getHost());
		assertEquals(3128, proxyServer.getPort());
		assertNull(proxyServer.getRealm());
		assertEquals(3, proxyServer.getNonProxyHosts().size());
	}

	@Test
	public void httpProxyEnable_noExceptions() {
		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_login.name(), "login");
		proxyConf.put(SysConfKeys.http_proxy_password.name(), "password");

		ProxyServer proxyServer = new BMProxyServerSelector(SystemConf.create(proxyConf))
				.select(Uri.create("http://sample.org"));
		assertNotNull(proxyServer);
		assertEquals(new BmConfIni().get(DockerContainer.PROXY.getName()), proxyServer.getHost());
		assertEquals(3128, proxyServer.getPort());
		assertNotNull(proxyServer.getRealm());
		assertEquals("login", proxyServer.getRealm().getPrincipal());
		assertEquals("password", proxyServer.getRealm().getPassword());
		assertEquals(AuthScheme.BASIC, proxyServer.getRealm().getScheme());

		assertEquals(3, proxyServer.getNonProxyHosts().size());
		assertTrue(proxyServer.getNonProxyHosts().contains("127.0.0.1"));
		assertTrue(proxyServer.getNonProxyHosts().contains("localhost"));
		assertTrue(proxyServer.getNonProxyHosts().contains("localhost.localdomain"));
	}

	@Test
	public void httpProxyEnable_noAuth_exceptions() {
		Map<String, String> proxyConf = getProxyConf();
		proxyConf.put(SysConfKeys.http_proxy_exceptions.name(), "e1, e2, e3");

		ProxyServer proxyServer = new BMProxyServerSelector(SystemConf.create(proxyConf))
				.select(Uri.create("http://sample.org"));
		assertNotNull(proxyServer);
		assertEquals(new BmConfIni().get(DockerContainer.PROXY.getName()), proxyServer.getHost());
		assertEquals(3128, proxyServer.getPort());
		assertNull(proxyServer.getRealm());

		assertEquals(6, proxyServer.getNonProxyHosts().size());
		assertTrue(proxyServer.getNonProxyHosts().contains("127.0.0.1"));
		assertTrue(proxyServer.getNonProxyHosts().contains("localhost"));
		assertTrue(proxyServer.getNonProxyHosts().contains("localhost.localdomain"));
		assertTrue(proxyServer.getNonProxyHosts().contains("e1"));
		assertTrue(proxyServer.getNonProxyHosts().contains("e2"));
		assertTrue(proxyServer.getNonProxyHosts().contains("e3"));
	}
}
