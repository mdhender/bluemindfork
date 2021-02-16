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

import java.util.Map;
import java.util.UUID;

import net.bluemind.core.rest.BmContext;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.videoconferencing.api.IVideoConferencing;

public class JitsiProvider implements IVideoConferencing {

	@Override
	public String id() {
		return "jitsi";
	}

	@Override
	public String name() {
		return "Jitsi";
	}

	@Override
	public ICalendarElement add(BmContext context, ICalendarElement vevent) {
		return vevent;
	}

	@Override
	public ICalendarElement remove(BmContext context, ICalendarElement vevent) {
		return vevent;
	}

	@Override
	public String getUrl(Map<String, String> resourceSettings) {

		String baseUrl = resourceSettings.get("url");

		if (!baseUrl.startsWith("http")) {
			baseUrl = "https://" + baseUrl;
		}
		if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}

		String unique = UUID.randomUUID().toString();

		return baseUrl + unique;
	}

}
