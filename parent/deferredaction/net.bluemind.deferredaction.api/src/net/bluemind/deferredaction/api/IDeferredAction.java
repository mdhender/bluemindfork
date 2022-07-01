/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.deferredaction.api;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.model.ItemValue;

/**
 * Manages {@link DeferredAction}'s which describe an action which will be
 * executed at a specific date.
 * 
 * This api works on a specific container exposed by
 * {@link IDeferredActionContainerUids}
 */
@BMApi(version = "3")
@Path("/deferredaction/{containerUid}")
public interface IDeferredAction extends IChangelogSupport, IDataShardSupport {

	/**
	 * Creates a new {@link DeferredAction} with the given uid.
	 * 
	 * @param uid            the deferred action's unique id
	 * @param deferredAction deferred action data
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, DeferredAction deferredAction) throws ServerFault;

	/**
	 * Updates a {@link DeferredAction}.
	 * 
	 * @param uid            the deferred action's unique id
	 * @param deferredAction deferred action data
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, DeferredAction deferredAction) throws ServerFault;

	/**
	 * Deletes a {@link DeferredAction}.
	 * 
	 * @param uid the deferred action's unique id
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Deletes all {@link DeferredAction}.
	 * 
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@DELETE
	@Path("_deleteAll")
	public void deleteAll() throws ServerFault;

	/**
	 * Fetches a {@link DeferredAction}.
	 * 
	 * @param uid the deferred action's unique id
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}")
	public DeferredAction get(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetches a {@link DeferredAction} item
	 * 
	 * @param uid the deferred action's unique id
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<DeferredAction> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetches a list of all {@link DeferredAction}s matching an actionId and period
	 * of time
	 * 
	 * @param actionId the actionId
	 * @param from     fetches actions having date > from
	 * @param to       fetches actions having date < to
	 * 
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{actionId}/_byaction")
	public List<ItemValue<DeferredAction>> getByActionId(@PathParam(value = "actionId") String actionId,
			@QueryParam(value = "to") Long to) throws ServerFault;

	/**
	 * Fetches a list of all {@link DeferredAction}s matching a reference
	 * 
	 * @param reference the reference
	 * 
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{reference}/_byreference")
	public List<ItemValue<DeferredAction>> getByReference(@PathParam(value = "reference") String reference)
			throws ServerFault;

	/**
	 * Fetch multiple {@link DeferredAction}s by their uids.
	 * 
	 * @param uids the unique identifiers to fetch
	 * @return a list of {@link ItemValue<DeferredACtion>}
	 * @throws ServerFault If anything goes wrong
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<DeferredAction>> multipleGet(List<String> uids) throws ServerFault;
}
