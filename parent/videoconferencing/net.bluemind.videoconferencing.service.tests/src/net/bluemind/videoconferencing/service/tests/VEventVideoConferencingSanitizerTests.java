/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;
import net.bluemind.videoconferencing.service.calendar.VEventVideoConferencingSanitizer;

public class VEventVideoConferencingSanitizerTests extends AbstractVideoConferencingTests {
	private BmTestContext domainAdminCtx;

	private String videoconfProviderId = "test-provider";
	private VEventVideoConferencingSanitizer sanitizer;

	@Override
	public void before() throws Exception {
		super.before();
		PopulateHelper.addDomain(domainUid);

		domainAdminCtx = BmTestContext.contextWithSession("sid", "admin", domainUid, SecurityContext.ROLE_ADMIN);

		// videoconf resource
		ServerSideServiceProvider.getProvider(domainAdminCtx).instance(IResources.class, domainUid)
				.create(videoconfProviderId, defaultDescriptor());

		ContainerDescriptor cd = ContainerDescriptor.create(videoconfProviderId + "-settings-container",
				"settings container video conf", videoconfProviderId, "container_settings", domainUid, false);

		ServerSideServiceProvider.getProvider(domainAdminCtx).instance(IContainers.class)
				.create(videoconfProviderId + "-settings-container", cd);

		Map<String, String> settings = new HashMap<>();
		settings.put("url", "http://video.conf");
		settings.put("templates", "{\"fr\":\"voilà ${URL} yay\",\"en\":\"this is ${URL}<br>\"}");
		IContainerManagement containerMgmtService = ServerSideServiceProvider.getProvider(domainAdminCtx)
				.instance(IContainerManagement.class, videoconfProviderId + "-settings-container");
		containerMgmtService.setAccessControlList(Arrays.asList(AccessControlEntry.create(domainUid, Verb.All)));
		containerMgmtService.setSettings(settings);

		Container cal = Container.create(ICalendarUids.defaultUserCalendar("admin"), ICalendarUids.TYPE,
				"admin's calenddar", "admin");

		sanitizer = new VEventVideoConferencingSanitizer(domainAdminCtx, cal);

	}

	@Test
	public void testCreate() {
		VEventSeries event = defaultVEvent();
		VEvent.Attendee videoconf = VEvent.Attendee.create(VEvent.CUType.Resource, "", VEvent.Role.OptionalParticipant,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef",
				"bm://" + domainUid + "/resources/" + videoconfProviderId, null, null,
				"videoconferencing@" + domainUid);
		event.main.attendees.add(videoconf);

		sanitizer.create(event);

		assertNotNull(event.main.conference);
		assertEquals(
				"Lorem ipsum blah blah<br><videoconferencingtemplate id=\"" + videoconfProviderId
						+ "\"><div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div>voilà <a href=\"" + event.main.conference
						+ "\" target=\"_blank\">" + event.main.conference
						+ "</a> yay<div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div></videoconferencingtemplate><br><br>",
				event.main.description);
	}

	@Test
	public void testDelete() {
		VEventSeries event = defaultVEvent();
		VEvent.Attendee videoconf = VEvent.Attendee.create(VEvent.CUType.Resource, "", VEvent.Role.OptionalParticipant,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef",
				"bm://" + domainUid + "/resources/" + videoconfProviderId, null, null,
				"videoconferencing@" + domainUid);
		event.main.attendees.add(videoconf);

		sanitizer.create(event);

		VEventSeries updated = defaultVEvent();
		sanitizer.update(event, updated);

		assertNull(updated.main.conference);
		assertEquals(defaultVEvent().main.description, updated.main.description);
	}

	@Test
	public void testAdd() {
		VEventSeries event = defaultVEvent();
		sanitizer.create(event);

		assertNull(event.main.conference);
		assertEquals(defaultVEvent().main.description, event.main.description);

		VEventSeries updated = event.copy();
		VEvent.Attendee videoconf = VEvent.Attendee.create(VEvent.CUType.Resource, "", VEvent.Role.OptionalParticipant,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef",
				"bm://" + domainUid + "/resources/" + videoconfProviderId, null, null,
				"videoconferencing@" + domainUid);
		updated.main.attendees.add(videoconf);

		sanitizer.update(event, updated);
		assertNotNull(updated.main.conference);
		assertEquals(
				"Lorem ipsum blah blah<br><videoconferencingtemplate id=\"" + videoconfProviderId
						+ "\"><div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div>voilà <a href=\"" + updated.main.conference
						+ "\" target=\"_blank\">" + updated.main.conference
						+ "</a> yay<div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div></videoconferencingtemplate><br><br>",
				updated.main.description);

	}

	@Test
	public void testUpdateVideoConfEventDescription() {
		VEventSeries event = defaultVEvent();
		VEvent.Attendee videoconf = VEvent.Attendee.create(VEvent.CUType.Resource, "", VEvent.Role.OptionalParticipant,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef",
				"bm://" + domainUid + "/resources/" + videoconfProviderId, null, null,
				"videoconferencing@" + domainUid);
		event.main.attendees.add(videoconf);

		sanitizer.create(event);

		VEventSeries updated = event.copy();
		updated.main.description = "coucou";
		sanitizer.update(event, updated);
		assertNotNull(updated.main.conference);
		assertEquals(
				"coucou<br><videoconferencingtemplate id=\"" + videoconfProviderId
						+ "\"><div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div>voilà <a href=\"" + updated.main.conference
						+ "\" target=\"_blank\">" + updated.main.conference
						+ "</a> yay<div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div></videoconferencingtemplate><br><br>",
				updated.main.description);

		String currentDescription = updated.main.description;

		sanitizer.update(event, updated);

		assertEquals(currentDescription, updated.main.description);
	}

	@Test
	public void testUpdateVideoConfRemoveResource() {
		VEventSeries event = defaultVEvent();
		VEvent.Attendee videoconf = VEvent.Attendee.create(VEvent.CUType.Resource, "", VEvent.Role.OptionalParticipant,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef",
				"bm://" + domainUid + "/resources/" + videoconfProviderId, null, null,
				"videoconferencing@" + domainUid);
		event.main.attendees.add(videoconf);

		sanitizer.create(event);

		VEventSeries updated = event.copy();
		updated.main.attendees = defaultVEvent().main.attendees;
		sanitizer.update(event, updated);
		assertNull(updated.main.conference);
		assertEquals(defaultVEvent().main.description, updated.main.description);
	}

	protected VEventSeries defaultVEvent() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2032, 2, 13, 1, 0, 0, 0, tz));
		event.summary = "VideoConferencingServiceTests " + System.currentTimeMillis();
		event.description = "Lorem ipsum blah blah";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer("admin@" + domainUid);
		event.organizer.dir = "bm://" + IDirEntryPath.path(domainUid, "admin", Kind.USER);

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				"external@attendee.lan");
		attendees.add(me);

		event.attendees = attendees;

		series.main = event;
		return series;
	}

	private ResourceDescriptor defaultDescriptor() {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.label = "coucou";
		rd.typeIdentifier = IVideoConferenceUids.RESOURCETYPE_UID;
		rd.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		rd.emails = Arrays.asList(Email.create("videoconferencing@" + domainUid, true));
		rd.properties = Arrays.asList(
				ResourceDescriptor.PropertyValue.create(IVideoConferenceUids.PROVIDER_TYPE, videoconfProviderId));
		return rd;
	}
}
