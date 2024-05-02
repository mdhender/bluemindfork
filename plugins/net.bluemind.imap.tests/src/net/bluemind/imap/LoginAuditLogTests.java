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
package net.bluemind.imap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LoginAuditLogTests {

	private final int PORT = 1143;
	private String loginUid;
	private static final String domainUid = "test.devenv";
	private static final String DATASTREAM_NAME = AuditLogConfig.resolveDataStreamName(domainUid);

	@AfterAll
	public static void afterClass() {
		System.clearProperty("retry.queue.no.debounce");
	}

	@BeforeAll
	public static void beforeClass() throws Exception {
		System.setProperty("retry.queue.no.debounce", "true");
	}

	@BeforeEach
	public void setUp() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		StateContext.setInternalState(new RunningState());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Lists.newArrayList(TagDescriptor.mail_imap.getTag());

		PopulateHelper.initGlobalVirt(pipo, esServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		loginUid = "user" + System.currentTimeMillis();
		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser(loginUid, domainUid);
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@AfterEach
	public void tearDown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSecurityContextOrigin() throws Exception {

		IAuthentication authService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAuthentication.class);

		LoginResponse resp = authService.login(loginUid + "@" + domainUid, loginUid, "testSIDLoginLogout");

		String sid = resp.authKey;
		assertNotNull(sid);

		try (StoreClient sc = new StoreClient("127.0.0.1", PORT, loginUid + "@" + domainUid, loginUid)) {
			boolean ok = sc.login();
			assertTrue(ok);
		} catch (Exception e) {
			e.printStackTrace();
			fail("error on login");
		} finally {
			JdbcTestHelper.getInstance().afterTest();
		}

		ElasticsearchClient esClient = ESearchActivator.getClient();
		ESearchActivator.refreshIndex(DATASTREAM_NAME);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> search(esClient).hits().total().value() > 0);
		SearchResponse<AuditLogEntry> response = search(esClient);

		assertEquals(2L, response.hits().total().value());
		List<String> origins = response.hits().hits().stream().map(h -> h.source().securityContext.origin()).toList();
		assertTrue(origins.contains("testSIDLoginLogout"));
		assertTrue(origins.contains("imap-endpoint"));
	}

	private SearchResponse<AuditLogEntry> search(ElasticsearchClient esClient) throws IOException {
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(DATASTREAM_NAME) //
				.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		return response;
	}

}
