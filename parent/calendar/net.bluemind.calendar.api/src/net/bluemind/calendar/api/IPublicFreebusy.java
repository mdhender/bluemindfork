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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/calendars/sfreebusy")
public interface IPublicFreebusy {

	@GET
	@Path("{email}")
	@Produces("text/calendar")
	public String simple(@PathParam("email") String email, @QueryParam("callerUserUid") String callerUserUid,
			@QueryParam("callerDomain") String callerDomain) throws ServerFault;

	/**
	 * Export the free/busy information
	 * 
	 * @param query
	 *            {@link VFreebusyQuery}
	 * @return the free/busy information as {@link VFreebusy}
	 * @throws ServerFault
	 */
	@POST
	@Path("{email}")
	public VFreebusy get(@PathParam("email") String email, @QueryParam("callerUserUid") String callerUserUid,
			@QueryParam("callerDomain") String callerDomain, VFreebusyQuery query) throws ServerFault;

	/**
	 * Export the free/busy information
	 * 
	 * @param query
	 *            {@link VFreebusyQuery}
	 * @return the free/busy information as String
	 * @throws ServerFault
	 */
	@POST
	@Path("{email}/_ics")
	public String getAsString(@PathParam("email") String email, @QueryParam("callerUserUid") String callerUserUid,
			@QueryParam("callerDomain") String callerDomain, VFreebusyQuery query) throws ServerFault;

}
