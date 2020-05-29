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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DefaultDomainHookTests {
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		domainUid = String.format("test-%s.lan", System.currentTimeMillis());
		PopulateHelper.createTestDomain(domainUid);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void sanitize_noKey() {
		Map<String, String> modifications = new HashMap<>();
		new DefaultDomainHook().sanitize(null, modifications);
		assertFalse(modifications.containsKey(SysConfKeys.default_domain.name()));
	}

	@Test
	public void sanitize_nullValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.default_domain.name(), null);

		new DefaultDomainHook().sanitize(null, modifications);
		assertEquals("", modifications.get(SysConfKeys.default_domain.name()));
	}

	@Test
	public void sanitize_trimValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.default_domain.name(), " value ");

		new DefaultDomainHook().sanitize(null, modifications);
		assertEquals("value", modifications.get(SysConfKeys.default_domain.name()));
	}

	@Test
	public void validate_noKey() {
		Map<String, String> modifications = new HashMap<>();
		new DefaultDomainHook().validate(null, modifications);
	}

	@Test
	public void validate_emptyValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.default_domain.name(), "");
		new DefaultDomainHook().validate(null, modifications);
	}

	@Test
	public void validate_invalidDomain() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.default_domain.name(), "invalid.lan");

		try {
			new DefaultDomainHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().contains("invalid.lan"));
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_validDomain() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.default_domain.name(), domainUid);
		new DefaultDomainHook().validate(null, modifications);
	}
}
