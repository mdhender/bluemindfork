/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.externaluser.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.group.api.IGroupMember;

/**
 * ExternalUser API. The main use case for this kind of
 * {@link net.bluemind.directory.api.DirEntry} entity is to add an external
 * email to a group.
 */
@BMApi(version = "3")
@Path("/externaluser/{domainUid}")
public interface IExternalUser extends IRestoreCrudSupport<ExternalUser>, IGroupMember {

	/**
	 * Creates a new {@link ExternalUser}.
	 * 
	 * @param uid          {@link ExternalUser}'s unique id
	 * @param externalUser {@link ExternalUser}
	 * @throws ServerFault standard error object
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String uid, ExternalUser externalUser) throws ServerFault;

	/**
	 * Creates a new {@link ExternalUser} with the given uid. Associates an external
	 * id to the {@link ExternalUser}.
	 * 
	 * @param uid          the {@link ExternalUser}'s unique id
	 * @param extId        an external id. Usually used to link the
	 *                     {@link ExternalUser} to an external system
	 * @param externalUser {@link ExternalUser}
	 * @throws ServerFault standard error object
	 */
	@PUT
	@Path("{uid}/{extid}/createwithextid")
	public void createWithExtId(@PathParam(value = "uid") String uid, @PathParam(value = "extid") String extId,
			ExternalUser externalUser) throws ServerFault;

	/**
	 * Modify an existing external user.
	 * 
	 * @param uid          {@link ExternalUser}'s unique id
	 * @param externalUser updated {@link ExternalUser}
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String uid, ExternalUser externalUser) throws ServerFault;

	/**
	 * Delete an external user.
	 * 
	 * @param uid {@link ExternalUser}'s unique id
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Fetch a {@link ExternalUser} by its uid.
	 * 
	 * @param uid {@link ExternalUser}'s unique id
	 * @return {@link ExternalUser}
	 *         {@link net.bluemind.core.container.api.ItemValue}, or null if the
	 *         {@link ExternalUser} does not exist
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<ExternalUser> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetch a {@link ExternalUser} by its external id.
	 * 
	 * @param extId the external user's external id. Usually used to link the
	 *              {@link ExternalUser} to an external system
	 * @return {@link ExternalUser}
	 *         {@link net.bluemind.core.container.api.ItemValue}, or null if the
	 *         {@link ExternalUser} does not exist
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("byExtId/{extid}")
	public ItemValue<ExternalUser> byExtId(@PathParam(value = "extid") String extId) throws ServerFault;

}
