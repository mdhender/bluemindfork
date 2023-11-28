/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */

package net.bluemind.core.auditlogs.client.es.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.ilm.Phase;
import co.elastic.clients.elasticsearch.ilm.get_lifecycle.Lifecycle;
import net.bluemind.core.auditlogs.client.es.datastreams.DataStreamActivator;
import net.bluemind.core.auditlogs.exception.AuditLogCreationException;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.domain.service.tests.FakeDomainHook;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DataStreamTests {
	private static final String domainUid = "bm" + System.currentTimeMillis() + ".lan";
	private static final String domainUid01 = "bm01" + System.currentTimeMillis() + ".lan";
	private DataStreamActivator dataStreamActivator;

	private static final String AUDIT_LOG_DATASTREAM_PREFIX = "audit_log";
	private ElasticsearchClient esClient;
	private Container domainsContainer;
	private BmTestContext testContext;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		ContainerStore containerStore = new ContainerStore(testContext, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);
		domainsContainer = containerStore.get(DomainsContainerIdentifier.getIdentifier());
		assertNotNull(domainsContainer);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		FakeDomainHook.initFlags();
		esClient = ESearchActivator.getClient();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void createDataStream() throws AuditLogCreationException, IOException {
		dataStreamActivator = new DataStreamActivator();
		dataStreamActivator.setupAuditBackingStoreForDomain(domainUid);
		boolean isDataStream = !esClient.indices()
				.resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid)).dataStreams().isEmpty();
		assertTrue(isDataStream);
	}

	@Test
	public void checkILMPolicy() throws AuditLogCreationException, IOException {
		dataStreamActivator = new DataStreamActivator();
		dataStreamActivator.setupAuditBackingStoreForDomain(domainUid);
		Lifecycle lifeCycle = esClient.ilm().getLifecycle(b -> b.name("logs")).result().get("logs");
		assertNotNull(lifeCycle);
		Phase deletePhase = lifeCycle.policy().phases().delete();
		assertEquals("3d", deletePhase.minAge().time());
	}

	@Test
	public void removeDataStreamForNameAndDomain() throws AuditLogCreationException, IOException {
		dataStreamActivator = new DataStreamActivator();
		dataStreamActivator.setupAuditBackingStoreForDomain(domainUid);
		dataStreamActivator.setupAuditBackingStoreForDomain(domainUid01);
		boolean isDataStream = !esClient.indices()
				.resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid)).dataStreams().isEmpty();
		boolean isDataStream01 = !esClient.indices()
				.resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid01)).dataStreams().isEmpty();
		assertTrue(isDataStream);
		assertTrue(isDataStream01);

		dataStreamActivator.removeAuditBackingStoreForDomain(domainUid);
		isDataStream = !esClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid))
				.dataStreams().isEmpty();
		assertFalse(isDataStream);
		assertTrue(isDataStream01);
	}

	@Test
	public void removeDataStreamForName() throws AuditLogCreationException, IOException {
		dataStreamActivator = new DataStreamActivator();
		dataStreamActivator.setupAuditBackingStoreForDomain(domainUid);
		dataStreamActivator.setupAuditBackingStoreForDomain(domainUid01);
		boolean isDataStream = !esClient.indices()
				.resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid)).dataStreams().isEmpty();
		boolean isDataStream01 = !esClient.indices()
				.resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid01)).dataStreams().isEmpty();
		assertTrue(isDataStream);
		assertTrue(isDataStream01);

		dataStreamActivator.removeAuditBackingStore();
		isDataStream = !esClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid))
				.dataStreams().isEmpty();
		isDataStream01 = !esClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM_PREFIX + "_" + domainUid01))
				.dataStreams().isEmpty();
		assertFalse(isDataStream);
		assertFalse(isDataStream01);
	}
}
