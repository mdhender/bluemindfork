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
package net.bluemind.core.logs.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.LogMailQuery;
import net.bluemind.core.auditlogs.api.ILogRequestService;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;

public class LogServiceQueryTests {
	private String domainUid;
	private String user1;
	private SecurityContext userSecurityContext1;
	private String datalocation;
	private DataSource dataDataSource;
	private Container container;
	private String partition;
	private String mboxUniqueId;
	private static final AtomicReference<Long> timerId = new AtomicReference<>();

	@AfterClass
	public static void afterClass() {
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);

		ElasticsearchTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer, pipo);

		partition = CyrusPartition.forServerAndDomain(pipo.ip, domainUid).name;
		PopulateHelper.addDomain(domainUid);

		user1 = PopulateHelper.addUserWithRoles("user1", domainUid, BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT);

		userSecurityContext1 = BmTestContext.contextWithSession("sid1" + System.currentTimeMillis(), "user1", domainUid,
				BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT).getSecurityContext();

		container = createTestContainer();
		IMailboxFolders mailboxFolderService = ServerSideServiceProvider.getProvider(userSecurityContext1)
				.instance(IMailboxFolders.class, partition, "user." + user1.replace(".", "^"));
		ItemValue<MailboxFolder> folder = mailboxFolderService.byName("INBOX");
		mboxUniqueId = folder.uid;

		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		getTodoService(userSecurityContext1).create(uid, todo);

		todo.summary = "Coucou";

		getTodoService(userSecurityContext1).update(uid, todo);

		createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");
		ESearchActivator.refreshIndex("audit_log");

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			LogMailQuery logQuery = new LogMailQuery();
			logQuery.logtype = "mailbox_records";
			logQuery.author = "user1@devenv.net";

			ILogRequestService logRequestService = getLogQueryService(userSecurityContext1);
			List<AuditLogEntry> list = logRequestService.queryMailLog(logQuery);
			return 2 == list.size();

		});
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSQueryAllEmailsFromUser1() throws InterruptedException {

		LogMailQuery logQuery = new LogMailQuery();
		logQuery.logtype = "mailbox_records";
		logQuery.author = "user1@devenv.net";

		ILogRequestService logRequestService = getLogQueryService(userSecurityContext1);
		List<AuditLogEntry> list = logRequestService.queryMailLog(logQuery);
		assertEquals(2, list.size());
	}

	@Test
	public void testSQueryAllEmailsToUser2() throws InterruptedException {
		LogMailQuery logQuery = new LogMailQuery();
		logQuery.with = "user1@devenv.net";
		ILogRequestService logRequestService = getLogQueryService(userSecurityContext1);
		List<AuditLogEntry> list = logRequestService.queryMailLog(logQuery);
		assertEquals(10, list.size());
	}

	@Test
	public void testSQueryAllEmailsFromUser2() throws InterruptedException {
		LogMailQuery logQuery = new LogMailQuery();
		logQuery.author = "user2@devenv.net";
		ILogRequestService logRequestService = getLogQueryService(userSecurityContext1);
		List<AuditLogEntry> list = logRequestService.queryMailLog(logQuery);
		assertEquals(1, list.size());
	}

	protected VTodo defaultVTodo() {
		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		ZonedDateTime temp = ZonedDateTime.of(2024, 12, 28, 0, 0, 0, 0, ZoneId.of("UTC"));
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "Test Todo";
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.priority = 3;

		todo.organizer = new VTodo.Organizer("mehdi@bm.lan");

		List<VTodo.Attendee> attendees = new ArrayList<>(2);

		VTodo.Attendee john = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.Chair,
				VTodo.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "", "", "uid1", "john.bang@bm.lan");
		attendees.add(john);

		VTodo.Attendee jane = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.RequiredParticipant,
				VTodo.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", "uid2",
				"jane.bang@bm.lan");
		attendees.add(jane);

		todo.attendees = attendees;

		todo.attendees = attendees;

		return todo;
	}

	protected Container createTestContainer() throws SQLException {
		ContainerStore containerHome = new ContainerStore(new BmTestContext(userSecurityContext1), dataDataSource,
				userSecurityContext1);

		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, ITodoUids.TYPE, "test", user1, domainUid);
		container = containerHome.create(container);
		assertNotNull(container);

		containerHome = new ContainerStore(new BmTestContext(userSecurityContext1),
				JdbcActivator.getInstance().getDataSource(), userSecurityContext1);
		containerHome.createOrUpdateContainerLocation(container, datalocation);

		return container;
	}

	private ItemValue<MailboxRecord> createBodyAndRecord(int imapUid, Date internalDate, String eml) {
		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource(eml);
		Stream bmStream = VertxStream.stream(emlReadStream);

		String bodyUid = randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords records = getMailboxRecordService(SecurityContext.SYSTEM);
		assertNotNull(records);
		MailboxRecord record = new MailboxRecord();
		record.imapUid = imapUid;
		record.internalDate = internalDate;
		record.lastUpdated = record.internalDate;
		record.messageBody = bodyUid;
		String mailUid = "uid." + imapUid;
		records.create(mailUid, record);

		return records.getComplete(mailUid);
	}

	private static String randomGuid() {
		Random r = new Random();
		String left = UUID.randomUUID().toString().replace("-", "");
		String right = Strings.padStart(Integer.toHexString(r.nextInt()), 8, '0');
		return left + right;
	}

	private Date adaptDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return Date.from(adapted.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

	private ReadStream<Buffer> openResource(String path) {
		InputStream inputStream = LogServiceQueryTests.class.getClassLoader().getResourceAsStream(path);
		Objects.requireNonNull(inputStream, "Failed to open resource @ " + path);
		return new InputReadStream(inputStream);
	}

	private ITodoList getTodoService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, container.uid);
	}

	private IDbMailboxRecords getMailboxRecordService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMailboxRecords.class, mboxUniqueId);
	}

	private ILogRequestService getLogQueryService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(ILogRequestService.class);
	}

}
