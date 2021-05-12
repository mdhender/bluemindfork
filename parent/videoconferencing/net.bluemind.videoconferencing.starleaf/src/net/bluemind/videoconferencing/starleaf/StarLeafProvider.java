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
import net.bluemind.videoconferencing.starleaf.dto.SLConference;
import net.bluemind.videoconferencing.starleaf.dto.SLConference.Layout;
import net.bluemind.videoconferencing.starleaf.dto.SLConferenceDialInfo;
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
			ItemValue<ResourceDescriptor> resource, ICalendarElement vevent) {

		String token = resourceSettings.get("token");
		String conferenceOwner = getStarLeafUserUid(context, vevent, token);
		String title = Strings.isNullOrEmpty(vevent.summary) ? "conf" : vevent.summary;

		SLConference conference = new SLConference(conferenceOwner, title, vevent.description,
				Layout.speaker_with_strip, false, false, false, "", "");
		SLConferenceClient starLeafConferenceClient = new SLConferenceClient(token);
		SLConferenceDialInfo dialInfo = starLeafConferenceClient.create(conference);

		String description = templateHelper.addTag(dialInfo.customInviteFooter.replace("\n", "<br/>"), resource.uid);
		return new VideoConference(dialInfo.dialInfoUrl, description);
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