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
package net.bluemind.todolist.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/todolists/vtodos/{containerUid}")
public interface IVTodo {

	/**
	 * Export all {@link VTodo} from a todolist as iCalendar VTodo objects.
	 * 
	 * @see https://tools.ietf.org/html/rfc5545#section-3.6.2.
	 * @return {@link net.bluemind.core.api.Stream} of iCalendar VTodo list.
	 * @throws ServerFault common error object
	 */
	@GET
	@Produces("text/calendar")
	public Stream exportAll() throws ServerFault;

	/**
	 * Export a list of {@link VTodo} as iCalendar VTodo objects
	 * 
	 * @see https://tools.ietf.org/html/rfc5545#section-3.6.2.
	 * @param uids List of {@link ItemValue#uid} to export
	 * @return {@link net.bluemind.core.api.Stream} of iCalendar VTodo list.
	 * @throws ServerFault common error object
	 */
	@POST
	@Produces("text/calendar")
	public Stream exportTodos(List<String> uids) throws ServerFault;

	/**
	 * Imports an ICS
	 * 
	 * @param ics data in ICS format
	 * @return {@link net.bluemind.core.task.api.TaskRef} which can be used to track
	 *         this asynchronous operation
	 * @throws ServerFault common error object
	 */
	@PUT
	public TaskRef importIcs(String ics) throws ServerFault;

}
