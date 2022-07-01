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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ICrudByIdSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;

/**
 * 
 * Todolist API. All methods work on Todos in a specific container identified by
 * a unique UID, see {@link ITodoUids}. Use
 * {@link net.bluemind.core.container.api.IContainers#all} to lookup all
 * containers of specific type.
 * 
 */
@BMApi(version = "3", genericType = VTodo.class)
@Path("/todolist/{containerUid}")
public interface ITodoList extends IChangelogSupport, ICountingSupport, ICrudByIdSupport<VTodo>, ISortingSupport,
		IDataShardSupport, IRestoreItemCrudSupport<VTodo> {

	/**
	 * List all Tasks of a Todolist container
	 * 
	 * @return All {@link VTodo} of the container
	 * @throws ServerFault common error object
	 */
	@GET
	public List<ItemValue<VTodo>> all() throws ServerFault;

	/**
	 * Creates a new {@link VTodo} entry.
	 * 
	 * @param uid  Unique entry UID
	 * @param todo {@link VTodo} values
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, VTodo todo) throws ServerFault;

	/**
	 * Modifies an existing {@link VTodo}.
	 * 
	 * @param uid  Unique entry UID
	 * @param todo {@link VTodo} values
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, VTodo todo) throws ServerFault;

	/**
	 * Fetch a {@link VTodo} by its unique UID
	 * 
	 * @param uid Unique entry UID
	 * @return {@link net.bluemind.core.container.model.ItemValue} containing a
	 *         {@link VTodo}
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<VTodo> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetch multiple {@link VTodo}s by their unique UIDs
	 * 
	 * @param uids list of unique UIDs
	 * @return list of {@link net.bluemind.core.container.model.ItemValue}s
	 *         containing {@link VTodo}s
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<VTodo>> multipleGet(List<String> uids) throws ServerFault;

	/**
	 * Delete a {@link VTodo}
	 * 
	 * @param uid unique UID
	 * @throws ServerFault common error object
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Search {@link VTodo}'s by {@link VTodoQuery}
	 * 
	 * @param query {@link VTodoQuery}
	 * @return {@link net.bluemind.core.api.ListResult} of the matching
	 *         {@link net.bluemind.core.container.model.ItemValue}s containing a
	 *         {@link VTodo}
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_search")
	public ListResult<ItemValue<VTodo>> search(VTodoQuery query) throws ServerFault;

	/**
	 * Updates multiple {@link VTodo}s.
	 * 
	 * @param changes {@link VTodoChanges} containing the requested updates
	 * @return {@link net.bluemind.core.container.model.ContainerUpdatesResult}
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("_mupdates")
	public ContainerUpdatesResult updates(VTodoChanges changes) throws ServerFault;

	/**
	 * Client/Server synchronization of {@link VTodo}s. Applies client changes and
	 * returns server updates happened since {@code since} parameter.
	 * 
	 * @param since   timestamp of the requested server updates
	 * @param changes client updates
	 * @return {@link net.bluemind.core.container.model.ContainerChangeset}
	 *         containing the server updates
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_sync")
	public ContainerChangeset<String> sync(@QueryParam("since") Long since, VTodoChanges changes) throws ServerFault;

	/**
	 * Copy {@link VTodo}s to another Todolist
	 * 
	 * @param uids             list of unique UIDs
	 * @param descContainerUid the destination Todolist container UID
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_copy/{destContainerUid}")
	public void copy(List<String> uids, @PathParam("destContainerUid") String descContainerUid) throws ServerFault;

	/**
	 * Move {@link VTodo}s to another Todolist
	 * 
	 * @param uids             list of unique UIDs
	 * @param descContainerUid the destination Todolist container UID
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_move/{destContainerUid}")
	public void move(List<String> uids, @PathParam("destContainerUid") String descContainerUid) throws ServerFault;

	/**
	 * Delete all {@link VTodo}s of this Todolist
	 * 
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_reset")
	void reset() throws ServerFault;

	/**
	 * Retrieve all {@link VTodo} UIDs of this Todolist
	 * 
	 * @return List of UIDs
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_all")
	List<String> allUids() throws ServerFault;

}
