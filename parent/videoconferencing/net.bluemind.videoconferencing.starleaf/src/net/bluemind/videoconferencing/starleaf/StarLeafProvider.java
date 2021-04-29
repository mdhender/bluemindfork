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
import java.util.Optional;

import com.google.common.io.ByteStreams;

import net.bluemind.videoconferencing.api.IVideoConferencingProvider;

public class StarLeafProvider implements IVideoConferencingProvider {

	@Override
	public String id() {
		return "videoconferencing-starleaf";
	}

	@Override
	public String name() {
		return "StarLeaf";
	}

	@Override
	public String getUrl(String baseUrl) {
		return "starleafurl";
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

}
