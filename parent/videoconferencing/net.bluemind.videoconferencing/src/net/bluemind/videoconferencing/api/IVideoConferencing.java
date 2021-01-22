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
package net.bluemind.videoconferencing.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.icalendar.api.ICalendarElement;

@BMApi(version = "3")
@Path("/videoconferencing/{domainUid}")
public interface IVideoConferencing {

	/**
	 * Add videoconferencing data to the
	 * {@link net.bluemind.icalendar.api.ICalendarElement}
	 * 
	 * @param vevent
	 * @return
	 */
	@PUT
	public ICalendarElement add(ICalendarElement vevent);

	/**
	 * Remove videoconferencing data from the
	 * {@link net.bluemind.icalendar.api.ICalendarElement}
	 * 
	 * @param vevent
	 * @return
	 */
	@DELETE
	public ICalendarElement remove(ICalendarElement vevent);
}
