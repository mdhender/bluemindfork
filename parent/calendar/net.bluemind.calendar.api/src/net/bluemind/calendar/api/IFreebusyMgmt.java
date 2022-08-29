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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/mgmt/freebusy/{containerUid}")
public interface IFreebusyMgmt {

	/**
	 * @param calendar
	 * @throws ServerFault
	 */
	@PUT
	@Path("{calendar}")
	public void add(@PathParam("calendar") String calendar) throws ServerFault;

	/**
	 * @param calendar
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{calendar}")
	public void remove(@PathParam("calendar") String calendar) throws ServerFault;

	/**
	 * @return
	 * @throws ServerFault
	 */
	@GET
	public List<String> get() throws ServerFault;

	/**
	 * @param calendars
	 * @throws ServerFault
	 */
	@POST
	public void set(List<String> calendars) throws ServerFault;

}
