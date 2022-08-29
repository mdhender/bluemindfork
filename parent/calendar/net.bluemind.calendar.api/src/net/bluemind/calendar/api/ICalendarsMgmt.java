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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.task.api.TaskRef;

/**
 * Calendars management api
 *
 */
@BMApi(version = "3")
@Path("/mgmt/calendars")
public interface ICalendarsMgmt extends IRestoreCrudSupport<CalendarDescriptor> {

	@POST
	@Path("_reindex")
	/**
	 * reindex all calendars (drop current index and recreate them)
	 * 
	 * @return
	 * @throws ServerFault
	 */
	public TaskRef reindexAll() throws ServerFault;

	/**
	 * reindex a calendar
	 * 
	 * @param calUid
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{containerUid}/_reindex")
	public TaskRef reindex(@PathParam("containerUid") String calUid) throws ServerFault;

	@Path("{uid}")
	@GET
	public CalendarDescriptor getComplete(@PathParam("uid") String uid) throws ServerFault;

	@Path("{uid}")
	@PUT
	public void create(@PathParam("uid") String uid, CalendarDescriptor descriptor) throws ServerFault;

	@Path("{uid}")
	@POST
	public void update(@PathParam("uid") String uid, CalendarDescriptor descriptor) throws ServerFault;

	@Path("{uid}")
	@DELETE
	public void delete(@PathParam("uid") String uid) throws ServerFault;

}