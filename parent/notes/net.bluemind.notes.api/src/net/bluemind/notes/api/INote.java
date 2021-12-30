/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ICrudByIdSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;

/**
 * 
 * Notes API. All methods work on Notes in a specific container identified by a
 * unique UID. Use {@link net.bluemind.core.container.api.IContainers#all} to
 * lookup all containers of specific type.
 * 
 */
@BMApi(version = "3")
@Path("/notes/{containerUid}")
public interface INote
		extends IChangelogSupport, ICountingSupport, ICrudByIdSupport<VNote>, ISortingSupport, IDataShardSupport {

	/**
	 * List all Notes of a container
	 * 
	 * @return All {@link VNote} of the container
	 * @throws ServerFault common error object
	 */
	@GET
	public List<ItemValue<VNote>> all() throws ServerFault;

	/**
	 * Creates a new {@link VNote} entry.
	 * 
	 * @param uid  Unique entry UID
	 * @param note {@link VNote} values
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, VNote note) throws ServerFault;

	/**
	 * Creates a new {@link VNote} entry from an {@link ItemValue}.
	 * 
	 * @param noteItem {@link VNote} {@link ItemValue}.
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("_createWithItem")
	public void createWithItem(ItemValue<VNote> noteItem) throws ServerFault;

	/**
	 * Modifies an existing {@link VNote}.
	 * 
	 * @param uid  Unique entry UID
	 * @param note {@link VNote} values
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, VNote note) throws ServerFault;

	/**
	 * Modifies an existing {@link VNote} entry from an {@link ItemValue}.
	 * 
	 * @param noteItem {@link VNote} {@link ItemValue}.
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("_updateWithItem")
	public void updateWithItem(ItemValue<VNote> noteItem) throws ServerFault;

	/**
	 * Fetch a {@link VNote} by its unique UID
	 * 
	 * @param uid Unique entry UID
	 * @return {@link net.bluemind.core.container.model.ItemValue} containing a
	 *         {@link VNote}
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<VNote> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetch multiple {@link VNote}s by their unique UIDs
	 * 
	 * @param uids list of unique UIDs
	 * @return list of {@link net.bluemind.core.container.model.ItemValue}s
	 *         containing {@link VNote}s
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<VNote>> multipleGet(List<String> uids) throws ServerFault;

	/**
	 * Fetch multiple {@link VNote}s by their unique IDs
	 * 
	 * @param ids list of unique IDs
	 * @return list of {@link net.bluemind.core.container.model.ItemValue}s
	 *         containing {@link VNote}s
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_mgetById")
	public List<ItemValue<VNote>> multipleGetById(List<Long> ids) throws ServerFault;

	/**
	 * Delete a {@link VNote}
	 * 
	 * @param uid unique UID
	 * @throws ServerFault common error object
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Delete all {@link VNote}s of this user
	 * 
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_reset")
	void reset() throws ServerFault;

	/**
	 * Updates multiple {@link VNote}s.
	 * 
	 * @param changes {@link VNoteChanges} containing the requested updates
	 * @return {@link net.bluemind.core.container.model.ContainerUpdatesResult}
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("_mupdates")
	public ContainerUpdatesResult updates(VNoteChanges changes) throws ServerFault;

	/**
	 * Get {@link net.bluemind.core.container.model.ItemValue} containing a
	 * {@link VNote} by its internal id
	 * 
	 * @param id internal id
	 * @return Matching {@link net.bluemind.core.container.model.ItemValue}
	 *         containing a {@link VNote}
	 */
	@GET
	@Path("{id}/completeById")
	ItemValue<VNote> getCompleteById(@PathParam("id") long id);

	/**
	 * Update a {@link VNote}
	 * 
	 * @param id    internal id
	 * @param value {@link VNote}
	 * 
	 * @return {@link net.bluemind.core.container.api.Ack} containing the new
	 *         version number
	 */
	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, VNote value);

	/**
	 * Create a {@link VNote}
	 * 
	 * @param id    internal id
	 * @param value {@link VNote}
	 * 
	 * @return {@link net.bluemind.core.container.api.Ack} containing the new
	 *         version number
	 */
	@PUT
	@Path("id/{id}")
	Ack createById(@PathParam("id") long id, VNote value);

	/**
	 * Delete a {@link VNote}
	 * 
	 * @param id internal id
	 */
	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	/**
	 * Retrieve all {@link VNote} UIDs of this user
	 * 
	 * @return List of UIDs
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_all")
	List<String> allUids() throws ServerFault;

	/**
	 * Get a sorted list (IDs according to the sorted list of items) of internal IDs
	 * 
	 * @param {@link net.bluemind.core.container.model.SortDescriptor}
	 * @return List of internal IDs
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_sorted")
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault;

	/**
	 * Search {@link VNote}'s by {@link VNoteQuery}
	 * 
	 * @param query {@link VNoteQuery}
	 * @return {@link net.bluemind.core.api.ListResult} of the matching
	 *         {@link net.bluemind.core.container.model.ItemValue}s containing a
	 *         {@link VNote}
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_search")
	public ListResult<ItemValue<VNote>> search(VNoteQuery query) throws ServerFault;

}
