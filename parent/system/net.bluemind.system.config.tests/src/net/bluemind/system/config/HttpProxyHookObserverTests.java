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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class HttpProxyHookObserverTests {
	private static final String PROXYVARS = "/etc/bm/proxy-vars";
	private static final String BM_TEST_SERVER = new BmConfIni().get("bluemind/node-tests");

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server server = new Server();
		server.ip = BM_TEST_SERVER;
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/imap");
		PopulateHelper.initGlobalVirt(false, server);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		NodeActivator.get(BM_TEST_SERVER).deleteFile(BM_TEST_SERVER);
	}

	private SystemConf getSystemConf() {
		Map<String, String> systemConf = new HashMap<>();
		systemConf.put(SysConfKeys.http_proxy_enabled.name(), "false");
		systemConf.put(SysConfKeys.http_proxy_hostname.name(), "172.17.3.4");
		systemConf.put(SysConfKeys.http_proxy_port.name(), "3128");
		systemConf.put(SysConfKeys.http_proxy_login.name(), "login");
		systemConf.put(SysConfKeys.http_proxy_password.name(), "password");
		systemConf.put(SysConfKeys.http_proxy_exceptions.name(), "e1,*.e2");

		return SystemConf.create(systemConf);
	}

	@Test
	public void proxyDisabled() {
		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), new SystemConf(), getSystemConf());

		assertEquals(1, NodeActivator.get(BM_TEST_SERVER).listFiles(PROXYVARS).size());
		assertTrue(new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)).isEmpty());
	}

	@Test
	public void proxyEnabled() {
		SystemConf systemConf = getSystemConf();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), Boolean.TRUE.toString());

		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), new SystemConf(), systemConf);

		String expected = "# DO NOT EDIT\n" + "# Setup proxy using bm-cli or AC\n"
				+ "http_proxy=http://login:password@172.17.3.4:3128/\n"
				+ "https_proxy=http://login:password@172.17.3.4:3128/\n" + "no_proxy=\"e1,.e2\"\n";
		assertEquals(expected, new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)));
	}

	@Test
	public void proxyUpdated_alreadyEnabled() {
		SystemConf oldSystemConf = getSystemConf();
		oldSystemConf.values.put(SysConfKeys.http_proxy_enabled.name(), Boolean.TRUE.toString());
		oldSystemConf.values.put(SysConfKeys.http_proxy_hostname.name(), "hostnameoldvalue");

		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), new SystemConf(), oldSystemConf);

		String expected = "# DO NOT EDIT\n" + "# Setup proxy using bm-cli or AC\n"
				+ "http_proxy=http://login:password@hostnameoldvalue:3128/\n"
				+ "https_proxy=http://login:password@hostnameoldvalue:3128/\n" + "no_proxy=\"e1,.e2\"\n";
		assertEquals(expected, new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)));

		SystemConf systemConf = getSystemConf();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), Boolean.TRUE.toString());

		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), oldSystemConf, systemConf);

		expected = "# DO NOT EDIT\n" + "# Setup proxy using bm-cli or AC\n"
				+ "http_proxy=http://login:password@172.17.3.4:3128/\n"
				+ "https_proxy=http://login:password@172.17.3.4:3128/\n" + "no_proxy=\"e1,.e2\"\n";
		assertEquals(expected, new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)));
	}

	@Test
	public void proxyUpdated_wasDisabled() {
		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), new SystemConf(), getSystemConf());

		assertEquals(1, NodeActivator.get(BM_TEST_SERVER).listFiles(PROXYVARS).size());
		assertTrue(new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)).isEmpty());

		SystemConf systemConf = getSystemConf();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), Boolean.TRUE.toString());

		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), getSystemConf(), systemConf);

		String expected = "# DO NOT EDIT\n" + "# Setup proxy using bm-cli or AC\n"
				+ "http_proxy=http://login:password@172.17.3.4:3128/\n"
				+ "https_proxy=http://login:password@172.17.3.4:3128/\n" + "no_proxy=\"e1,.e2\"\n";
		assertEquals(expected, new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)));
	}

	@Test
	public void proxyUpdated_enabledToDisabled() {
		SystemConf systemConf = getSystemConf();
		systemConf.values.put(SysConfKeys.http_proxy_enabled.name(), Boolean.TRUE.toString());

		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), new SystemConf(), systemConf);

		String expected = "# DO NOT EDIT\n" + "# Setup proxy using bm-cli or AC\n"
				+ "http_proxy=http://login:password@172.17.3.4:3128/\n"
				+ "https_proxy=http://login:password@172.17.3.4:3128/\n" + "no_proxy=\"e1,.e2\"\n";
		assertEquals(expected, new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)));

		new HttpProxyHook().onUpdated(new BmTestContext(SecurityContext.SYSTEM), systemConf, getSystemConf());

		assertEquals(1, NodeActivator.get(BM_TEST_SERVER).listFiles(PROXYVARS).size());
		assertTrue(new String(NodeActivator.get(BM_TEST_SERVER).read(PROXYVARS)).isEmpty());
	}
}
