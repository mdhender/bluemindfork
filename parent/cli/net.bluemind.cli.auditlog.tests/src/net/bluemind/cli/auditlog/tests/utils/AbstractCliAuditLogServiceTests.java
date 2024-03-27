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
package net.bluemind.cli.auditlog.tests.utils;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.persistence.VEventSeriesStore;
import net.bluemind.calendar.service.internal.CalendarAuditLogMapper;
import net.bluemind.calendar.service.internal.CalendarService;
import net.bluemind.calendar.service.internal.VEventContainerStoreService;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ItemValueAuditLogService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;

public abstract class AbstractCliAuditLogServiceTests {

	protected static String domainUid = "bm.lan";;
	private String datalocation;
	private DataSource dataDataSource;
	private DataSource systemDataSource;
	protected BmTestContext testContext = new BmTestContext(SecurityContext.SYSTEM);
	private Container domainContainer;
	protected ItemValue<User> user01;
	protected ItemValue<User> user02;
	protected ItemValue<User> user03;
	private ElasticsearchClient esClient;
	protected boolean sendNotifications = false;
	protected Container user01CalendarContainer;
	protected SecurityContext user01SecurityContext;
	private SecurityContext user02SecurityContext;
	private SecurityContext user03SecurityContext;
	private Container user02CalendarContainer;
	private Container user03CalendarContainer;
	private Organizer organizer01;
	private Organizer organizer02;
	private Organizer organizer03;
	private String uid01;
	private String uid02;
	private String uid03;
	protected VEventSeries event01;
	protected VEventSeries event02;
	protected VEventSeries event03;
	protected VEventSeries event04;
	private String uid04;
	private String partition;
	private String mboxUniqueId;

	@Before
	public void before() throws Exception {

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());

		partition = CyrusPartition.forServerAndDomain(pipo.ip, domainUid).name;
		PopulateHelper.initGlobalVirt(esServer, pipo);

		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);
		systemDataSource = JdbcTestHelper.getInstance().getDataSource();

		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid);

		ContainerStore containerStore = new ContainerStore(testContext, systemDataSource, SecurityContext.SYSTEM);

		domainContainer = containerStore.get(domainUid);
		assertNotNull(domainContainer);

		// define users
		user01 = defaultUser("testUser01" + System.nanoTime(), "Test01", "User01");
		user02 = defaultUser("testUser02" + System.nanoTime(), "Test02", "User02");
		user03 = defaultUser("testUser03" + System.nanoTime(), "Test03", "User03");

		// Create users
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.uid);
		userService.create(user01.uid, user01.value);
		userService.create(user02.uid, user02.value);
		userService.create(user03.uid, user03.value);

		// Create security contexts and sessions
		user01SecurityContext = new SecurityContext("user01", user01.uid, Arrays.<String>asList(),
				Arrays.<String>asList("hasSimpleVideoconferencing"), domainUid);
		Sessions.get().put(user01SecurityContext.getSessionId(), user01SecurityContext);
		user02SecurityContext = new SecurityContext("user02", user02.uid, Arrays.<String>asList(),
				Arrays.<String>asList("hasSimpleVideoconferencing"), domainUid);
		Sessions.get().put(user02SecurityContext.getSessionId(), user02SecurityContext);
		user03SecurityContext = new SecurityContext("user03", user03.uid, Arrays.<String>asList(),
				Arrays.<String>asList("hasSimpleVideoconferencing"), domainUid);
		Sessions.get().put(user03SecurityContext.getSessionId(), user03SecurityContext);

		// Create containers
		user01CalendarContainer = createTestContainer(user01SecurityContext, dataDataSource, ICalendarUids.TYPE,
				"User01Calendar", ICalendarUids.TYPE + ":Default:" + user01.uid, user01.uid);
		user02CalendarContainer = createTestContainer(user02SecurityContext, dataDataSource, ICalendarUids.TYPE,
				"User02Calendar", ICalendarUids.TYPE + ":Default:" + user02.uid, user02.uid);
		user03CalendarContainer = createTestContainer(user03SecurityContext, dataDataSource, ICalendarUids.TYPE,
				"User03Calendar", ICalendarUids.TYPE + ":Default:" + user03.uid, user03.uid);

		// Define organizers
		uid01 = "test01_" + System.nanoTime();
		organizer01 = new VEvent.Organizer(user01.value.login + "@" + domainUid);
		uid02 = "test02_" + System.nanoTime();
		organizer02 = new VEvent.Organizer(user02.value.login + "@" + domainUid);
		uid03 = "test03_" + System.nanoTime();
		organizer03 = new VEvent.Organizer(user03.value.login + "@" + domainUid);
		uid04 = "test04_" + System.nanoTime();

		// Define attendees
		VEvent.Attendee attendeeUser01 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "osef", null, null, null,
				user01.value.defaultEmail().address);
		VEvent.Attendee attendeeUser02 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				user02.value.defaultEmail().address);
		VEvent.Attendee attendeeUser03 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Declined, true, "", "", "", "osef", null, null, null,
				user03.value.defaultEmail().address);

		// Create Events
		event01 = createVevent("First Meeting", organizer01, Arrays.asList(attendeeUser02, attendeeUser03));
		event02 = createVevent("Second Meeting", organizer02, Arrays.asList(attendeeUser01, attendeeUser03));
		event03 = createVevent("Thrid Meeting", organizer03, Arrays.asList(attendeeUser01));
		event04 = createVevent("Private Event", organizer03, Arrays.asList());

		esClient = ElasticsearchTestHelper.getInstance().getClient();

		// Create events
		getCalendarService(user01SecurityContext, user01CalendarContainer).create(uid01, event01, sendNotifications);
		getCalendarService(user02SecurityContext, user02CalendarContainer).create(uid02, event02, sendNotifications);
		getCalendarService(user03SecurityContext, user03CalendarContainer).create(uid03, event03, sendNotifications);
		getCalendarService(user03SecurityContext, user03CalendarContainer).create(uid04, event04, sendNotifications);

		IMailboxFolders mailboxFolderService = ServerSideServiceProvider.getProvider(user01SecurityContext)
				.instance(IMailboxFolders.class, partition, "user." + user01.value.login.replace(".", "^"));
		ItemValue<MailboxFolder> folder = mailboxFolderService.byName("INBOX");
		mboxUniqueId = folder.uid;

		createBodyAndRecord(1, adaptDate(5), "data/sort_1.eml");
		createBodyAndRecord(2, adaptDate(10), "data/sort_2.eml");
		createBodyAndRecord(3, adaptDate(12), "data/sort_3.eml");

		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	protected ItemValue<User> defaultUser(String login, String lastname, String firstname) {
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

	protected Container createTestContainer(SecurityContext context, DataSource datasource, String type, String name,
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

	protected ICalendar getCalendarService(SecurityContext context, Container container) throws ServerFault {
		BmContext ctx = new BmTestContext(context);
		DataSource ds = DataSourceRouter.get(ctx, container.uid);
		VEventSeriesStore veventStore = new VEventSeriesStore(ds, container);

		BaseContainerDescriptor descriptor = BaseContainerDescriptor.create(container.uid, container.name,
				container.owner, container.type, container.domainUid, container.defaultContainer);
		descriptor.internalId = container.id;
		CalendarAuditLogMapper mapper = new CalendarAuditLogMapper();
		ItemValueAuditLogService<VEventSeries> calendarLogService = new ItemValueAuditLogService<>(context, descriptor,
				mapper);

		VEventContainerStoreService storeService = new VEventContainerStoreService(ctx, ds, context, container,
				veventStore, calendarLogService);
		return new CalendarService(ds, esClient, container, ctx,
				CalendarAuditor.auditor(IAuditManager.instance(), ctx, container), storeService);
	}

	protected VEventSeries createVevent(String summary, Organizer organizer, List<VEvent.Attendee> attendees) {
		ZoneId tz = ZoneId.of("Europe/Paris");
		return defaultVEvent(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz), summary, organizer, attendees);
	}

	/**
	 * @return
	 */
	protected VEventSeries defaultVEvent(ZonedDateTime start, String summary, Organizer organizer,
			List<VEvent.Attendee> attendees) {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(start);
		event.summary = summary;
		event.location = "Toulouse";
		event.description = "une description";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;
		event.url = "https://www.bluemind.net";
		event.conference = "https//vi.sio.com/xxx";
		event.conferenceConfiguration.put("conf1", "val1");
		event.conferenceConfiguration.put("conf2", "val2");

		event.attachments = new ArrayList<>();
		AttachedFile attachment1 = new AttachedFile();
		attachment1.publicUrl = "http://somewhere/1";
		attachment1.name = "test.gif";
		attachment1.cid = "cid0123456789";
		event.attachments.add(attachment1);
		AttachedFile attachment2 = new AttachedFile();
		attachment2.publicUrl = "http://somewhere/2";
		attachment2.name = "test.png";
		event.attachments.add(attachment2);

		event.organizer = organizer;
		event.attendees = attendees;

		VAlarm vAlarm = new VAlarm();
		vAlarm.action = VAlarm.Action.Email;
		vAlarm.description = "description";
		vAlarm.duration = 3600;
		vAlarm.repeat = 10;
		vAlarm.summary = "summary";
		vAlarm.trigger = 5;
		event.alarm = new ArrayList<>();
		event.alarm.add(vAlarm);

		series.main = event;
		return series;
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

	private Date adaptDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return Date.from(adapted.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

	private IDbMailboxRecords getMailboxRecordService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMailboxRecords.class, mboxUniqueId);
	}

	private ReadStream<Buffer> openResource(String path) {
		InputStream inputStream = AbstractCliAuditLogServiceTests.class.getClassLoader().getResourceAsStream(path);
		Objects.requireNonNull(inputStream, "Failed to open resource @ " + path);
		return new InputReadStream(inputStream);
	}

	private static String randomGuid() {
		Random r = new Random();
		String left = UUID.randomUUID().toString().replace("-", "");
		String right = Strings.padStart(Integer.toHexString(r.nextInt()), 8, '0');
		return left + right;
	}

}
