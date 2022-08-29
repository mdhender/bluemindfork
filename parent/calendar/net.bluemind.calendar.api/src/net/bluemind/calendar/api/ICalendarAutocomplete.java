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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.acl.Verb;

@BMApi(version = "3")
@Path("/calendar/autocomplete")
public interface ICalendarAutocomplete {

	@GET
	@Path("_calendarsGroupLookup/{groupUid}")
	public List<CalendarLookupResponse> calendarGroupLookup(@PathParam(value = "groupUid") String groupUid)
			throws ServerFault;

	/**
	 * Lookup for calendar container
	 * 
	 * @param pattern
	 *            the search pattern
	 * @param verb
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_calendarLookup/{pattern}")
	public List<CalendarLookupResponse> calendarLookup(@PathParam(value = "pattern") String pattern, Verb verb)
			throws ServerFault;

}
