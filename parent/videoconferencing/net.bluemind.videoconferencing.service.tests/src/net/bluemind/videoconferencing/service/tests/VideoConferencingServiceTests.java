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
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.videoconferencing.api.IVideoConferencing;
import net.bluemind.videoconferencing.api.VideoConferencingResourceDescriptor;

public class VideoConferencingServiceTests extends AbstractVideoConferencingTests {

	private BmTestContext domainAdminCtx;

	private String videoconfProviderId = "test-provider";

	@Override
	public void before() throws Exception {
		super.before();
		PopulateHelper.addDomain(domainUid);

		domainAdminCtx = BmTestContext.contextWithSession("sid", "admin", domainUid, SecurityContext.ROLE_ADMIN);

		// videoconf resource
		ServerSideServiceProvider.getProvider(domainAdminCtx).instance(IVideoConferencing.class, domainUid)
				.createResource(videoconfProviderId,
						VideoConferencingResourceDescriptor.create("coucou", "test-provider"));

		Map<String, String> settings = new HashMap<>();
		settings.put("url", "http://video.conf");
		settings.put("templates", "{\"fr\":\"voilà ${URL} yay\",\"en\":\"this is ${URL}<br>\"}");
		IContainerManagement containerMgmtService = ServerSideServiceProvider.getProvider(domainAdminCtx)
				.instance(IContainerManagement.class, videoconfProviderId + "-settings-container");
		containerMgmtService.setSettings(settings);
	}

	@Test
	public void testVideoConferencing() {
		VEventSeries event = defaultVEvent();
		VEvent.Attendee videoconf = VEvent.Attendee.create(VEvent.CUType.Resource, "", VEvent.Role.OptionalParticipant,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef",
				"bm://" + domainUid + "/resources/" + videoconfProviderId, null, null,
				"videoconferencing@" + domainUid);
		event.main.attendees.add(videoconf);

		// hello videoconf
		ICalendarElement main = getService(domainAdminCtx.getSecurityContext()).add(event.main);
		assertNotNull(main.conference);
		assertEquals(
				"Lorem ipsum blah blah<videoconferencingtemplate id=\"" + videoconfProviderId
						+ "\"><br><div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div>voilà <a href=\"" + main.conference
						+ "\" target=\"_blank\">" + main.conference + "</a> yay"
						+ "<div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div><br></videoconferencingtemplate>",
				main.description);

		// bye-bye videoconf
		main = getService(domainAdminCtx.getSecurityContext()).remove(main);
		assertNull(main.conference);

		assertEquals(defaultVEvent().main.description, main.description);
		assertEquals(defaultVEvent().main.attendees.size(), main.attendees.size());
	}

	@Test
	public void testCreateVideoConferencingResource() {
		String uid = UUID.randomUUID().toString();
		getService(domainAdminCtx.getSecurityContext()).createResource(uid,
				VideoConferencingResourceDescriptor.create("yeah", "test-provider"));

		ResourceDescriptor res = ServerSideServiceProvider.getProvider(domainAdminCtx)
				.instance(IResources.class, domainUid).get(uid);
		assertNotNull(res);

		IContainers containersService = ServerSideServiceProvider.getProvider(domainAdminCtx)
				.instance(IContainers.class);
		ContainerDescriptor resCalendar = containersService.get(ICalendarUids.resourceCalendar(uid));
		assertNotNull(resCalendar);
		ContainerDescriptor resContainerSettings = containersService.get(uid + "-settings-container");
		assertNotNull(resContainerSettings);

		IContainerManagement containerMgmtService = ServerSideServiceProvider.getProvider(domainAdminCtx)
				.instance(IContainerManagement.class, resCalendar.uid);
		List<AccessControlEntry> calAcls = containerMgmtService.getAccessControlList();
		assertEquals(1, calAcls.size());
		AccessControlEntry ace = calAcls.get(0);
		assertEquals(domainUid, ace.subject);
		assertEquals(Verb.Invitation, ace.verb);

		containerMgmtService = ServerSideServiceProvider.getProvider(domainAdminCtx)
				.instance(IContainerManagement.class, resContainerSettings.uid);
		List<AccessControlEntry> settingsdAcls = containerMgmtService.getAccessControlList();
		assertEquals(1, settingsdAcls.size());
		ace = settingsdAcls.get(0);
		assertEquals(domainUid, ace.subject);
		assertEquals(Verb.Read, ace.verb);

	}

	@Test
	public void testDeleteVideoConferencingResource() throws Exception {
		String uid = UUID.randomUUID().toString();
		getService(domainAdminCtx.getSecurityContext()).createResource(uid,
				VideoConferencingResourceDescriptor.create("woot", "test-provider"));

		IContainers containersService = ServerSideServiceProvider.getProvider(domainAdminCtx)
				.instance(IContainers.class);
		ContainerDescriptor resContainerSettings = containersService.get(uid + "-settings-container");
		assertNotNull(resContainerSettings);

		TaskRef tr = ServerSideServiceProvider.getProvider(domainAdminCtx).instance(IResources.class, domainUid)
				.delete(uid);
		waitEnd(tr);

		try {
			containersService.get(uid + "-settings-container");
			fail(uid + "-settings-container still exists");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}

	}

	public TaskStatus waitEnd(TaskRef ref) throws Exception {
		TaskStatus status = null;
		while (true) {
			ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, ref.id);
			status = task.status();
			if (status.state.ended) {
				break;
			}
		}

		return status;
	}

	protected IVideoConferencing getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IVideoConferencing.class, domainUid);
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

		event.organizer = new VEvent.Organizer("roberto@" + domainUid);

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				"external@attendee.lan");
		attendees.add(me);

		event.attendees = attendees;

		series.main = event;
		return series;
	}

}
