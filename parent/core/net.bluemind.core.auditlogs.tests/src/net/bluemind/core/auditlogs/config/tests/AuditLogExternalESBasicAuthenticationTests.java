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
package net.bluemind.core.auditlogs.config.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.attachment.api.AttachedFile;
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
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.AuditLogQuery;
import net.bluemind.core.auditlogs.api.ILogRequestService;
import net.bluemind.core.auditlogs.client.es.AudiLogEsClientActivator;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
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
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class AuditLogExternalESBasicAuthenticationTests {

	private static final String domainUid = "bm.lan";;
	private static final String AUDIT_LOG_DATASTREAM = AuditLogConfig.resolveDataStreamName(domainUid);
	private static final String ES_USER = "elastic";
	private static final String ES_PASSWORD = "DkIedPPSCb";

	private String datalocation;
	private DataSource dataDataSource;
	private DataSource systemDataSource;
	private static final String CONF_FILE_PATH = "/etc/bm/auditlog-store.conf";
	protected BmTestContext testContext = new BmTestContext(SecurityContext.SYSTEM);
	private Container domainContainer;
	private ItemValue<User> user01;
	private static final String CALENDAR_LOGTYPE = "calendar";
	private ItemValue<User> user02;
	private ItemValue<User> user03;
	private ContainerUserStoreService userStore;
	private SecurityContext user01SecurityContext;
	private SecurityContext user02SecurityContext;
	private SecurityContext user03SecurityContext;
	private Container user01CalendarContainer;
	private Container user02CalendarContainer;
	private Container user03CalendarContainer;
	private String uid01;
	private Organizer organizer01;
	private String uid02;
	private Organizer organizer02;
	private String uid03;
	private Organizer organizer03;
	private String uid04;
	private VEventSeries event01;
	private VEventSeries event02;
	private VEventSeries event03;
	private VEventSeries event04;
	private ElasticsearchClient esClient;
	protected boolean sendNotifications = false;
	private File confFile;
	private String externalEsAddress;
	private static ElasticContainer esContainer = new ElasticContainer(ES_USER, ES_PASSWORD);

	@BeforeClass
	public static void beforeClass() throws Exception {
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	@Before
	public void before() throws Exception {
		esContainer.start();
		externalEsAddress = esContainer.inspectAddress();

		AuditLogConfig.clear();
		confFile = new File(CONF_FILE_PATH);
		confFile.getParentFile().mkdirs();
		try (FileOutputStream fos = new FileOutputStream(confFile)) {
			String toWrite = String.format("""
					auditlog {
						activate=true
						store {
							type=elastic
							server=%s
							port=9200
							authentication {
								 	mode=basic
									user=%s
									password=%s
							}
						}
						}
					""", externalEsAddress, ES_USER, ES_PASSWORD);
			fos.write(toWrite.getBytes());
		}

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server nodeServer = new Server();
		nodeServer.ip = DockerEnv.getIp(DockerContainer.NODE.getName());
		nodeServer.tags = Lists.newArrayList(TagDescriptor.bm_filehosting.getTag());

		PopulateHelper.initGlobalVirt(esServer, nodeServer);

		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);
		systemDataSource = JdbcTestHelper.getInstance().getDataSource();

		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid);

		ContainerStore containerStore = new ContainerStore(testContext, systemDataSource, SecurityContext.SYSTEM);

		domainContainer = containerStore.get("bm.lan");
		assertNotNull(domainContainer);
		userStore = new ContainerUserStoreService(testContext, domainContainer, domain);

		// define users
		user01 = defaultUser("testUser01" + System.nanoTime(), "Test01", "User01");
		user02 = defaultUser("testUser02" + System.nanoTime(), "Test02", "User02");
		user03 = defaultUser("testUser03" + System.nanoTime(), "Test03", "User03");

		// Create users
		userStore.create(user01.uid, "testUser01", user01.value);
		userStore.create(user02.uid, "testUser02", user02.value);
		userStore.create(user03.uid, "testUser03", user03.value);

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

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			AuditLogQuery logQuery = new AuditLogQuery();
			logQuery.logtype = CALENDAR_LOGTYPE;
			logQuery.author = user01.value.defaultEmailAddress();
			logQuery.domainUid = domainUid;

			ILogRequestService logRequestService = getLogQueryService(user01SecurityContext);
			List<AuditLogEntry> list = logRequestService.queryAuditLog(logQuery);
			return 1 == list.size();
		});
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
		esContainer.stop();
		if (confFile.exists()) {
			confFile.delete();
		}
		AuditLogConfig.clear();
	}

	@Test
	public void testAuditLogStoreExternalES() throws Exception {
		assertTrue(AuditLogConfig.isActivated());

		AuditLogQuery logQuery = new AuditLogQuery();
		logQuery.logtype = CALENDAR_LOGTYPE;
		logQuery.domainUid = domainUid;

		Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> {
			ILogRequestService requestService = getLogQueryService(user01SecurityContext);
			List<AuditLogEntry> l = requestService.queryAuditLog(logQuery);
			return 4 == l.size();
		});

		ILogRequestService logRequestService = getLogQueryService(user01SecurityContext);
		List<AuditLogEntry> list = logRequestService.queryAuditLog(logQuery);
		assertEquals(4, list.size());
		assertEquals(event04.main.summary, list.get(0).content.description());
		assertEquals(event03.main.summary, list.get(1).content.description());
		assertEquals(event02.main.summary, list.get(2).content.description());
		assertEquals(event01.main.summary, list.get(3).content.description());

		ElasticsearchClient esClient = AudiLogEsClientActivator.get();

		ElasticsearchClient esInternalClient = ESearchActivator.getClient();
		boolean isExternalESDataStream = !esClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM))
				.dataStreams().isEmpty();
		boolean isInternalESDataStream = !esInternalClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM))
				.dataStreams().isEmpty();
		esClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM)).dataStreams()
				.forEach(d -> System.err.println("external datastream:" + d));
		esInternalClient.indices().resolveIndex(r -> r.name(AUDIT_LOG_DATASTREAM)).dataStreams()
				.forEach(d -> System.err.println("internal datastream:" + d));
		assertTrue(isExternalESDataStream);
		assertFalse(isInternalESDataStream);
	}

	protected ItemValue<User> defaultUser(String login, String lastname, String firstname) {
		net.bluemind.user.api.User user = new User();
		login = login.toLowerCase();
		user.login = login;
		Email em = new Email();
		em.address = login + "@bm.lan";
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		VCard card = new VCard();
		card.identification.name = Name.create(lastname, firstname, null, null, null, null);
		card.identification.formatedName = VCard.Identification.FormatedName.create(firstname + " " + lastname,
				Arrays.<VCard.Parameter>asList());
		user.contactInfos = card;
		ItemValue<User> ret = ItemValue.create(login + "_bm.lan", user);
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

	protected VEventSeries createVevent(String summary, Organizer organizer, List<VEvent.Attendee> attendees) {
		ZoneId tz = ZoneId.of("Europe/Paris");
		return defaultVEvent(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz), summary, organizer, attendees);
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

	private ILogRequestService getLogQueryService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(ILogRequestService.class);
	}

}
