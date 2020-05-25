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
package net.bluemind.backend.postfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.base.Strings;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.postfix.internal.PostfixPaths;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.persistence.SystemConfStore;

public class SmtpTagServerHookTests extends HooksTests {
	private BmContext testContext = new BmTestContext(SecurityContext.SYSTEM);

	@Override
	protected String getTestTag() {
		return "mail/smtp";
	}

	@Override
	protected String getServerIp() {
		return new BmConfIni().get("bluemind/smtp-role");
	}

	private void initBasicConfiguration(String key, String value) throws SQLException {
		SystemConfStore confStore = new SystemConfStore(JdbcActivator.getInstance().getDataSource());
		Map<String, String> values = confStore.get();
		values.put(key, value);
		confStore.update(values);
	}

	@Test
	public void testOnServerTagged() throws ServerFault, SQLException, IOException {
		initBasicConfiguration(SysConfKeys.external_url.name(), "smtp.bm.lan");
		initBasicConfiguration("mynetworks", "127.0.0.1/8, 10.0.0.0/16");
		initBasicConfiguration("message_size_limit", "10000");

		String mailSmtpTestFqdn = getFqdn(mailSmtpTestIp);

		rmMaps(new String[] { mailSmtpTestIp });

		new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());

		assertPostfixConfiguration("myhostname", mailSmtpTestFqdn);
		assertPostfixConfiguration("mynetworks", "127.0.0.1/8, 10.0.0.0/16");
		assertPostfixConfiguration("message_size_limit", "10000");
		assertPostfixConfiguration("mailbox_size_limit", "10000");

		ensureMapsExistsAndEmpty();
	}

	private String getFqdn(String mailSmtpTestIp) {
		INodeClient nc = NodeActivator.get(mailSmtpTestIp);

		TaskRef tr = nc.executeCommand("hostname -f");
		ExitList values = NCUtils.waitFor(nc, tr);
		assertEquals(1, values.size());
		assertFalse(Strings.isNullOrEmpty(values.get(0)));
		return values.get(0);
	}

	private ItemValue<Server> getServer() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(mailSmtpTestIp);
	}

	private void ensureMapsExistsAndEmpty() throws ServerFault {
		INodeClient nc = NodeActivator.get(mailSmtpTestIp);

		for (String mapFileName : mapsFileNames) {
			ExitList status = NCUtils.waitFor(nc, nc.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(0, status.getExitCode());

			assertTrue(new String(nc.read(mapFileName + "-flat")).isEmpty());

			status = NCUtils.waitFor(nc, nc.executeCommand("test -e " + mapFileName + ".db"));
			assertEquals(0, status.getExitCode());
		}
	}

	private void assertPostfixConfiguration(String key, String value) throws ServerFault {
		INodeClient nc = NodeActivator.get(mailSmtpTestIp);

		TaskRef tr = nc.executeCommand("postconf " + key);
		ExitList values = NCUtils.waitFor(nc, tr);
		values.forEach(v -> System.out.println(v));

		List<String> notEmptyValues = values.stream().filter(v -> !v.isEmpty()).collect(Collectors.toList());

		assertEquals(1, notEmptyValues.size());
		assertEquals(key + " = " + value, notEmptyValues.get(0));
	}

	@Test
	public void testOnServerTaggedInvalidServerUid() throws ServerFault {

		ItemValue<Server> server = new ItemValue<Server>();
		server.value = new Server();
		server.uid = "invaliduid";
		try {
			new SmtpTagServerHook().onServerTagged(testContext, server, getTestTag());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testOnServerTaggedInvalidExternalUrl() throws ServerFault {

		try {
			new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains(SysConfKeys.external_url.name()));
		}
	}

	@Test
	public void testOnServerTaggedEmptyExternalUrl() throws ServerFault, SQLException {
		initBasicConfiguration(SysConfKeys.external_url.name(), "");

		try {
			new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains(SysConfKeys.external_url.name()));
		}
	}

	@Test
	public void testOnServerTaggedInvalidMyNetworks() throws ServerFault, SQLException {
		initBasicConfiguration(SysConfKeys.external_url.name(), "test.bm.lam");

		try {
			new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains("mynetworks"));
		}
	}

	@Test
	public void testOnServerTaggedEmptyMyNetworks() throws ServerFault, SQLException {
		initBasicConfiguration(SysConfKeys.external_url.name(), "test.bm.lam");
		initBasicConfiguration("mynetworks", "");

		try {
			new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains("mynetworks"));
		}
	}

	@Test
	public void testOnServerTaggedNullStringMyNetworks() throws ServerFault, SQLException {
		initBasicConfiguration(SysConfKeys.external_url.name(), "test.bm.lam");
		initBasicConfiguration("mynetworks", "null");

		try {
			new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains("mynetworks"));
		}
	}

	@Test
	public void testOnServerTaggedInvalidMessageSizeLimitShouldUseDefaultValue() throws Exception {
		initBasicConfiguration(SysConfKeys.external_url.name(), "test.bm.lam");
		initBasicConfiguration("mynetworks", "127.0.0.1/8");

		new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());

		Map<String, String> configData = readPostfixconf();
		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));

	}

	@Test
	public void testOnServerTaggedEmptyMessageSizeLimitShouldUseDefaultValue() throws Exception {
		initBasicConfiguration(SysConfKeys.external_url.name(), "test.bm.lam");
		initBasicConfiguration("mynetworks", "127.0.0.1/8");
		initBasicConfiguration("message_size_limit", "");

		new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());

		Map<String, String> configData = readPostfixconf();
		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));

	}

	@Test
	public void testOnServerTaggedNullStringMessageSizeLimitShouldUseDefaultValue() throws Exception {
		initBasicConfiguration(SysConfKeys.external_url.name(), "test.bm.lam");
		initBasicConfiguration("mynetworks", "127.0.0.1/8");
		initBasicConfiguration("message_size_limit", "null");

		new SmtpTagServerHook().onServerTagged(testContext, getServer(), getTestTag());

		Map<String, String> configData = readPostfixconf();
		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));

	}

	private Map<String, String> readPostfixconf() throws Exception {
		Map<String, String> config = new HashMap<>();
		String path = PostfixPaths.MAIN_CF;
		INodeClient nc = NodeActivator.get(getServerIp());
		StringBuilder data = new StringBuilder();
		try (InputStream in = nc.openStream(path)) {
			int i;
			while ((i = in.read()) != -1) {
				data.append((char) i);
			}
		}
		String[] lines = data.toString().split("\n");
		for (String line : lines) {
			if (line.contains("=")) {
				String[] kv = line.split("=");
				String key = kv[0].toLowerCase().trim();
				String value = kv[1].toLowerCase().trim();
				config.put(key, value);
			}
		}

		return config;
	}

	@Test
	public void onServerAssigned_supportedTag() throws Exception {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new SmtpTagServerHook().onServerAssigned(new BmTestContext(SecurityContext.SYSTEM), getServer(), domain,
				getTestTag());

		Message<JsonObject> message = dirtyMapChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void onServerAssigned_unsupportedTag() throws Exception {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new SmtpTagServerHook().onServerAssigned(testContext, getServer(), domain, "nostmptag");

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onServerUnassigned_unsupportedTag() throws Exception {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new SmtpTagServerHook().onServerUnassigned(testContext, getServer(), domain, "nostmptag");

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onServerUnassigned_supportedTag() throws Exception {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new SmtpTagServerHook().onServerUnassigned(testContext, getServer(), domain, getTestTag());

		Message<JsonObject> message = dirtyMapChecker.shouldSuccess();
		assertNotNull(message);
	}
}
