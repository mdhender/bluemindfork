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
package net.bluemind.core.logs.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.domain.service.internal.DomainStoreService;
import net.bluemind.domain.service.tests.FakeDomainHook;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainsServiceAuditLogTests {

	private Container domainsContainer;
	private BmContext testContext;
	ElasticsearchClient esClient;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

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
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

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
	public void testCreateDomainMustHaveDatastream() throws ServerFault, ElasticsearchException, IOException {
		String domainUid = "test.lan";
		createDomain(domainUid);

		DomainStoreService domainStoreService = new DomainStoreService(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, domainsContainer);
		assertNotNull(domainStoreService.get(domainUid, null));

		assertTrue(FakeDomainHook.created);
		String AUDIT_LOG_DATASTREAM = AuditLogConfig.resolveDataStreamName(domainUid);
		boolean isDataStream = !esClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM)).dataStreams()
				.isEmpty();
		assertTrue(isDataStream);
	}

	@Test(timeout = 45000)
	public void testDelete() throws Exception {
		String domainUid = "test" + System.currentTimeMillis() + ".lan";
		PopulateHelper.createTestDomain(domainUid);

		try {
			getService().delete(domainUid);
			fail("should fail");
		} catch (ServerFault e) {
			// cant delete doamin because we need to delete dir entries
			// (addressbook)
		}

		// need to delete addressbook
		TaskRef taskRef = getService().deleteDomainItems(domainUid);

		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);

		assertTrue(status.state.succeed);

		// now we can delete domain
		getService().delete(domainUid);
		String AUDIT_LOG_DATASTREAM = AuditLogConfig.resolveDataStreamName(domainUid);
		boolean isDataStream = !esClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM)).dataStreams()
				.isEmpty();
		assertFalse(isDataStream);
	}

	private Domain domain(String name, Set<String> aliases) {
		return Domain.create(name, "label", "desc", aliases);
	}

	private Domain createDomain(String name) throws ServerFault {
		return createDomain(name, Collections.emptySet());
	}

	private Domain createDomain(String name, Set<String> aliases) throws ServerFault {
		Domain d = domain(name, aliases);
		getService().create(d.name, d);
		return d;
	}

	private IDomains getService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
	}

}
