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

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/calendars/vevent/{containerUid}")
public interface IVEvent {

	/**
	 * Exports an ICS version of a {@link VEvent}
	 * 
	 * @param uid
	 *                the {@link VEvent} uid to export
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}")
	public String exportIcs(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Imports an ICS
	 * 
	 * @param ics
	 * @return
	 * @throws ServerFault
	 */
	@PUT
	public TaskRef importIcs(Stream ics) throws ServerFault;

	/**
	 * Sync an ICS.
	 * 
	 * @param stream
	 *                   ics content
	 * @return {@link net.bluemind.core.task.api.TaskRef} indicating the import
	 *         process
	 * @throws ServerFault
	 *                         common error object
	 */
	@PUT
	@Path("_sync")
	public TaskRef syncIcs(Stream stream) throws ServerFault;

	/**
	 * Exports all the {@link VEvent} of the container
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	public Stream exportAll() throws ServerFault;

}