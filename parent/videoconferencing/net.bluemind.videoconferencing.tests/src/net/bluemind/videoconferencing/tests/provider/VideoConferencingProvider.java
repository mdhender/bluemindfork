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
package net.bluemind.videoconferencing.tests.provider;

import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.videoconferencing.api.IVideoConferencing;

public class VideoConferencingProvider implements IVideoConferencing {

	@Override
	public String id() {
		return "this-is-video-conferencing";
	}

	@Override
	public String name() {
		return "Video Conferenring";
	}

	@Override
	public ICalendarElement add(ICalendarElement vevent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICalendarElement remove(ICalendarElement vevent) {
		// TODO Auto-generated method stub
		return null;
	}

}
