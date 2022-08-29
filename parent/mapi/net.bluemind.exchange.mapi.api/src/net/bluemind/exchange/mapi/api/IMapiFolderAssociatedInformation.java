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
package net.bluemind.exchange.mapi.api;

import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3")
@Path("/mapi_fai/{replicaUid}")
public interface IMapiFolderAssociatedInformation extends IDataShardSupport {

	/**
	 * Creates or updates an FAI with the given globalCounter (itemId in bm)
	 * 
	 * @param gc  to itemId to update/assign
	 * @param fai
	 * @return
	 * @throws ServerFault
	 */
	@PUT
	@Path("{globalCounter}")
	ItemValue<MapiFAI> store(@PathParam("globalCounter") long gc, MapiFAI fai) throws ServerFault;

	/**
	 * Creates an FAI
	 * 
	 * @param fai
	 * @return
	 * @throws ServerFault
	 */
	@PUT
	@Path("_preload")
	void preload(MapiFAI fai) throws ServerFault;

	/**
	 * Fetches all the FAIs for a given {@link MapiFAI#id}
	 * 
	 * @param id the folder id
	 * @return the values of FAIs
	 * @throws ServerFault
	 */
	@GET
	@Path("folder/{folderId}")
	List<ItemValue<MapiFAI>> getByFolderId(@PathParam("folderId") String identifier) throws ServerFault;

	/**
	 * Tries to batch delete all the FAIs with the given internal ids.
	 * 
	 * Returns a list of the ids we really deleted.
	 * 
	 * @param internalIds
	 * @return what was deleted
	 * @throws ServerFault
	 */
	@POST
	@Path("_mdelete")
	Collection<Long> deleteByIds(Collection<Long> internalIds) throws ServerFault;

	@DELETE
	@Path("_deleteall")
	void deleteAll() throws ServerFault;

	@GET
	@Path("_all")
	List<ItemValue<MapiFAI>> all() throws ServerFault;

}
