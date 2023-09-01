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

package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AclAuditLogServiceTests {

	private Container container;
	private AclService aclService;
	private ElasticsearchClient esClient;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		String domainUid = "bm.lan";
		PopulateHelper.addDomain(domainUid);

		SecurityContext defaultSecurityContext = BmTestContext.contextWithSession("testUser", "test", domainUid)
				.getSecurityContext();
		BmContext context = new BmTestContext(defaultSecurityContext);

		ContainerStore containerHome = new ContainerStore(context, JdbcTestHelper.getInstance().getDataSource(),
				defaultSecurityContext);

		String containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		BaseContainerDescriptor desc = BaseContainerDescriptor.create(container.uid, container.type, container.name,
				container.owner, container.domainUid, container.defaultContainer);
		desc.internalId = container.id;
		aclService = new AclService(context, context.getSecurityContext(), context.getDataSource(), desc);
		esClient = ESearchActivator.getClient();

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void storeAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		AccessControlEntry test = AccessControlEntry.create("test", Verb.All);
		AccessControlEntry toto = AccessControlEntry.create("toto", Verb.Read);
		BaseContainerDescriptor desc = BaseContainerDescriptor.create(container.uid, container.type, container.name,
				container.owner, container.domainUid, container.defaultContainer);
		desc.internalId = container.id;
		aclService.store(Arrays.asList(test, toto));
		List<AccessControlEntry> actual = aclService.get();
		assertEquals(2, actual.size());
		assertTrue(actual.contains(test));
		assertTrue(actual.contains(toto));

		ESearchActivator.refreshIndex("audit_log");
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(AccessControlEntry.class.getSimpleName()))
								._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());
		assertTrue(response.hits().hits().stream().anyMatch(h -> h.source().content.key().equals("test")));
		assertTrue(response.hits().hits().stream().anyMatch(h -> h.source().content.key().equals("toto")));
		assertTrue(response.hits().hits().stream().filter(h -> h.source().content.key().equals("toto")).findFirst()
				.get().source().content.description().equals(Verb.Read.name()));

	}

	@Test
	public void storeAndRemoveAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		AccessControlEntry test = AccessControlEntry.create("test", Verb.All);
		AccessControlEntry toto = AccessControlEntry.create("toto", Verb.Read);
		BaseContainerDescriptor desc = BaseContainerDescriptor.create(container.uid, container.type, container.name,
				container.owner, container.domainUid, container.defaultContainer);
		desc.internalId = container.id;
		aclService.store(Arrays.asList(test, toto));
		List<AccessControlEntry> actual = aclService.get();
		aclService.deleteAll();
		assertEquals(2, actual.size());
		assertTrue(actual.contains(test));
		assertTrue(actual.contains(toto));

		ESearchActivator.refreshIndex("audit_log");
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(AccessControlEntry.class.getSimpleName()))
								._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(AccessControlEntry.class.getSimpleName()))
								._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());

	}

	@Test
	public void addAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		AccessControlEntry test = AccessControlEntry.create("test", Verb.All);
		AccessControlEntry toto = AccessControlEntry.create("toto", Verb.Read);
		BaseContainerDescriptor desc = BaseContainerDescriptor.create(container.uid, container.type, container.name,
				container.owner, container.domainUid, container.defaultContainer);
		desc.internalId = container.id;
		aclService.store(Arrays.asList(test, toto));
		List<AccessControlEntry> actual = aclService.get();
		assertEquals(2, actual.size());
		assertTrue(actual.contains(test));
		assertTrue(actual.contains(toto));

		ESearchActivator.refreshIndex("audit_log");
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(AccessControlEntry.class.getSimpleName()))
								._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());
	}
}
