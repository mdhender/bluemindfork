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
package net.bluemind.calendar.hook.ics;

import java.util.Map;
import java.util.Optional;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.api.VideoConference;

public class VideoConfTestProvider implements IVideoConferencingProvider {

	@Override
	public String id() {
		return "test-provider";
	}

	@Override
	public String name() {
		return "Video Conferencing Provider Yay";
	}

	@Override
	public Optional<byte[]> getIcon() {
		return Optional.empty();
	}

	@Override
	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent) throws ServerFault {
		return null;
	}

	@Override
	public void deleteConference(BmContext context, Map<String, String> resourceSettings, String conferenceId) {
	}

}
