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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;

public class AclAuditLogServiceTests {

	private AclService aclService;
	private ElasticsearchClient esClient;
	private String domainUid = "bm.lan";
	private ItemValue<User> user01;
	private SecurityContext user01SecurityContext;
	private String datalocation;
	private DataSource dataDataSource;
	private DataSource systemDataSource;
	private Container user01CalendarContainer;
	private BaseContainerDescriptor user01CalendarDesc;
	private ItemValue<User> user02;
	private ItemValue<User> user03;
	private static final String ACL_AUDITLOG_TYPE = "containeracl";
	private static final String AUDIT_LOG_DATASTREAM = "audit_log";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, pipo);

		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);
		systemDataSource = JdbcTestHelper.getInstance().getDataSource();
		PopulateHelper.addDomain(domainUid);

		SecurityContext defaultSecurityContext = BmTestContext.contextWithSession("testUser", "test", domainUid)
				.getSecurityContext();
		BmContext context = new BmTestContext(defaultSecurityContext);

		// define users
		user01 = defaultUser("testUser01_" + System.nanoTime(), "Test01", "User01");
		user02 = defaultUser("testUser02_" + System.nanoTime(), "Test02", "User02");
		user03 = defaultUser("testUser03_" + System.nanoTime(), "Test03", "User03");

		// Create users
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		userService.create(user01.uid, user01.value);
		userService.create(user02.uid, user02.value);
		userService.create(user03.uid, user03.value);

		// Create security contexts and sessions
		user01SecurityContext = new SecurityContext("user01", user01.uid, Arrays.<String>asList(),
				Arrays.<String>asList("hasSimpleVideoconferencing"), domainUid);
		Sessions.get().put(user01SecurityContext.getSessionId(), user01SecurityContext);

		// Create containers
		user01CalendarContainer = createTestContainer(user01SecurityContext, dataDataSource, ICalendarUids.TYPE,
				"User01Calendar", ICalendarUids.TYPE + ":Default:" + user01.uid, user01.uid);
		user01CalendarDesc = BaseContainerDescriptor.create(user01CalendarContainer.uid, user01CalendarContainer.name,
				user01CalendarContainer.owner, user01CalendarContainer.type, user01CalendarContainer.domainUid,
				user01CalendarContainer.defaultContainer);
		user01CalendarDesc.internalId = user01CalendarContainer.id;
		aclService = new AclService(context, context.getSecurityContext(), context.getDataSource(), user01CalendarDesc);
		esClient = ESearchActivator.getClient();
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void storeAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		AccessControlEntry user01AclEntry = AccessControlEntry.create(user01.uid, Verb.All);
		AccessControlEntry user02AclEntry = AccessControlEntry.create(user02.uid, Verb.Read);
		aclService.store(Arrays.asList(user01AclEntry, user02AclEntry));
		List<AccessControlEntry> actual = aclService.get();
		assertEquals(2, actual.size());
		assertTrue(actual.contains(user01AclEntry));
		assertTrue(actual.contains(user02AclEntry));

		ESearchActivator.refreshIndex(AUDIT_LOG_DATASTREAM);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(AUDIT_LOG_DATASTREAM) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return response.hits().total().value() == 2;
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(AUDIT_LOG_DATASTREAM) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());
		assertTrue(response.hits().hits().stream().anyMatch(h -> h.source().content.key().equals(user01.uid)));
		assertTrue(response.hits().hits().stream().anyMatch(h -> h.source().content.key().equals(user02.uid)));

		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		AuditLogEntry secondEntry = response.hits().hits().get(1).source();

		assertEquals("test", firstEntry.securityContext.uid());
		assertEquals("test", firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());
		assertEquals(user01CalendarDesc.name, firstEntry.container.name());
		assertTrue(firstEntry.item == null);
		assertEquals(2L, firstEntry.content.with().size());
		assertTrue(firstEntry.content.with().contains(user01.value.defaultEmailAddress()));
		assertEquals(1L, firstEntry.content.author().size());
		assertTrue(firstEntry.content.newValue() != null);
		assertTrue(firstEntry.content.description() != null);

		assertEquals("test", secondEntry.securityContext.uid());
		assertEquals("test", secondEntry.securityContext.displayName());
		assertEquals("unknown-origin", secondEntry.securityContext.origin());
		assertEquals(user01CalendarDesc.name, secondEntry.container.name());
		assertTrue(secondEntry.item == null);
		assertEquals(2L, secondEntry.content.with().size());
		assertTrue(secondEntry.content.with().contains(user01.value.defaultEmailAddress()));
		assertTrue(secondEntry.content.with().contains(user02.value.defaultEmailAddress()));
		assertEquals(1L, secondEntry.content.author().size());
		assertTrue(secondEntry.content.newValue() != null);
		assertTrue(secondEntry.content.description() != null);
	}

	@Test
	public void storeAndRemoveAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		AccessControlEntry user01AclEntry = AccessControlEntry.create(user01.uid, Verb.All);
		AccessControlEntry user02AclEntry = AccessControlEntry.create(user02.uid, Verb.Read);
		aclService.store(Arrays.asList(user01AclEntry, user02AclEntry));
		List<AccessControlEntry> actual = aclService.get();
		aclService.deleteAll();
		assertEquals(2, actual.size());
		assertTrue(actual.contains(user01AclEntry));
		assertTrue(actual.contains(user02AclEntry));

		ESearchActivator.refreshIndex(AUDIT_LOG_DATASTREAM);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(AUDIT_LOG_DATASTREAM) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return response.hits().total().value() == 2;
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(AUDIT_LOG_DATASTREAM) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());

		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertEquals(2L, firstEntry.content.with().size());
		assertTrue(firstEntry.content.newValue() != null);

		assertEquals("test", firstEntry.securityContext.uid());
		assertEquals("test", firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());

		assertEquals(user01CalendarDesc.name, firstEntry.container.name());

		assertTrue(firstEntry.item == null);

		assertEquals(user01.uid, firstEntry.content.key());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertTrue(firstEntry.content.is().isEmpty());
		assertTrue(firstEntry.content.has().isEmpty());

	}

	@Test
	public void addAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		AccessControlEntry user01AclEntry = AccessControlEntry.create(user01.uid, Verb.All);
		AccessControlEntry user02AclEntry = AccessControlEntry.create(user02.uid, Verb.Read);
		aclService.store(Arrays.asList(user01AclEntry, user02AclEntry));
		List<AccessControlEntry> actual = aclService.get();
		assertEquals(2, actual.size());
		assertTrue(actual.contains(user01AclEntry));
		assertTrue(actual.contains(user02AclEntry));

		ESearchActivator.refreshIndex(AUDIT_LOG_DATASTREAM);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(AUDIT_LOG_DATASTREAM) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return response.hits().total().value() == 2;
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(AUDIT_LOG_DATASTREAM) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);

		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertEquals(2L, firstEntry.content.with().size());
		assertTrue(firstEntry.content.newValue() != null);
		assertEquals("test", firstEntry.securityContext.uid());
		assertEquals("test", firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());

		assertEquals(user01CalendarDesc.name, firstEntry.container.name());

		assertTrue(firstEntry.item == null);

		assertEquals(user01.uid, firstEntry.content.key());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertTrue(firstEntry.content.is().isEmpty());
		assertTrue(firstEntry.content.has().isEmpty());
	}

	@Test
	public void retrieveAndStoreAcl() throws ServerFault, SQLException, ElasticsearchException, IOException {
		AccessControlEntry user02Acl = AccessControlEntry.create(user02.uid, Verb.All);
		AccessControlEntry user03Acl = AccessControlEntry.create(user03.uid, Verb.Read);
		aclService.store(Arrays.asList(user02Acl, user03Acl));
		List<AccessControlEntry> actual = aclService.get();
		assertEquals(2, actual.size());
		assertTrue(actual.contains(user02Acl));
		assertTrue(actual.contains(user03Acl));
		aclService.retrieveAndStore(Arrays.asList(AccessControlEntry.create(user02.uid, Verb.Freebusy)));

		ESearchActivator.refreshIndex(AUDIT_LOG_DATASTREAM);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(AUDIT_LOG_DATASTREAM) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return response.hits().total().value() == 2;
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(AUDIT_LOG_DATASTREAM) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);

		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertEquals(2L, firstEntry.content.with().size());
		assertTrue(firstEntry.content.newValue() != null);

		assertEquals("test", firstEntry.securityContext.uid());
		assertEquals("test", firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());

		assertEquals(user01CalendarDesc.name, firstEntry.container.name());

		assertTrue(firstEntry.item == null);

		assertEquals(user02Acl.subject, firstEntry.content.key());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertTrue(firstEntry.content.is().isEmpty());
		assertTrue(firstEntry.content.has().isEmpty());

		response = esClient.search(s -> s //
				.index(AUDIT_LOG_DATASTREAM) //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(user01CalendarDesc.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(ACL_AUDITLOG_TYPE))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(2L, response.hits().total().value());
	}

	private ItemValue<User> defaultUser(String login, String lastname, String firstname) {
		net.bluemind.user.api.User user = new User();
		login = login.toLowerCase();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = login;
		user.routing = Routing.internal;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		VCard card = new VCard();
		card.identification.name = Name.create(lastname, firstname, null, null, null, null);
		card.identification.formatedName = VCard.Identification.FormatedName.create(firstname + " " + lastname,
				Arrays.<VCard.Parameter>asList());
		user.contactInfos = card;
		ItemValue<User> ret = ItemValue.create(login + "_" + domainUid, user);
		ret.displayName = card.identification.formatedName.value;
		return ret;
	}

	private Container createTestContainer(SecurityContext context, DataSource datasource, String type, String name,
			String uid, String owner) throws SQLException {
		BmContext ctx = new BmTestContext(context);
		ContainerStore containerHome = new ContainerStore(ctx, datasource, context);
		Container container = Container.create(uid, type, name, owner, domainUid, true);
		container = containerHome.create(container);
		if (datasource != systemDataSource) {
			ContainerStore directoryStore = new ContainerStore(ctx, ctx.getDataSource(), context);
			directoryStore.createOrUpdateContainerLocation(container, datalocation);
		}
		IUserSubscription subApi = ctx.provider().instance(IUserSubscription.class, domainUid);
		subApi.subscribe(context.getSubject(), Arrays.asList(ContainerSubscription.create(container.uid, true)));
		return container;
	}
}
