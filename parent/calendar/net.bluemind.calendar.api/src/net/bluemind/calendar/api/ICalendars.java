/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.calendar.api;

import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3")
@Path("/calendars")
public interface ICalendars {

	/**
	 * @param dtalarm
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_reminder")
	public List<Reminder> getReminder(BmDateTime dtalarm) throws ServerFault;

	/**
	 * Returns a {@link ListResult} of {@link ItemValue} of {@link VEvent}
	 * 
	 * @param query
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_search")
	public List<ItemContainerValue<VEventSeries>> search(CalendarsVEventQuery query) throws ServerFault;
}
