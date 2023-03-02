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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.videoconferencing.webex;

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
import net.bluemind.icalendar.parser.ICal4jHelper;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.system.api.ExternalSystem.AuthKind;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.api.VideoConference;
import net.bluemind.videoconferencing.service.template.TemplateBasedVideoConferencingProvider;
import net.bluemind.videoconferencing.webex.client.WebexConferenceClient;
import net.bluemind.videoconferencing.webex.dto.WebexConference;
import net.bluemind.videoconferencing.webex.dto.WebexDialInfo;
import net.bluemind.videoconferencing.webex.dto.WebexInivitee;

public class WebexProvider extends TemplateBasedVideoConferencingProvider implements IVideoConferencingProvider {

	public static final String ID = "videoconferencing-webex";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Webex";
	}

	@Override
	public Optional<byte[]> getIcon() {
		try {
			return Optional.of(ByteStreams
					.toByteArray(WebexProvider.class.getClassLoader().getResourceAsStream("resources/logo.png")));
		} catch (IOException e) {
		}
		return Optional.empty();
	}

	@Override
	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent) throws ServerFault {
		String title = Strings.isNullOrEmpty(vevent.summary) ? "conf" : vevent.summary;

		String tz = vevent.timezone();
		String start = new BmDateTimeWrapper(vevent.dtstart).format("yyyy-MM-dd'T'HH:mm:ss");
		String end = new BmDateTimeWrapper(vevent.dtend).format("yyyy-MM-dd'T'HH:mm:ss");

		Optional<String> repetition = null;

		if (vevent.rrule != null) {
			repetition = Optional.of(ICal4jHelper.toRRuleString(vevent.rrule));
		} else {
			repetition = Optional.empty();
		}

		List<WebexInivitee> invitees = vevent.attendees.stream()
				.map(att -> new WebexInivitee(att.mailto, Optional.ofNullable(att.commonName))).toList();
		WebexConference conference = new WebexConference(title, start, end, tz, repetition, invitees);
		WebexConferenceClient webexConferenceClient = new WebexConferenceClient(context);

		String confId = vevent.conferenceId;
		WebexDialInfo dialInfo;
		if (Strings.isNullOrEmpty(confId)) {
			dialInfo = webexConferenceClient.create(conference);
			vevent.conferenceId = dialInfo.confId;
			vevent.conference = dialInfo.weblink;
		} else {
			dialInfo = webexConferenceClient.update(confId, conference);
		}

		resourceSettings.put("url", dialInfo.weblink);
		return super.getConferenceInfo(context, resourceSettings, resource, vevent);
	}

	@Override
	public void deleteConference(BmContext context, Map<String, String> resourceSettings, String conferenceId) {
		WebexConferenceClient webexConferenceClient = new WebexConferenceClient(context);
		webexConferenceClient.delete(conferenceId);
	}

	@Override
	public AuthKind getAuthKind() {
		return AuthKind.OPEN_ID_PKCE;
	}

}