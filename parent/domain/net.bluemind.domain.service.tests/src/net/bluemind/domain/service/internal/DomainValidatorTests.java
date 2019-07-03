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
package net.bluemind.domain.service.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainValidatorTests {

	private DomainValidator validator = new DomainValidator();
	private Container domainsContainer;
	private DomainStoreService domainStoreService;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();
		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);
		domainsContainer = containerStore.get(DomainsContainerIdentifier.getIdentifier());
		assertNotNull(domainsContainer);

		domainStoreService = new DomainStoreService(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, domainsContainer);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
	}

	@Test
	public void testNominal() {
		validateNotFail(Domain.create("test.lan", "test", null, Collections.<String> emptySet()));

		validateNotFail(Domain.create("test.lan.org", "test", "desc", new HashSet<>(Arrays.asList("test.lan"))));
	}

	@Test
	public void testNull() {
		validateFail(null);
	}

	@Test
	public void testDomainName() {
		Domain testData = Domain.create("test.lan", "test", null, Collections.<String> emptySet());
		testData.name = null;
		validateFail(testData);

		testData.name = "";
		validateFail(testData);

		// utf-8 not authorized into domain name
		testData.name = "€urope.Org";
		validateFail(testData);

		testData.name = "test.lan";
		validateNotFail(testData);

	}

	@Test
	public void testDomainLabel() {
		Domain testData = Domain.create("test.lan", null, null, Collections.<String> emptySet());
		validateFail(testData);

		testData.label = "";
		validateFail(testData);

		testData.label = "ok";
		validateNotFail(testData);
	}

	@Test
	public void testAliases() {

		Domain testData = Domain.create("test.lan", "test", null, null);

		validateFail(testData);

		// invalid alias
		testData.aliases = new HashSet<>(Arrays.asList("test"));
		validateFail(testData);

		testData.aliases = new HashSet<>(Arrays.asList("test.org", "test"));
		validateFail(testData);

		testData.aliases = new HashSet<>();
		validateNotFail(testData);
		testData.aliases = new HashSet<>(Arrays.asList("test.org"));
		validateNotFail(testData);
	}

	private void validateNotFail(Domain d) {

		try {
			validator.validate(domainStoreService, d);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail();
		}
	}

	private void validateFail(Domain d) {
		try {
			validator.validate(domainStoreService, d);
			fail();
		} catch (ServerFault e) {

		}
	}

}
