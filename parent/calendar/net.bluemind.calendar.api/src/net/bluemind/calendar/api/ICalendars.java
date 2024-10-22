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

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3")
@Path("/calendars")
public interface ICalendars {

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

	/**
	 * Retrieve a list of pending counter propositions of the current user
	 * 
	 * @param calendars list of calendar uids
	 * @return list of pending counter propositions
	 * @throws ServerFault
	 */
	@POST
	@Path("_search_counters")
	public List<ItemContainerValue<VEventSeries>> searchPendingCounters(List<String> calendars) throws ServerFault;
}
