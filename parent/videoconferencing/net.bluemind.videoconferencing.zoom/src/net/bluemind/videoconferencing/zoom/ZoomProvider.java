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
package net.bluemind.videoconferencing.zoom;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.system.api.ExternalSystem.AuthKind;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.api.VideoConference;
import net.bluemind.videoconferencing.service.template.TemplateBasedVideoConferencingProvider;
import net.bluemind.videoconferencing.zoom.client.ZoomConferenceClient;
import net.bluemind.videoconferencing.zoom.dto.ZoomConference;
import net.bluemind.videoconferencing.zoom.dto.ZoomDialInfo;
import net.bluemind.videoconferencing.zoom.dto.ZoomInivitee;

public class ZoomProvider extends TemplateBasedVideoConferencingProvider implements IVideoConferencingProvider {

	public static final String ID = "videoconferencing-zoom";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Zoom";
	}

	@Override
	public Optional<byte[]> getIcon() {
		try {
			return Optional.of(ByteStreams
					.toByteArray(ZoomProvider.class.getClassLoader().getResourceAsStream("resources/logo.png")));
		} catch (IOException e) {
		}
		return Optional.empty();
	}

	@Override
	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent) throws ServerFault {
		String title = Strings.isNullOrEmpty(vevent.summary) ? "conf" : vevent.summary;

		String tz = vevent.timezone();
		BmDateTimeWrapper bmDateTimeWrapperStart = new BmDateTimeWrapper(vevent.dtstart);
		String start = bmDateTimeWrapperStart.format("yyyy-MM-dd'T'HH:mm:ss");
		BmDateTimeWrapper bmDateTimeWrapperEnd = new BmDateTimeWrapper(vevent.dtend);
		String end = bmDateTimeWrapperEnd.format("yyyy-MM-dd'T'HH:mm:ss");

		int duration = (int) ((bmDateTimeWrapperEnd.toTimestamp("UTC") - bmDateTimeWrapperStart.toTimestamp("UTC"))
				/ 1000 / 60);

		Optional<RRule> repetition = null;

		if (vevent.rrule != null) {
			// FIXME recurrence
		} else {
			repetition = Optional.empty();
		}

		List<ZoomInivitee> invitees = vevent.attendees.stream().map(att -> new ZoomInivitee(att.mailto)).toList();
		ZoomConference conference = new ZoomConference(title, start, tz, duration, repetition, invitees);
		ZoomConferenceClient ZoomConferenceClient = new ZoomConferenceClient(context);

		String confId = vevent.conferenceId;
		ZoomDialInfo dialInfo;
		if (Strings.isNullOrEmpty(confId)) {
			dialInfo = ZoomConferenceClient.create(conference);
			vevent.conferenceId = dialInfo.confId;
			vevent.conference = dialInfo.weblink;
		} else {
			dialInfo = ZoomConferenceClient.update(confId, conference);
		}

		resourceSettings.put("url", dialInfo.weblink);
		return super.getConferenceInfo(context, resourceSettings, resource, vevent);
	}

	@Override
	public void deleteConference(BmContext context, Map<String, String> resourceSettings, String conferenceId) {
		ZoomConferenceClient ZoomConferenceClient = new ZoomConferenceClient(context);
		ZoomConferenceClient.delete(conferenceId);
	}

	@Override
	public AuthKind getAuthKind() {
		return AuthKind.OPEN_ID_PKCE;
	}

}