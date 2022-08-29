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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;

/**
 * 
 * Todolist management API. See {@link ITodoUids} on how to create the unique
 * UIDs.
 * 
 */
@BMApi(version = "3")
@Path("/todolists")
public interface ITodoLists {

	/**
	 * Create a todolist + auto-subscribe + set ALL necessary Access-Control-List
	 * entries
	 * 
	 * @param uid        Unique UID according to {@link ITodoUids}
	 * @param descriptor Todolist values
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String uid, ContainerDescriptor descriptor) throws ServerFault;

	/**
	 * Delete a Todolist and all containing {@link IVTodo}s
	 * 
	 * @param uid Unique UID
	 * @throws ServerFault common error object
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Returns a {@link ListResult} of {@link ItemValue}s containing {@link VEvent}s
	 * 
	 * @param query {@link TodoListsVTodoQuery}
	 * @return {@link ListResult} of {@link ItemValue}s containing {@link VEvent}s
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_search")
	public List<ItemContainerValue<VTodo>> search(TodoListsVTodoQuery query) throws ServerFault;

}
