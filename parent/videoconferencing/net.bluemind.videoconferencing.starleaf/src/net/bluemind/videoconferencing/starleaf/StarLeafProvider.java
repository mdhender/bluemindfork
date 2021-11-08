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
package net.bluemind.videoconferencing.starleaf;

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
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.api.VideoConference;
import net.bluemind.videoconferencing.service.template.VideoConferencingTemplateHelper;
import net.bluemind.videoconferencing.starleaf.client.SLConferenceClient;
import net.bluemind.videoconferencing.starleaf.client.SLUserClient;
import net.bluemind.videoconferencing.starleaf.dto.SLConferenceDialInfo;
import net.bluemind.videoconferencing.starleaf.dto.SLConferenceRepetition;
import net.bluemind.videoconferencing.starleaf.dto.SLConferenceSettings;
import net.bluemind.videoconferencing.starleaf.dto.SLUser;

public class StarLeafProvider implements IVideoConferencingProvider {

	private static final VideoConferencingTemplateHelper templateHelper = new VideoConferencingTemplateHelper();

	@Override
	public String id() {
		return "videoconferencing-starleaf";
	}

	@Override
	public String name() {
		return "StarLeaf";
	}

	@Override
	public Optional<byte[]> getIcon() {
		try {
			return Optional.of(ByteStreams
					.toByteArray(StarLeafProvider.class.getClassLoader().getResourceAsStream("resources/logo.png")));
		} catch (IOException e) {
		}
		return Optional.empty();
	}

	@Override
	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent) {

		String token = resourceSettings.get("token");
		String conferenceOwner = getStarLeafUserUid(context, vevent, token);
		String title = Strings.isNullOrEmpty(vevent.summary) ? "conf" : vevent.summary;

		String tz = vevent.timezone();
		String start = new BmDateTimeWrapper(vevent.dtstart).format("yyyy-MM-dd'T'HH:mm:ss");
		String end = new BmDateTimeWrapper(vevent.dtend).format("yyyy-MM-dd'T'HH:mm:ss");

		SLConferenceRepetition repetition = null;

		if (vevent.rrule != null) {
			String until = null;
			if (vevent.rrule.until != null) {
				until = new BmDateTimeWrapper(vevent.rrule.until).format("yyyy-MM-dd'T'HH:mm:ss");
			}
			repetition = new SLConferenceRepetition(SLConferenceRepetition.frequency(vevent.rrule.frequency),
					vevent.rrule.interval, vevent.rrule.count, until, null, null, null, null, null);
		}

		SLConferenceSettings conference = new SLConferenceSettings(conferenceOwner, title, vevent.description, true,
				start, end, tz, repetition);
		SLConferenceClient starLeafConferenceClient = new SLConferenceClient(token);

		String confId = vevent.conferenceId;
		SLConferenceDialInfo dialInfo;
		if (Strings.isNullOrEmpty(confId)) {
			dialInfo = starLeafConferenceClient.create(conference);
			confId = dialInfo.confId;
		} else {
			dialInfo = starLeafConferenceClient.get(confId).dialInfo;
			conference = new SLConferenceSettings(conferenceOwner, title,
					templateHelper.removeTemplate(vevent.description, resource.uid), false, start, end, tz, repetition);
			starLeafConferenceClient.update(confId, conference);
		}
		String description = templateHelper.addTag(dialInfo.customInviteFooter.replace("\n", "<br/>"), resource.uid);
		return new VideoConference(confId, dialInfo.dialInfoUrl, description);
	}

	@Override
	public void deleteConference(BmContext context, Map<String, String> resourceSettings, String conferenceId) {
		String token = resourceSettings.get("token");
		SLConferenceClient starLeafConferenceClient = new SLConferenceClient(token);

		starLeafConferenceClient.delete(conferenceId);
	}

	private String resolveOrganizer(BmContext context, ICalendarElement vevent) {
		String organizer = vevent.organizer.mailto;
		if (Strings.isNullOrEmpty(organizer)) {
			IDirectory directory = context.provider().instance(IDirectory.class,
					context.getSecurityContext().getContainerUid());
			DirEntry dirEntry = directory.getEntry(vevent.organizer.dir.substring("bm://".length()));
			organizer = dirEntry.email;

		}

		return organizer;
	}

	private String getStarLeafUserUid(BmContext context, ICalendarElement vevent, String token) {
		String organizer = resolveOrganizer(context, vevent);

		if (organizer == null) {
			throw new ServerFault("Null organizer");
		}

		SLUserClient slUserClient = new SLUserClient(token);
		List<SLUser> users = slUserClient.list();

		Optional<SLUser> slUser = users.stream().filter(user -> organizer.equals(user.email)).findFirst();
		if (!slUser.isPresent()) {
			throw new ServerFault("Null StarLeaf user");
		}

		return slUser.get().uid;
	}

}