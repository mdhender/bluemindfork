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
package net.bluemind.authentication.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.authentication.api.APIKey;
import net.bluemind.authentication.api.IAPIKeys;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogStoreConfig;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AuthenticationAuditLogTests {

	private ElasticsearchClient esClient;
	private static final String domainUid = "bm.lan";
	private static final String domainGlobalUid = "global.virt";

	private static final String AUDIT_LOG_NAME_BM_LAN = AuditLogStoreConfig.resolveDataStreamName(domainUid);
	private static final String AUDIT_LOG_NAME_GLOBAL = AuditLogStoreConfig.resolveDataStreamName(domainGlobalUid);

	private static final String TEST_API_KEY = "testApiKey";

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = new BmConfIni().get("es-host");
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		IDomainSettings settings0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainGlobalUid);
		Map<String, String> domainSettings0 = settings0.get();
		domainSettings0.put(DomainSettingsKeys.mail_routing_relay.name(), "external@test.fr");
		settings0.set(domainSettings0);

		PopulateHelper.addDomainAdmin("admin0", domainGlobalUid, Routing.external);

		PopulateHelper.createTestDomain(domainUid, esServer);
		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> domainSettings = settings.get();
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "external@test.fr");
		domainSettings.put(DomainSettingsKeys.domain_max_basic_account.name(), "");
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		settings.set(domainSettings);
		PopulateHelper.addDomainAdmin("admin", domainUid, Routing.external);
		PopulateHelper.addUser("toto", domainUid, Routing.external);
		PopulateHelper.addUser("archived", domainUid, Routing.external);
		PopulateHelper.addUser("nomail", domainUid, Routing.none);
		PopulateHelper.addSimpleUser("simple", domainUid, Routing.external);
		createUserWithEpiredPassword();

		StateContext.setState("reset");
		StateContext.setState("core.started");

		esClient = ESearchActivator.getClient();
	}

	private void createUserWithEpiredPassword() throws SQLException {
		PopulateHelper.addUser("expiredpassword", domainUid, Routing.external);

		Connection conn = JdbcTestHelper.getInstance().getDataSource().getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(
					"UPDATE t_domain_user SET password_lastchange=now() - interval '10 year' WHERE login='expiredpassword'");
			st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);
		}
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void testAuditLogStandardLogin() throws ElasticsearchException, IOException {
		initState();

		IAuthentication authentication = getService(null);

		LoginResponse response = authentication.login("admin0@" + domainGlobalUid, "admin", "junit");
		assertEquals(Status.Ok, response.status);
		assertNotNull(response.authKey);

		String authKey = response.authKey;

		response = authentication.login("admin0@" + domainGlobalUid, authKey, "auth-key");
		assertEquals(Status.Ok, response.status);
		assertEquals(authKey, response.authKey);

		response = authentication.login("nomail@" + domainUid, "nomail", "junit");
		assertEquals(Status.Ok, response.status);

		response = authentication.login("expiredpassword@" + domainUid, "expiredpassword", "junit");
		assertEquals(Status.Expired, response.status);

		response = authentication.login("expiredpassword@" + domainUid, "badexpiredpassword", "junit");
		assertEquals(Status.Bad, response.status);

		response = authentication.login("admin0@" + domainGlobalUid, "not_valid", "invalid-junit");
		assertEquals(Status.Bad, response.status);

		ESearchActivator.refreshIndex(AUDIT_LOG_NAME_BM_LAN);
		ESearchActivator.refreshIndex(AUDIT_LOG_NAME_GLOBAL);

		Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
					.index(AUDIT_LOG_NAME_BM_LAN) //
					.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
					AuditLogEntry.class);
			return 2L == esResponse.hits().total().value();
		});
		Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
					.index(AUDIT_LOG_NAME_GLOBAL) //
					.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
					AuditLogEntry.class);
			return 2L == esResponse.hits().total().value();
		});

		SearchResponse<AuditLogEntry> esResponseGlobal = esClient.search(s -> s //
				.index(AUDIT_LOG_NAME_GLOBAL) //
				.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, esResponseGlobal.hits().total().value());
		List<String> loggedUserMailsGlobal = esResponseGlobal.hits().hits().stream()
				.map(h -> h.source().securityContext.email()).toList();
		assertTrue(loggedUserMailsGlobal.contains("admin0@" + domainGlobalUid));

		AuditLogEntry auditLogEntryBmGlobal = esResponseGlobal.hits().hits().get(0).source();
		assertEquals("admin0", auditLogEntryBmGlobal.securityContext.uid());
		assertEquals("admin0", auditLogEntryBmGlobal.securityContext.displayName());
		assertEquals("junit", auditLogEntryBmGlobal.securityContext.origin());
		assertEquals("admin0@" + domainGlobalUid, auditLogEntryBmGlobal.securityContext.email());

		assertTrue(auditLogEntryBmGlobal.container == null);
		assertTrue(auditLogEntryBmGlobal.item == null);
		assertTrue(auditLogEntryBmGlobal.content.with().contains("admin0@" + domainGlobalUid));

		SearchResponse<AuditLogEntry> esResponseBmLan = esClient.search(s -> s //
				.index(AUDIT_LOG_NAME_BM_LAN) //
				.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, esResponseBmLan.hits().total().value());
		List<String> loggedUserMailsBmLan = esResponseBmLan.hits().hits().stream()
				.map(h -> h.source().securityContext.email()).toList();
		assertTrue(loggedUserMailsBmLan.contains("nomail@" + domainUid));
		assertTrue(loggedUserMailsBmLan.contains("expiredpassword@" + domainUid));

		AuditLogEntry auditLogEntryBmLan = esResponseBmLan.hits().hits().get(0).source();
		assertEquals("nomail", auditLogEntryBmLan.securityContext.uid());
		assertEquals("nomail", auditLogEntryBmLan.securityContext.displayName());
		assertEquals("junit", auditLogEntryBmLan.securityContext.origin());
		assertEquals("nomail@" + domainUid, auditLogEntryBmLan.securityContext.email());

		assertTrue(auditLogEntryBmLan.container == null);
		assertTrue(auditLogEntryBmLan.item == null);
		assertTrue(auditLogEntryBmLan.content.with().contains("nomail@" + domainUid));
	}

	private void initState() {
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	private IAuthentication getService(String sessionId) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sessionId)
				.instance(IAuthentication.class);
	}

	@Test
	public void testAuditLogSu() throws ElasticsearchException, IOException {
		initState();
		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@" + domainGlobalUid, "admin", "junit");

		authentication = getService(response.authKey);

		response = authentication.su("admin0@" + domainGlobalUid);
		assertEquals(LoginResponse.Status.Ok, response.status);
		assertNotNull(response.authKey);

		ESearchActivator.refreshIndex(AUDIT_LOG_NAME_BM_LAN);
		ESearchActivator.refreshIndex(AUDIT_LOG_NAME_GLOBAL);

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
					.index(AUDIT_LOG_NAME_BM_LAN) //
					.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
					AuditLogEntry.class);
			return 0L == esResponse.hits().total().value();
		});
		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
					.index(AUDIT_LOG_NAME_GLOBAL) //
					.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
					AuditLogEntry.class);
			return 2L == esResponse.hits().total().value();
		});

		SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
				.index(AUDIT_LOG_NAME_GLOBAL) //
				.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, esResponse.hits().total().value());
		List<String> loggedUserMails = esResponse.hits().hits().stream().map(h -> h.source().securityContext.email())
				.toList();
		assertTrue(loggedUserMails.contains("admin0@" + domainGlobalUid));

		AuditLogEntry auditLogEntry = esResponse.hits().hits().get(0).source();
		assertEquals("admin0", auditLogEntry.securityContext.uid());
		assertEquals("admin0", auditLogEntry.securityContext.displayName());
		assertEquals("junit", auditLogEntry.securityContext.origin());
		assertEquals("admin0@" + domainGlobalUid, auditLogEntry.securityContext.email());

		assertNull(auditLogEntry.container);
		assertNull(auditLogEntry.item);
		assertTrue(auditLogEntry.content.with().contains("admin0"));
		assertTrue(auditLogEntry.content.with().contains("admin0@global.virt"));
	}

	@Test
	public void testAuditLogApiKey() throws ServerFault, IOException {
		initState();

		SecurityContext ctx = new SecurityContext(null, "admin0", Arrays.<String>asList(), Arrays.<String>asList(),
				domainGlobalUid);

		IAPIKeys service = ServerSideServiceProvider.getProvider(ctx).instance(IAPIKeys.class);

		APIKey key = service.create(TEST_API_KEY);

		assertNotNull(key);

		IAuthentication authentication = getService(null);
		LoginResponse response = authentication.login("admin0@" + domainGlobalUid, key.sid, TEST_API_KEY);
		assertEquals(Status.Ok, response.status);

		service.delete(key.sid);

		response = authentication.login("admin0@" + domainGlobalUid, key.sid, TEST_API_KEY);
		assertEquals(Status.Bad, response.status);

		ESearchActivator.refreshIndex(AUDIT_LOG_NAME_BM_LAN);
		ESearchActivator.refreshIndex(AUDIT_LOG_NAME_GLOBAL);

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
					.index(AUDIT_LOG_NAME_BM_LAN) //
					.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
					AuditLogEntry.class);
			return 0L == esResponse.hits().total().value();
		});

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
					.index(AUDIT_LOG_NAME_GLOBAL) //
					.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
					AuditLogEntry.class);
			return 1L == esResponse.hits().total().value();
		});

		SearchResponse<AuditLogEntry> esResponse = esClient.search(s -> s //
				.index(AUDIT_LOG_NAME_GLOBAL) //
				.query(q -> q.bool(b -> b.must(TermQuery.of(t -> t.field("logtype").value("login"))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, esResponse.hits().total().value());
		List<String> loggedUserMails = esResponse.hits().hits().stream().map(h -> h.source().securityContext.email())
				.toList();
		assertTrue(loggedUserMails.contains("admin0@" + domainGlobalUid));

		AuditLogEntry auditLogEntry = esResponse.hits().hits().get(0).source();
		assertEquals("admin0", auditLogEntry.securityContext.uid());
		assertEquals("admin0", auditLogEntry.securityContext.displayName());
		assertEquals(TEST_API_KEY, auditLogEntry.securityContext.origin());
		assertEquals("admin0@" + domainGlobalUid, auditLogEntry.securityContext.email());

		assertTrue(auditLogEntry.container == null);
		assertTrue(auditLogEntry.item == null);
		assertTrue(auditLogEntry.content.with().contains("admin0"));
		assertTrue(auditLogEntry.content.with().contains("admin0@global.virt"));
	}

}
