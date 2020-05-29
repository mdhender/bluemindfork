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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import net.bluemind.backend.postfix.internal.PostfixPaths;
import net.bluemind.backend.postfix.internal.cf.MainCf;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class SettingsHookTests extends HooksTests {

	@Before
	public void setup() throws Exception {
		super.before();

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		MainCf mainCf = new MainCf(serverService, getServerIp());
		mainCf.setHostname("bcad5a346f31");
		mainCf.setMyNetworks("127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128");
		mainCf.setMessageSizeLimit("10485760");
		mainCf.write();
	}

	List<ItemValue<Server>> getTaggedServers(IServer serverService, String... tag) throws ServerFault {

		List<ItemValue<Server>> all = serverService.allComplete();
		List<ItemValue<Server>> ret = new ArrayList<>();
		for (ItemValue<Server> server : all) {
			for (int i = 0; i < tag.length; i++) {
				if (server.value.tags.contains(tag[i])) {
					ret.add(server);
				}
			}
		}
		return ret;
	}

	@After
	public void teardown() throws Exception {
		new File(System.getProperty("java.io.tmpdir"), "bm-ini").delete();
		super.after();
	}

	@Override
	protected String getTestTag() {
		return "mail/smtp";
	}

	@Override
	protected String getServerIp() {
		return new BmConfIni().get("bluemind/smtp-role");
	}

	@Test
	public void testSaveSettings() throws Exception {

		ISystemConfiguration configService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);

		Map<String, String> configData = readPostfixconf();

		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));
		assertEquals("127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128", configData.get(SysConfKeys.mynetworks.name()));
		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("", configData.get("relayhost"));

		SystemConf values = configService.getValues();
		values.values.put(SysConfKeys.message_size_limit.name(), "120000");
		values.values.put(SysConfKeys.mynetworks.name(), "192.168.31.0/24, 127.0.0.0/8, 192.168.131.0/24");
		values.values.put(SysConfKeys.relayhost.name(), "test.relayhost.tld");

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("120000", configData.get(SysConfKeys.message_size_limit.name()));
		Set<String> actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8", "192.168.131.0/24").build(),
				actual);
		assertEquals("test.relayhost.tld", configData.get(SysConfKeys.relayhost.name()));
	}

	@Test
	public void testUpdateSettings() throws Exception {

		ISystemConfiguration configService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);

		Map<String, String> configData = readPostfixconf();

		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));
		assertEquals("127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128", configData.get(SysConfKeys.mynetworks.name()));
		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("", configData.get("relayhost"));

		SystemConf values = configService.getValues();
		values.values.put(SysConfKeys.message_size_limit.name(), "120000");
		values.values.put(SysConfKeys.mynetworks.name(), "192.168.31.0/24, 127.0.0.0/8, 192.168.131.0/24");
		values.values.put(SysConfKeys.relayhost.name(), "test.relayhost.tld");

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("120000", configData.get(SysConfKeys.message_size_limit.name()));
		Set<String> actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8", "192.168.131.0/24").build(),
				actual);
		assertEquals("test.relayhost.tld", configData.get(SysConfKeys.relayhost.name()));

		configService.getValues();
		values.values.put(SysConfKeys.message_size_limit.name(), "240000");
		values.values.put(SysConfKeys.mynetworks.name(), "127.0.0.0/8, 192.168.31.0/24");
		values.values.put(SysConfKeys.relayhost.name(), null);

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("240000", configData.get(SysConfKeys.message_size_limit.name()));
		actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8").build(), actual);
		assertEquals("", configData.get("relayhost"));
	}

	@Test
	public void testUpdateOnlyMessageSizeSettings() throws Exception {

		ISystemConfiguration configService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);

		Map<String, String> configData = readPostfixconf();

		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));
		assertEquals("127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128", configData.get(SysConfKeys.mynetworks.name()));
		assertEquals("bcad5a346f31", configData.get("myhostname"));

		SystemConf values = configService.getValues();
		values.values.put(SysConfKeys.message_size_limit.name(), "120000");
		values.values.put(SysConfKeys.mynetworks.name(), "192.168.31.0/24, 127.0.0.0/8, 192.168.131.0/24");

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("120000", configData.get(SysConfKeys.message_size_limit.name()));

		configService.getValues();
		values.values.put(SysConfKeys.message_size_limit.name(), "240000");

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("240000", configData.get(SysConfKeys.message_size_limit.name()));
		Set<String> actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8", "192.168.131.0/24").build(),
				actual);
	}

	@Test
	public void testUpdateOnlyMyNetworksSettings() throws Exception {

		ISystemConfiguration configService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);

		Map<String, String> configData = readPostfixconf();

		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));
		assertEquals("127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128", configData.get(SysConfKeys.mynetworks.name()));
		assertEquals("bcad5a346f31", configData.get("myhostname"));

		SystemConf values = configService.getValues();
		values.values.put(SysConfKeys.message_size_limit.name(), "120000");
		values.values.put(SysConfKeys.mynetworks.name(), "192.168.31.0/24, 127.0.0.0/8, 192.168.131.0/24");

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		Set<String> actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8", "192.168.131.0/24").build(),
				actual);

		configService.getValues();
		values.values.put(SysConfKeys.mynetworks.name(), "127.0.0.0/8, 192.168.31.0/24");

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("120000", configData.get(SysConfKeys.message_size_limit.name()));
		actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8").build(), actual);
	}

	@Test
	public void testUpdateSettingsWithSameValuesShouldDoNothing() throws Exception {

		ISystemConfiguration configService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);

		Map<String, String> configData = readPostfixconf();

		assertEquals("10485760", configData.get(SysConfKeys.message_size_limit.name()));
		assertEquals("10485760", configData.get("mailbox_size_limit"));
		assertEquals("127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128", configData.get(SysConfKeys.mynetworks.name()));
		assertEquals("bcad5a346f31", configData.get("myhostname"));

		SystemConf values = configService.getValues();
		values.values.put(SysConfKeys.message_size_limit.name(), "120000");
		values.values.put(SysConfKeys.mynetworks.name(), "192.168.31.0/24, 127.0.0.0/8, 192.168.131.0/24");

		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("120000", configData.get(SysConfKeys.message_size_limit.name()));
		assertEquals("120000", configData.get("mailbox_size_limit"));
		Set<String> actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8", "192.168.131.0/24").build(),
				actual);

		configService.getValues();
		configService.updateMutableValues(values.values);

		configData = readPostfixconf();

		assertEquals("bcad5a346f31", configData.get("myhostname"));
		assertEquals("120000", configData.get(SysConfKeys.message_size_limit.name()));
		actual = new HashSet<>();
		for (String v : configData.get(SysConfKeys.mynetworks.name()).split(",")) {
			actual.add(v.trim());
		}
		assertEquals(ImmutableSet.<String>builder().add("192.168.31.0/24", "127.0.0.0/8", "192.168.131.0/24").build(),
				actual);

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

	public static void resetSettings(IDomainSettings settingsService) throws ServerFault {
		settingsService.set(Collections.<String, String>emptyMap());
	}
}
