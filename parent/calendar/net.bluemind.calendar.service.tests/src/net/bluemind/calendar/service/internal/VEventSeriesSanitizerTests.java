/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.service.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceDescriptor.PropertyValue;
import net.bluemind.resource.api.ResourceReservationMode;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.resource.helper.IResourceTemplateHelper;
import net.bluemind.resource.helper.ResourceTemplateHelpers;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.UserSubscriptionStore;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class VEventSeriesSanitizerTests {
	private static final String USER_UID_AND_LOGIN = "u1";
	private static final String TEMPLATE = "Hello! I am a template mate! ${Organizer} invites you to this wonderful event with the property ${MyCustomPropOne} and also ${MyCustomPropTwo} and the even better ${MyCustomPropThree} !!! How lucky you!\nThis entire ${line} should be removed since it contains ${unknown} variables.\nThis line should be kept.";
	private static final String RESOURCE_ID = "123-456-789";
	private static final String TRANSFORMED_TEMPLATE_SEPARATOR = "<br>\n";
	private static final String TRANSFORMED_TEMPLATE_SUFFIX = "<br><br>";
	private String transformedTemplate;
	private String domainUid;
	private ItemValue<Server> dataLocation;
	private ContainerStore containerHome;
	private ContainerUserStoreService userStoreService;
	private User user;
	private ServerSideServiceProvider provider;

	@SuppressWarnings("deprecation")
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

		this.domainUid = "test.lan";

		this.provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);
		this.dataLocation = this.provider.instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(imapServer.ip);

		this.containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		this.initDomain(dataLocation, imapServer);

		this.createCyrusPartition(imapServer, this.domainUid);

		final IResourceTemplateHelper resourceTemplateHelper = ResourceTemplateHelpers.getInstance();
		this.transformedTemplate = resourceTemplateHelper.tagBegin(RESOURCE_ID) + "\n"
				+ "FR Hello! I am a template mate! John Doe invites you to this wonderful event with the property My Custom Prop One Value and also My Custom Prop Two Value and the even better My Custom Prop Three Value !!! How lucky you!\nThis line should be kept."
				+ "\n" + resourceTemplateHelper.tagEnd();
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

	/**
	 * Test {@link ResourceDescriptionAdapterHook#onEventCreated(VEventMessage)} :
	 * create an event with a resource having a template.
	 */
	@Test
	public void onEventCreated() {
		// build the event and call ResourceDescriptionAdapterHook.onEventCreated
		final String description = "2 individuals attendees and 1 resource attendee having a template";
		final VEventMessage vEventMessage = this.eventCreation(description, true);

		// check the result - the processed template should have been added
		Assert.assertNotNull(vEventMessage.vevent);
		final List<VEvent> vEvents = vEventMessage.vevent.flatten();
		Assert.assertNotNull(vEvents);
		final String expectedResult = description + TRANSFORMED_TEMPLATE_SEPARATOR + transformedTemplate
				+ TRANSFORMED_TEMPLATE_SUFFIX;
		this.checkDescription(vEvents, expectedResult);
	}

	private VEventMessage eventCreation(final String description, final boolean withResource) {
		// build event
		final VEventMessage vEventMessage = this.buildEvent(description, withResource);

		// call sanitizer
		final Map<String, String> params = new HashMap<>(2);
		params.put("owner", vEventMessage.container.owner);
		params.put("domainUid", this.domainUid);
		new VEventSeriesSanitizer(new BmTestContext(Sessions.get().getIfPresent(user.login)))
				.create(vEventMessage.vevent, params);

		return vEventMessage;
	}

	/**
	 * Test {@link ResourceDescriptionAdapterHook#onEventUpdated(VEventMessage)} :
	 * delete a resource having a template.
	 */
	@Test
	public void onResourceDeleted() {
		// first, create an event with a resource
		final String oldDescription = "2 individuals attendees and 1 resource attendee having a template";
		final VEventMessage oldEventMessage = this.eventCreation(oldDescription, true);

		// build an event without a resource
		final String description = "delete 1 resource attendee having an already transformed template in the description";
		final VEventMessage vEventMessage = this.buildEvent(description + transformedTemplate, false);
		vEventMessage.oldEvent = oldEventMessage.vevent;

		// execute the code
		final Map<String, String> params = new HashMap<>(2);
		params.put("owner", vEventMessage.container.owner);
		params.put("domainUid", this.domainUid);
		new VEventSeriesSanitizer(new BmTestContext(Sessions.get().getIfPresent(user.login)))
				.update(vEventMessage.oldEvent, vEventMessage.vevent, params);

		// check the result - the processed template should have been removed
		Assert.assertNotNull(vEventMessage.vevent);
		final List<VEvent> vEvents = vEventMessage.vevent.flatten();
		Assert.assertNotNull(vEvents);
		final String expectedResult = description;
		this.checkDescription(vEvents, expectedResult);
	}

	/**
	 * Test {@link ResourceDescriptionAdapterHook#onEventUpdated(VEventMessage)} :
	 * add a resource having a template.
	 */
	@Test
	public void onResourceAdded() {
		// first, create an event without resource
		final String oldDescription = "2 individuals attendees";
		final VEventMessage oldEventMessage = this.eventCreation(oldDescription, false);

		// build event with resource
		final String description = "add 1 resource attendee having a template";
		final VEventMessage vEventMessage = this.buildEvent(description, true);
		vEventMessage.oldEvent = oldEventMessage.vevent;

		// execute the code
		final Map<String, String> params = new HashMap<>(2);
		params.put("owner", vEventMessage.container.owner);
		params.put("domainUid", this.domainUid);
		new VEventSeriesSanitizer(new BmTestContext(Sessions.get().getIfPresent(user.login)))
				.update(vEventMessage.oldEvent, vEventMessage.vevent, params);

		// check the result - the processed template should have been added
		Assert.assertNotNull(vEventMessage.vevent);
		final List<VEvent> vEvents = vEventMessage.vevent.flatten();
		Assert.assertNotNull(vEvents);
		final String expectedResult = description + TRANSFORMED_TEMPLATE_SEPARATOR + transformedTemplate
				+ TRANSFORMED_TEMPLATE_SUFFIX;
		this.checkDescription(vEvents, expectedResult);
	}

	/**
	 * Test {@link ResourceDescriptionAdapterHook#onEventUpdated(VEventMessage)} :
	 * create a resource already having the template in its description.
	 */
	@Test
	public void onEventCreatedAlreadyHavingTemplate() {
		// build the event and call
		// ResourceDescriptionAdapterHook.onEventCreated
		final String description = "a resource already having the template in its description" + transformedTemplate;
		final VEventMessage vEventMessage = this.eventCreation(description, true);

		// check the result - the processed template should not have been added
		// another time, i.e. only one occurrence of the processed template
		// should be present
		Assert.assertNotNull(vEventMessage.vevent);
		final List<VEvent> vEvents = vEventMessage.vevent.flatten();
		Assert.assertNotNull(vEvents);
		final String expectedResult = description;
		this.checkDescription(vEvents, expectedResult);
	}

	@Test
	public void draftCannotBeUpdateToTrue() throws ServerFault {
		ItemValue<VEventSeries> item = defaultVEvent("Summer y", "De scription");
		VEventSeries old = item.value.copy();
		item.value.main.draft = true;
		new VEventSeriesSanitizer(new BmTestContext(Sessions.get().getIfPresent(user.login))).update(old, item.value);
		assertFalse(item.value.main.draft);
	}

	@Test
	public void onEventCreatedUnknownResource() {
		final String subject = "onEventCreatedUnknownResource";
		final List<Attendee> attendees = new ArrayList<>(3);

		final Attendee toto = new Attendee();
		toto.commonName = "Toto toto";
		toto.cutype = CUType.Individual;
		toto.mailto = "toto" + System.nanoTime() + "@" + this.domainUid;
		attendees.add(toto);

		final Attendee res = new Attendee();
		res.commonName = "res" + UUID.randomUUID();
		res.cutype = CUType.Resource;
		res.mailto = "res" + System.nanoTime() + "@" + this.domainUid;
		attendees.add(res);

		VEventMessage vEventMessage = this.buildEvent(subject, "onEventCreatedUnknownResource desc", attendees);

		final Map<String, String> params = new HashMap<>(2);
		params.put("owner", vEventMessage.container.owner);
		params.put("domainUid", this.domainUid);
		new VEventSeriesSanitizer(new BmTestContext(Sessions.get().getIfPresent(user.login)))
				.create(vEventMessage.vevent, params);

	}

	private void checkDescription(final List<VEvent> vEvents, final String expectedResult) {
		vEvents.forEach(vEvent -> {
			Assert.assertNotNull(vEvent.description);
			Assert.assertEquals(expectedResult, vEvent.description);
		});
	}

	private VEventMessage buildEvent(final String description, final boolean withResource) {
		final String subject = "onResourceCreated";
		final List<Attendee> attendees = new ArrayList<>(3);

		final Attendee toto = new Attendee();
		toto.commonName = "Toto Matic";
		toto.cutype = CUType.Individual;
		toto.mailto = "toto@" + this.domainUid;
		attendees.add(toto);

		final Attendee georges = new Attendee();
		georges.commonName = "Georges Abitbol";
		georges.cutype = CUType.Individual;
		georges.mailto = "georges@" + this.domainUid;
		attendees.add(georges);

		if (withResource) {
			final Attendee visio = this.buildAndPersistResource();
			attendees.add(visio);
		}

		final VEventMessage vEventMessage = this.buildEvent(subject, description, attendees);

		return vEventMessage;
	}

	@SuppressWarnings("serial")
	private Attendee buildAndPersistResource() {
		final Attendee visio = new Attendee();
		visio.commonName = "Visio-conference";
		visio.cutype = CUType.Resource;
		final String resourceId = RESOURCE_ID;
		visio.mailto = resourceId + "@" + this.domainUid;
		visio.dir = "path/to/my/resource/" + resourceId;
		final String resourceTypeId = "visioTypeId";
		final String template = TEMPLATE;
		this.createResourceTypeWithTemplate(resourceTypeId, visio.commonName, new HashMap<String, String>() {
			{
				put("customProp1", "MyCustomPropOne");
				put("customProp2", "MyCustomPropTwo");
				put("customProp3", "MyCustomPropThree");
			}
		}, template);

		this.createResource(resourceId, resourceTypeId, visio.commonName, new HashMap<String, String>() {
			{
				put("customProp1", "My Custom Prop One Value");
				put("customProp2", "My Custom Prop Two Value");
				put("customProp3", "My Custom Prop Three Value");
			}
		});
		return visio;
	}

	private void initDomain(ItemValue<Server> dataLocation, Server... servers) throws Exception {
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid, servers);
		Container userContainer = containerHome.get(domainUid);
		this.userStoreService = new ContainerUserStoreService(new BmTestContext(SecurityContext.SYSTEM), userContainer,
				domain);
		Container mboxContainer = containerHome.get(domainUid);
		assertNotNull(mboxContainer);
		ItemValue<User> user1Item = createTestUser(dataLocation, USER_UID_AND_LOGIN);
		this.user = user1Item.value;
	}

	@SuppressWarnings("deprecation")
	private ItemValue<User> createTestUser(ItemValue<Server> dataLocation, String login)
			throws ServerFault, SQLException {
		ItemValue<User> user = defaultUser(dataLocation, login, login);
		userStoreService.create(user.uid, login, user.value);
		SecurityContext securityContext = new SecurityContext(login, login, new ArrayList<String>(),
				new ArrayList<String>(), domainUid);
		createTestContainer(securityContext, ICalendarUids.TYPE, user.value.login,
				ICalendarUids.defaultUserCalendar(user.uid), user.uid);
		Sessions.get().put(login, securityContext);
		return user;
	}

	@SuppressWarnings("deprecation")
	private void createTestContainer(SecurityContext context, String type, String login, String name, String owner)
			throws SQLException {
		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), context);
		Container container = Container.create(name, type, name, owner, this.domainUid, true);
		container = containerHome.create(container);
		Container dom = containerHome.get(domainUid);
		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(SecurityContext.SYSTEM,
				JdbcTestHelper.getInstance().getDataSource(), dom);
		userSubscriptionStore.subscribe(context.getSubject(), container);
	}

	private ItemValue<User> defaultUser(ItemValue<Server> dataLocation, String uid, String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@test.lan";
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.dataLocation = dataLocation.uid;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		card.identification.formatedName = FormatedName.create(login);
		user.contactInfos = card;
		return ItemValue.create(uid, user);
	}

	@SuppressWarnings("deprecation")
	private VEventMessage buildEvent(final String summary, final String description, final List<Attendee> attendees) {
		final VEventMessage veventMessage = new VEventMessage();

		final ItemValue<VEventSeries> event = defaultVEvent(summary, description);
		event.value.main.attendees = attendees;

		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = null;
		veventMessage.securityContext = SecurityContext.SYSTEM;
		veventMessage.sendNotifications = true;
		try {
			veventMessage.container = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
					SecurityContext.SYSTEM).get(ICalendarUids.defaultUserCalendar(user.login));
			Assert.assertNotNull(veventMessage.container);
			veventMessage.container.domainUid = this.domainUid;
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		return veventMessage;
	}

	private ItemValue<VEventSeries> defaultVEvent(final String summary, final String description) {
		final VEvent event = new VEvent();
		final ZoneId tz = ZoneId.of("Europe/Paris");

		final long now = System.currentTimeMillis();
		final long start = now + (1000 * 60 * 60);
		ZonedDateTime temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), tz);
		event.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);

		temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start + (1000 * 60 * 60)), tz);
		event.dtend = BmDateTimeWrapper.create(temp, Precision.DateTime);
		event.summary = summary;
		event.location = "Toulouse";
		event.description = description;
		event.priority = 1;
		event.organizer = new VEvent.Organizer("John Doe", this.user.defaultEmailAddress());
		event.organizer.dir = "bm://" + IDirEntryPath.path(domainUid, user.login, Kind.USER);
		event.attendees = new ArrayList<>();
		event.categories = new ArrayList<TagRef>(0);

		event.rdate = new HashSet<BmDateTime>();
		event.rdate.add(BmDateTimeWrapper.create(temp, Precision.Date));

		final VEventSeries series = new VEventSeries();
		series.main = event;

		return ItemValue.create(UUID.randomUUID().toString(), series);
	}

	private void createResourceTypeWithTemplate(final String resourceTypeId, final String label,
			final Map<String, String> propsLabels, String template) {
		final ResourceTypeDescriptor resourceTypeDescriptor = new ResourceTypeDescriptor();
		resourceTypeDescriptor.label = label;
		if (propsLabels != null) {
			resourceTypeDescriptor.properties = new ArrayList<>(propsLabels.size());
			propsLabels.entrySet().forEach(entry -> {
				final Property p = new Property();
				p.id = entry.getKey();
				p.label = "en::" + entry.getValue() + "\nfr::" + entry.getValue() + "Fr";
				p.type = Property.Type.String;
				resourceTypeDescriptor.properties.add(p);
			});
		}
		resourceTypeDescriptor.templates.put("fr", "FR " + template);
		resourceTypeDescriptor.templates.put("en", "EN " + template);

		this.provider.instance(IResourceTypes.class, domainUid).create(resourceTypeId, resourceTypeDescriptor);

	}

	private void createResource(final String resourceId, final String resourceTypeId, final String label,
			final Map<String, String> propsValues) {
		final ResourceDescriptor resourceDescriptor = new ResourceDescriptor();
		resourceDescriptor.typeIdentifier = resourceTypeId;
		resourceDescriptor.label = label;
		resourceDescriptor.description = "What a mighty description!";
		resourceDescriptor.reservationMode = ResourceReservationMode.OWNER_MANAGED;
		resourceDescriptor.dataLocation = this.dataLocation.uid;
		resourceDescriptor.emails = Collections
				.singletonList(Email.create(resourceId.toLowerCase() + "@test.lan", true));
		if (propsValues != null) {
			resourceDescriptor.properties = new ArrayList<>(propsValues.size());
			propsValues.entrySet().forEach(entry -> {
				final PropertyValue p = new PropertyValue();
				p.propertyId = entry.getKey();
				p.value = entry.getValue();
				resourceDescriptor.properties.add(p);
			});
		}

		this.provider.instance(IResources.class, domainUid).create(resourceId, resourceDescriptor);
	}

}
