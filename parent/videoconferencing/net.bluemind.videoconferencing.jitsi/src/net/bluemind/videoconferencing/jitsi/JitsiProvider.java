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
package net.bluemind.videoconferencing.jitsi;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.google.common.io.ByteStreams;

import net.bluemind.core.rest.BmContext;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.service.template.TemplateBasedVideoConferencingProvider;

public class JitsiProvider extends TemplateBasedVideoConferencingProvider implements IVideoConferencingProvider {

	@Override
	public String id() {
		return "videoconferencing-jitsi";
	}

	@Override
	public String name() {
		return "Jitsi";
	}

	@Override
	public Optional<byte[]> getIcon() {
		try {
			return Optional.of(ByteStreams
					.toByteArray(JitsiProvider.class.getClassLoader().getResourceAsStream("resources/icon.png")));
		} catch (IOException e) {
		}
		return Optional.empty();
	}

	@Override
	public void deleteConference(BmContext context, Map<String, String> resourceSettings, String conferenceId) {
	}

}
