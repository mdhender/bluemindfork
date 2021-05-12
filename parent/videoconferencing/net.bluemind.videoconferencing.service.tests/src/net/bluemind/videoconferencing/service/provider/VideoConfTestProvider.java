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
package net.bluemind.videoconferencing.service.provider;

import java.util.Optional;

import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.service.template.TemplateBasedVideoConferencingProvider;

public class VideoConfTestProvider extends TemplateBasedVideoConferencingProvider
		implements IVideoConferencingProvider {

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

}
