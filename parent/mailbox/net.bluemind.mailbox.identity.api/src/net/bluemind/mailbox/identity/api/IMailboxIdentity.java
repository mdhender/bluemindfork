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
package net.bluemind.mailbox.identity.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/mailboxes/{domainUid}/identity/{mboxUid}")
public interface IMailboxIdentity {

	/**
	 * Create an {@link Identity}. An {@link Identity} can be used by a user to
	 * set the from header in a mail and add a signature.
	 * 
	 * @param id
	 * @param identity
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String id, Identity identity) throws ServerFault;

	/**
	 * Update an existing {@link Identity}. An {@link Identity} can be used by a
	 * user to set the from header in a mail and add a signature.
	 * 
	 * @param id
	 * @param identity
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String id, Identity identity) throws ServerFault;

	/**
	 * Delete an existing {@link Identity}.
	 * 
	 * @param id
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String id) throws ServerFault;

	/**
	 * Retrieve an existing {@link Identity}
	 * 
	 * @param id
	 * @return
	 */
	@GET
	@Path("{uid}")
	public Identity get(@PathParam("uid") String id) throws ServerFault;

	/**
	 * Retrieve mailbox {@link Identity}s
	 * 
	 * @return
	 */
	@GET
	public List<IdentityDescription> getIdentities() throws ServerFault;

	/**
	 * Retrieve all possible mailbox {@link Identity}s (for each email defined
	 * in mailbox even if no identies are defined ( if identity doesnt exists
	 * for one mail, {@link IdentityDescription#id} will be null
	 * 
	 * @return
	 */
	@GET
	@Path("_possible")
	public List<IdentityDescription> getPossibleIdentities() throws ServerFault;

}
