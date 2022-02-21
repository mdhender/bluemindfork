/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.elasticsearch.client.transport.TransportClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.calendar.service.internal.CalendarService;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.persistence.UserSubscriptionStore;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;
import net.bluemind.videoconferencing.api.IVideoConferencing;
import net.bluemind.videoconferencing.api.VideoConferencingResourceDescriptor;
import net.bluemind.videoconferencing.saas.api.BlueMindVideoRoom;
import net.bluemind.videoconferencing.saas.api.IVideoConferencingSaas;

public class CalendarHookServiceTests {

	private static final String VIDEO_CONF_PROVIDER = "videoconferencing-bluemind";
	private static final String DOMAIN = "test.lan";
	protected SecurityContext defaultSecurityContext;
	protected SecurityContext anotherSecurityContext;
	protected Container container;
	protected TransportClient esearchClient;
	private String resUid;
	private DirEntry userDirEntry;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		PopulateHelper.createTestDomain(DOMAIN, esServer, imapServer);

		this.createCyrusPartition(imapServer, DOMAIN);

		String userUid = PopulateHelper.addUser("testuser", DOMAIN);
		userDirEntry = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, DOMAIN)
				.findByEntryUid(userUid);

		defaultSecurityContext = new SecurityContext("testuser", "testuser", Arrays.<String>asList(),
				Arrays.<String>asList(BasicRoles.ROLE_MANAGE_RESOURCE, "hasSimpleVideoconferencing"), DOMAIN);

		Sessions.get().put(defaultSecurityContext.getSessionId(), defaultSecurityContext);

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				defaultSecurityContext);

		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());

		container = Container.create(UUID.randomUUID().toString(), ICalendarUids.TYPE, "cal container",
				defaultSecurityContext.getSubject(), DOMAIN, true);
		container = containerStore.create(container);
		assertNotNull(container);

		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(SecurityContext.SYSTEM,
				JdbcTestHelper.getInstance().getDataSource(), containerStore.get(DOMAIN));

		userSubscriptionStore.subscribe(defaultSecurityContext.getSubject(), container);

		anotherSecurityContext = new SecurityContext(UUID.randomUUID().toString(), "another", Arrays.<String>asList(),
				Arrays.<String>asList(), "another.lan");

		Sessions.get().put(anotherSecurityContext.getSessionId(), anotherSecurityContext);

		esearchClient = ElasticsearchTestHelper.getInstance().getClient();

		resUid = UUID.randomUUID().toString();
		getVideoConfService(SecurityContext.SYSTEM).createResource(resUid, VideoConferencingResourceDescriptor.create(
				"visio", VIDEO_CONF_PROVIDER, Arrays.asList(AccessControlEntry.create(DOMAIN, Verb.Invitation))));

		IContainerManagement containerMgmtService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, resUid + "-settings-container");
		Map<String, String> settings = containerMgmtService.getSettings();
		settings.put("templates", "{\"name\":\"coucou\"}");
		containerMgmtService.setSettings(settings);

		setGlobalExternalUrl();

//		userCalendarContainer = createTestContainer(defaultSecurityContext, dataDataSource, ICalendarUids.TYPE,
//				"John Doe", ICalendarUids.TYPE + ":Default:" + testuser.uid, testuser.uid);

	}

	private void createCyrusPartition(final Server imapServer, final String domainUid) {
		final CyrusService cyrusService = new CyrusService(imapServer.ip);
		cyrusService.createPartition(domainUid);
		cyrusService.refreshPartitions(Arrays.asList(domainUid));
		new CyrusAdmins(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, "default"),
				imapServer.ip).write();
		cyrusService.reload();
	}

	@Test
	public void testDeleteVideoConfEvent() throws ServerFault, InterruptedException {
		VertxEventChecker<JsonObject> deletedMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_DELETED);

		ResourceDescriptor res = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IResources.class, DOMAIN).get(resUid);
		assertNotNull(res);
		assertEquals(IVideoConferenceUids.RESOURCETYPE_UID, res.typeIdentifier);

		VEventSeries event = defaultVEvent();
		// add visio
		Attendee videoconf = Attendee.create(CUType.Resource, "", Role.RequiredParticipant,
				ParticipationStatus.Accepted, true, resUid, "", "", "visioName",
				"bm://" + DOMAIN + "/resources/" + resUid, "", resUid, res.defaultEmailAddress());
		event.main.attendees.add(videoconf);

		String uid = "test_" + System.nanoTime();

		VEvent veventValue = getVideoConfService(defaultSecurityContext).add(event.main);
		assertNotNull(veventValue);

		getCalendarService(defaultSecurityContext, container).create(uid, event, true);

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, container).delete(uid, true);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<VEventSeries> vevent = getCalendarService(defaultSecurityContext, container).getComplete(uid);
		assertNotNull(vevent);
		String conferenceId = vevent.value.main.conferenceId;
		assertNotNull(conferenceId);
		BlueMindVideoRoom blueMindVideoRoom = getVideoConfSaasService().get(conferenceId);
		assertNotNull(blueMindVideoRoom);

		getCalendarService(defaultSecurityContext, container).delete(uid, true);

		Message<JsonObject> message = deletedMessageChecker.shouldSuccess();
		assertNotNull(message);

		vevent = getCalendarService(defaultSecurityContext, container).getComplete(uid);
		assertNull(vevent);

		boolean deleted = false;
		for (int i = 0; i < 10; i++) {
			blueMindVideoRoom = getVideoConfSaasService().get(conferenceId);
			if (blueMindVideoRoom == null) {
				deleted = true;
				break;
			}
			Thread.sleep(100);
		}

		assertTrue(deleted);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected ICalendar getCalendarService(SecurityContext context, Container container) throws ServerFault {
		BmContext ctx = new BmTestContext(context);
		DataSource ds = DataSourceRouter.get(ctx, container.uid);
		return new CalendarService(ds, esearchClient, container, ctx,
				CalendarAuditor.auditor(IAuditManager.instance(), ctx, container));
	}

	protected IVideoConferencingSaas getVideoConfSaasService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(IVideoConferencingSaas.class);
	}

	protected IVideoConferencing getVideoConfService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IVideoConferencing.class, DOMAIN);
	}

	private static Map<String, String> setGlobalExternalUrl() {
		ISystemConfiguration systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), "global.external.url");
		systemConfiguration.updateMutableValues(sysValues);
		return sysValues;
	}

	/**
	 * @return
	 */
	private VEventSeries defaultVEvent() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.now());
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;
		event.url = "https://www.bluemind.net";
		event.conference = "https//vi.sio.com/xxx";
		event.conferenceConfiguration.put("conf1", "val1");
		event.conferenceConfiguration.put("conf2", "val2");

		event.organizer = new VEvent.Organizer(null, "testuser@bm.lan");
		event.organizer.dir = "bm://" + userDirEntry.path;

		series.main = event;
		return series;
	}

}
