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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.videoconferencing.service.videoconfhook;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.videoconferencing.api.IVideoConferencing;

public class VideoConferencingCalHook implements ICalendarHook {
	private static final Logger logger = LoggerFactory.getLogger(VideoConferencingCalHook.class);

	@Override
	public void onEventCreated(VEventMessage message) {
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		Optional<VEvent> vevent = Optional.of(message.vevent.main);
		vevent.ifPresent(e -> {
			if (videoConferenceCanBeDeleted(message.securityContext, e)) {
				IVideoConferencing videoConfService = ServerSideServiceProvider.getProvider(message.securityContext)
						.instance(IVideoConferencing.class, message.container.domainUid);
				videoConfService.remove(e);
			}
		});
	}

	private static boolean videoConferenceCanBeDeleted(SecurityContext context, VEvent vevent) {
		return vevent.organizer == null
				|| vevent.organizer != null && vevent.eventOrganizer(context.getOwnerPrincipal());
	}
}
