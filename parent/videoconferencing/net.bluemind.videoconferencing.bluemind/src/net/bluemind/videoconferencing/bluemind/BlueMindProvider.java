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
package net.bluemind.videoconferencing.bluemind;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.api.VideoConference;
import net.bluemind.videoconferencing.saas.api.BlueMindVideoRoom;
import net.bluemind.videoconferencing.saas.api.IVideoConferencingSaas;
import net.bluemind.videoconferencing.service.template.TemplateBasedVideoConferencingProvider;

public class BlueMindProvider extends TemplateBasedVideoConferencingProvider implements IVideoConferencingProvider {

	@Override
	public String id() {
		return "videoconferencing-bluemind";
	}

	@Override
	public String name() {
		return "BlueMind.Video";
	}

	@Override
	public Optional<byte[]> getIcon() {
		try {
			return Optional.of(ByteStreams
					.toByteArray(BlueMindProvider.class.getClassLoader().getResourceAsStream("resources/icon.png")));
		} catch (IOException e) {
		}
		return Optional.empty();
	}

	@Override
	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent) {
		ServerSideServiceProvider serviceProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		Optional<String> externalUrl = Optional
				.ofNullable(serviceProvider.instance(ISystemConfiguration.class).getValues().values
						.get(SysConfKeys.external_url.name()));
		resourceSettings.put("url", externalUrl.get() + "/visio/");

		IVideoConferencingSaas saasService = serviceProvider.instance(IVideoConferencingSaas.class);
		BlueMindVideoRoom room = null;

		if (!Strings.isNullOrEmpty(vevent.conferenceId)) {
			room = saasService.get(vevent.conferenceId);
		}

		if (room == null) {
			room = new BlueMindVideoRoom();
			room.identifier = UUID.randomUUID().toString();
			room.title = vevent.summary;
			room.owner = context.getSecurityContext().getOwnerPrincipal();
			saasService.create(room);
			vevent.conferenceId = room.identifier;
		} else {
			if (!vevent.summary.equals(room.title)) {
				saasService.updateTitle(vevent.conferenceId, vevent.summary);
			}
		}
		return super.getConferenceInfo(context, resourceSettings, resource, vevent);
	}
}
