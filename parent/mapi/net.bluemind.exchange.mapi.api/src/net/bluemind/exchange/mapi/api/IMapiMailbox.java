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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemUri;

@BMApi(version = "3")
@Path("/mapi/{domainUid}/{mailboxUid}")
public interface IMapiMailbox {

	@PUT
	public void create(MapiReplica mailbox) throws ServerFault;

	@POST
	@Path("_check")
	public void check();

	@GET
	MapiReplica get() throws ServerFault;

	@DELETE
	void delete() throws ServerFault;

	@POST
	@Path("_logging")
	void enablePerUserLog(boolean enable);

	/**
	 * Finds an item's uid & container with its item_id.
	 * 
	 * @param itemId
	 * @return
	 */
	@GET
	@Path("{itemId}")
	ItemUri locate(@PathParam("itemId") long itemId);

}
