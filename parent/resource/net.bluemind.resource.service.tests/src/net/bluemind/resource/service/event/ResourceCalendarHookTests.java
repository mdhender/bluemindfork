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
package net.bluemind.resource.service.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.calendar.persistance.VEventSeriesStore;
import net.bluemind.calendar.service.internal.VEventContainerStoreService;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.persistance.IItemValueStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.internal.MailboxStoreService;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceReservationMode;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.persistance.UserSubscriptionStore;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class ResourceCalendarHookTests {
	private String domainUid;
	private ContainerStore containerHome;
	private Container userContainer;
	private ItemValue<User> user1;
	protected MailboxStoreService mailboxStore;
	private ContainerUserStoreService userStoreService;

	@Test
	public void hook_onMaster_ShouldDoNothing() throws Exception {
		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container resourceCal = storeEvent("evt1", event);

		ContainerStore containerService = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		Container container = containerService.get(ICalendarUids.defaultUserCalendar(user1.uid));
		callResourceCalendarHook("evt1", event, container);

		ICalendar calService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				resourceCal.uid);
		assertEquals(ParticipationStatus.NeedsAction,
				calService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void hook_onOtherAttendee_ShouldDoNothing() throws Exception {
		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "u2", "", "", "", "bm://" + domainUid + "/user/u2", "", "u2", "u2@" + domainUid));
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container resourceCal = storeEvent("evt1", event);

		ContainerStore containerService = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		Container container = containerService.get(ICalendarUids.defaultUserCalendar("u2"));
		callResourceCalendarHook("evt1", event, container);

		ICalendar calService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				resourceCal.uid);

		boolean checked = false;
		for (Attendee att : calService.getComplete("evt1").value.main.attendees) {
			if (att.dir.contains("r1")) {
				assertEquals(ParticipationStatus.NeedsAction, att.partStatus);
				checked = true;
			}
		}
		assertTrue(checked);

	}

	@Test
	public void invitingAResource_usingDefaultReservationMode__OnFreeSlot_ShouldAutoAccept() throws Exception {
		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		ICalendar calService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				container.uid);

		assertEquals(ParticipationStatus.Accepted,
				calService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAcceptReservationMode__OnFreeSlot_ShouldAccept() throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT;
		resService.update("r1", resourceDescriptor);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		ICalendar calService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				container.uid);

		assertEquals(ParticipationStatus.Accepted,
				calService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAcceptRefuseReservationMode_OnFreeSlot_ShouldAccept() throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT;
		resService.update("r1", resourceDescriptor);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		ICalendar calService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));

		assertEquals(ParticipationStatus.Accepted,
				calService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingOwnerManagedReservationMode_OnFreeSlot_ShouldDoNothing() throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.OWNER_MANAGED;
		resService.update("r1", resourceDescriptor);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		ICalendar calService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));

		assertEquals(ParticipationStatus.NeedsAction,
				calService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingOwnerManagedReservationMode_OnBusySlot_ShouldDoNothing() throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.OWNER_MANAGED;
		resService.update("r1", resourceDescriptor);

		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		eventService.create(UUID.randomUUID().toString(), defaultVEvent("r1"), false);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.NeedsAction,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);

	}

	@Test
	public void invitingAResource_usingDefaultReservationMode_OnBusySlot_ShouldAutoRefuse() throws Exception {
		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		eventService.create(UUID.randomUUID().toString(), defaultVEvent("r1"), false);

		VEventSeries event = defaultVEvent("invite");
		event.main.dtstart = BmDateTimeWrapper.create("2017-05-23T11:38:14Z");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.Declined,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_OnTentativeSlot_ShouldDoNothing() throws Exception {
		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		VEventSeries defaultVEvent = defaultVEvent("ree");
		defaultVEvent.main.dtstart = BmDateTimeWrapper.create("2017-05-23T11:38:14Z");
		defaultVEvent.main.attendees.add(VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant,
				ParticipationStatus.Tentative, false, "r1", "", "", "", "", "", "r1", "r1@" + domainUid));

		eventService.create(UUID.randomUUID().toString(), defaultVEvent, false);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.NeedsAction,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource__RecurringEvent_ShouldDoNothing() throws Exception {
		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		VEventSeries event = defaultVEvent("invite");
		event.main.rrule = new RRule();
		event.main.rrule.byDay = Arrays.asList(WeekDay.SA);
		event.main.rrule.frequency = Frequency.DAILY;
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.NeedsAction,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAutoAcceptReservationMode_OnBusySlot_ShouldDoNothing() throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT;
		resService.update("r1", resourceDescriptor);

		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		VEventSeries defaultVEvent = defaultVEvent("ree");
		defaultVEvent.main.dtstart = BmDateTimeWrapper.create("2017-05-23T11:38:14Z");
		defaultVEvent.main.attendees.add(VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant,
				ParticipationStatus.Accepted, false, "r1", "", "", "", "", "", "r1", "r1@" + domainUid));

		eventService.create(UUID.randomUUID().toString(), defaultVEvent, false);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.NeedsAction,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAutoAcceptRefuseReservationMode_OnBusySlot_ShouldAutoRefuse() throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT_REFUSE;
		resService.update("r1", resourceDescriptor);

		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		VEventSeries defaultVEvent = defaultVEvent("ree");
		defaultVEvent.main.dtstart = BmDateTimeWrapper.create("2017-05-23T11:38:14Z");
		defaultVEvent.main.attendees.add(VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant,
				ParticipationStatus.Accepted, false, "r1", "", "", "", "", "", "r1", "r1@" + domainUid));

		eventService.create(UUID.randomUUID().toString(), defaultVEvent, false);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.Declined,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAutoAcceptRefuseReservationMode_AllDay_OnBusyUnavailableDay_ShouldAutoRefuse()
			throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT_REFUSE;
		resService.update("r1", resourceDescriptor);

		setUnavailabilitySettings("r1", Arrays.asList(Day.MO, Day.WE, Day.TH, Day.SA, Day.SU), 8, 18);

		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		// Crossing a not working day
		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.dtstart = BmDateTimeWrapper.create("2018-03-01", Precision.Date);
		event.main.dtend = BmDateTimeWrapper.create("2018-03-03", Precision.Date);
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.Declined,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAutoAcceptRefuseReservationMode_AllDay_OnBusyUnavailableHour_ShouldAutoAccept()
			throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT_REFUSE;
		resService.update("r1", resourceDescriptor);

		setUnavailabilitySettings("r1", Arrays.asList(Day.MO, Day.WE, Day.TH, Day.SA, Day.SU), 8, 18);

		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		// Crossing only on working days
		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.dtstart = BmDateTimeWrapper.create("2018-03-03", Precision.Date);
		event.main.dtend = BmDateTimeWrapper.create("2018-03-06", Precision.Date);
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.Accepted,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAutoAcceptRefuseReservationMode_AllDay_OnBusy_ShouldAutoRefuse()
			throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT_REFUSE;
		resService.update("r1", resourceDescriptor);

		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		VEventSeries defaultVEvent = defaultVEvent("ree");
		defaultVEvent.main.dtstart = BmDateTimeWrapper.create("2018-03-01T11:38:14Z");
		defaultVEvent.main.dtend = BmDateTimeWrapper.create("2018-03-01T12:38:14Z");

		defaultVEvent.main.attendees.add(VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant,
				ParticipationStatus.Accepted, false, "r1", "", "", "", "", "", "r1", "r1@" + domainUid));

		eventService.create(UUID.randomUUID().toString(), defaultVEvent, false);

		VEventSeries event = defaultVEvent("invite");
		event.main.dtstart = BmDateTimeWrapper.create("2018-03-01", Precision.Date);
		event.main.dtend = BmDateTimeWrapper.create("2018-03-02", Precision.Date);
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.Declined,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Test
	public void invitingAResource_usingAutoAcceptRefuseReservationMode_OnBusySlot_ShouldDoNothing_IfAlreadyRefusedEvent()
			throws Exception {
		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		ResourceDescriptor resourceDescriptor = resService.get("r1");
		resourceDescriptor.reservationMode = ResourceReservationMode.AUTO_ACCEPT_REFUSE;
		resService.update("r1", resourceDescriptor);

		ICalendar eventService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.resourceCalendar("r1"));
		VEventSeries defaultVEvent = defaultVEvent("ree");
		defaultVEvent.main.dtstart = BmDateTimeWrapper.create("2017-05-23T11:38:14Z");
		defaultVEvent.main.attendees.add(VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant,
				ParticipationStatus.Accepted, false, "r1", "", "", "", "", "", "r1", "r1@" + domainUid));

		eventService.create(UUID.randomUUID().toString(), defaultVEvent, false);

		VEventSeries event = defaultVEvent("invite");
		event.main.status = ICalendarElement.Status.NeedsAction;
		event.main.attendees.add(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "r1", "", "", "", "bm://" + domainUid + "/resources/r1", "", "r1", "r1@" + domainUid));

		Container container = storeEvent("evt1", event);
		callResourceCalendarHook("evt1", event, container);

		assertEquals(ParticipationStatus.Declined,
				eventService.getComplete("evt1").value.main.attendees.get(0).partStatus);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		ElasticsearchTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		domainUid = "test.lan";

		// register elasticsearch to locator
		// Server esServer = new Server();
		// esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		// esServer.tags = Lists.newArrayList("bm/es");

		Server esServer = new Server();
		esServer.ip = new BmConfIni().get("es-host");
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid, esServer);

		userContainer = containerHome.get(domainUid);

		userStoreService = new ContainerUserStoreService(new BmTestContext(SecurityContext.SYSTEM), userContainer,
				domain);

		Container mboxContainer = containerHome.get(domainUid);
		assertNotNull(mboxContainer);

		mailboxStore = new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM,
				mboxContainer);

		ItemValue<User> user1Item = createTestUSer("u1");
		user1 = user1Item;
		createTestUSer("u2");

		ResourceDescriptor res = new ResourceDescriptor();
		res.description = "r1";
		res.emails = Arrays.asList(Email.create("r1@" + domainUid, true));
		res.label = "r1";
		res.typeIdentifier = "default";
		res.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		IResources resService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class,
				domainUid);
		resService.create("r1", res);

	}

	private ItemValue<User> createTestUSer(String login) throws ServerFault, SQLException {
		ItemValue<User> user = defaultUser(login, login);
		userStoreService.create(user.uid, login, user.value);
		SecurityContext securityContext = new SecurityContext(login, login, new ArrayList<String>(),
				new ArrayList<String>(), domainUid);
		createTestContainer(securityContext, ICalendarUids.TYPE, user.value.login,
				ICalendarUids.TYPE + ":Default:" + user.uid, user.uid);
		Sessions.get().put(login, securityContext);
		return user;
	}

	private void createTestContainer(SecurityContext context, String type, String login, String name, String owner)
			throws SQLException {
		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), context);
		Container container = Container.create(name, type, name, owner, "test.lan", true);
		container = containerHome.create(container);

		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(SecurityContext.SYSTEM,
				JdbcTestHelper.getInstance().getDataSource(), containerHome.get(domainUid));

		userSubscriptionStore.subscribe(context.getSubject(), container);
	}

	private ItemValue<User> defaultUser(String uid, String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@test.lan";
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		card.identification.formatedName = FormatedName.create(login);
		user.contactInfos = card;
		return ItemValue.create(uid, user);
	}

	private VEventSeries defaultVEvent(String title) {
		VEvent event = new VEvent();
		DateTimeZone tz = DateTimeZone.forID("Europe/Paris");

		event.dtstart = BmDateTimeWrapper.create("2017-05-23T11:37:14Z");
		event.dtend = BmDateTimeWrapper.create("2017-05-23T12:37:14Z");
		event.summary = title + "-" + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.priority = 1;
		event.organizer = new VEvent.Organizer(null, user1.value.defaultEmail().address);
		// event.organizer.uri = ICalendarUids.TYPE + ":Default:" +
		// "u1";
		event.attendees = new ArrayList<>();
		event.categories = new ArrayList<TagRef>(0);
		VEventSeries series = new VEventSeries();
		series.main = event;
		series.icsUid = UUID.randomUUID().toString();

		return series;
	}

	private void callResourceCalendarHook(String itemUid, VEventSeries event, Container container) {
		VEventMessage message = new VEventMessage();
		message.container = container;
		message.itemUid = itemUid;
		message.vevent = event;
		message.sendNotifications = true;
		message.securityContext = SecurityContext.SYSTEM;
		CountDownLatch cdl = new CountDownLatch(1);
		new ResourceCalendarHook(1, TimeUnit.MILLISECONDS).onEventCreated(message);
		VertxPlatform.getVertx().setTimer(200, tid -> cdl.countDown());
		try {
			boolean gotIt = cdl.await(2, TimeUnit.SECONDS);
			System.out.println("Wait is over: " + gotIt);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

	}

	private Container storeEvent(String itemUid, VEventSeries event) throws SQLException {
		BmContext context = new BmTestContext(SecurityContext.SYSTEM);
		DataSource ds = DataSourceRouter.get(context, ICalendarUids.resourceCalendar("r1"));

		ContainerStore containerService = new ContainerStore(ds, SecurityContext.SYSTEM);
		Container container = containerService.get(ICalendarUids.resourceCalendar("r1"));
		IItemValueStore<VEventSeries> veventStore = new VEventSeriesStore(ds, container);
		VEventContainerStoreService storeService = new VEventContainerStoreService(context, ds, SecurityContext.SYSTEM,
				container, "calendar", veventStore);

		storeService.createWithId(itemUid, null, event.icsUid, event.main.summary, event);
		return container;
	}

	private void setUnavailabilitySettings(String resourceId, List<Day> wd, int ds, int de) {
		CalendarSettingsData s = new CalendarSettingsData();
		s.dayStart = new LocalTime(ds, 0).getMillisOfDay();
		s.dayEnd = new LocalTime(de, 0).getMillisOfDay();
		s.timezoneId = DateTimeZone.UTC.getID();
		s.minDuration = 5;
		s.workingDays = wd;

		ICalendarSettings calendarSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ICalendarSettings.class, ICalendarUids.resourceCalendar(resourceId));
		calendarSettings.set(s);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

}