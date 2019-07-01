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

package net.bluemind.backend.systemconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.systemconf.internal.MyNetworksSanitizor;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SystemConf;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MyNetworksSanitizorTests {
	private static final String PARAMETER = "mynetworks";
	private static final String DEFAULT_VALUE = "127.0.0.0/8";
	private static final String FAKE_IP = "10.11.12.13";
	private static final String FAKE_IP2 = "10.11.12.113";

	@Before
	public void beforeBefore() throws Exception {
		DateTimeZone.setDefault(DateTimeZone.UTC);
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		Server fakeServer = new Server();
		fakeServer.ip = FAKE_IP;
		fakeServer.tags = Lists.newArrayList("fake/tag");

		Server fakeServer2 = new Server();
		fakeServer2.ip = FAKE_IP2;
		fakeServer2.tags = Lists.newArrayList("fake/tag");

		PopulateHelper.initGlobalVirt(fakeServer, fakeServer2);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSanitizeNullPrevious() throws ServerFault {
		MyNetworksSanitizor mns = new MyNetworksSanitizor();

		try {
			mns.sanitize(null, new HashMap<String, String>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testSanitizeNullModifications() {
		MyNetworksSanitizor mns = new MyNetworksSanitizor();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();

		try {
			mns.sanitize(systemConf, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testSanitizeDefineDefaultNoPrevious() throws ServerFault {
		MyNetworksSanitizor mns = new MyNetworksSanitizor();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();

		HashMap<String, String> modifications = new HashMap<String, String>();
		mns.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));

		HashSet<String> vals = new HashSet<>(Arrays.asList(modifications.get(PARAMETER).replace(" ", "").split(",")));
		Set<String> defVals = MyNetworksSanitizor.getDefaultValue();
		assertEquals(vals.size(), defVals.size());
		for (String val : vals) {
			assertTrue(defVals.contains(val));
		}
	}

	@Test
	public void testSanitizeNoMyNetworksModifications() throws ServerFault {
		MyNetworksSanitizor mns = new MyNetworksSanitizor();

		HashMap<String, String> modifications = new HashMap<String, String>();
		mns.sanitize(new SystemConf(), modifications);

		assertFalse(modifications.containsKey(DEFAULT_VALUE));
	}

	@Test
	public void testSanitize() throws ServerFault {
		MyNetworksSanitizor mns = new MyNetworksSanitizor();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, DEFAULT_VALUE + " , 10.0.0.0/24");
		mns.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));

		String val = modifications.get(PARAMETER);
		for (String dv : MyNetworksSanitizor.getDefaultValue()) {
			assertTrue(val.contains(dv));
		}
		assertTrue(val.contains("10.0.0.0/24"));

		val = val.replace(" ", "");
		HashSet<String> vals = new HashSet<>(Arrays.asList(val.split(",")));
		vals.remove("10.0.0.0/24");
		for (String dv : MyNetworksSanitizor.getDefaultValue()) {
			vals.remove(dv);
		}

		assertEquals(0, vals.size());
	}

	@Test
	public void testGetDefaultValue() {
		Set<String> defaultValues = MyNetworksSanitizor.getDefaultValue();

		assertTrue(defaultValues.contains("127.0.0.0/8"));
		assertTrue(defaultValues.contains(FAKE_IP + "/32"));
		assertTrue(defaultValues.contains(FAKE_IP2 + "/32"));

		for (String val : defaultValues) {
			if (val.startsWith("127.")) {
				continue;
			}

			assertTrue(val.endsWith("/32"));
		}
	}
}
