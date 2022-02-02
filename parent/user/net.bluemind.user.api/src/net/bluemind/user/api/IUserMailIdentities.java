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
package net.bluemind.user.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.IdentityDescription;

@BMApi(version = "3")
@Path("/users/{domainUid}/{userUid}/identity")
public interface IUserMailIdentities extends IRestoreCrudSupport<UserMailIdentity> {

	public static final String REPAIR_OP = "identities";

	/**
	 * Create an {@link Identity}. An {@link UserMailIdentity} can be used by a user
	 * to set the from header in a mail and add a signature.
	 * 
	 * @param id
	 * @param identity
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String id, UserMailIdentity identity) throws ServerFault;

	/**
	 * Update an existing {@link UserMailIdentity}. An {@link UserMailIdentity} can
	 * be used by a user to set the from header in a mail and add a signature.
	 * 
	 * @param id
	 * @param identity
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String id, UserMailIdentity identity) throws ServerFault;

	/**
	 * Delete an existing {@link UserMailIdentity}.
	 * 
	 * @param id
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String id) throws ServerFault;

	/**
	 * Retrieve an existing {@link UserMailIdentity}
	 * 
	 * @param id
	 * @return
	 */
	@GET
	@Path("{uid}")
	public UserMailIdentity get(@PathParam("uid") String id) throws ServerFault;

	@POST
	@Path("{uid}/_asdefault")
	public void setDefault(@PathParam("uid") String id) throws ServerFault;

	/**
	 * Retrieve user {@link UserMailIdentity}s
	 * 
	 * @return
	 */
	@GET
	public List<IdentityDescription> getIdentities() throws ServerFault;

	/**
	 * Retrieve mailbox {@link Identity}s
	 * 
	 * @return
	 */
	@GET
	@Path("_available")
	public List<IdentityDescription> getAvailableIdentities() throws ServerFault;

}
