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
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ICrudByIdSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;

/**
 * 
 * Notes API. All methods work on Notes in a specific container identified by a
 * unique UID. Use {@link net.bluemind.core.container.api.IContainers#all} to
 * lookup all containers of specific type.
 * 
 */
@BMApi(version = "3", genericType = VNote.class)
@Path("/notes/{containerUid}")
public interface INote extends IChangelogSupport, ICountingSupport, ICrudByIdSupport<VNote>, ISortingSupport,
		IDataShardSupport, IRestoreCrudSupport<VNote> {

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
	 * Retrieve all {@link VNote} UIDs of this user
	 * 
	 * @return List of UIDs
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_all")
	List<String> allUids() throws ServerFault;

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
