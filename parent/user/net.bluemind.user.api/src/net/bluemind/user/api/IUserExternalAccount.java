/*BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

/**
 * {@link UserAccount} API.
 * 
 * Handles external user accounts. [uid] corresponds to an user uid.
 * [systemIdentifier] is used to identify the external system and is usually
 * predetermined by the corresponding plugin.
 */

@BMApi(version = "3")
@Path("/users/{domain}/{uid}/accounts")
public interface IUserExternalAccount extends IRestoreCrudSupport<UserAccount> {

	@PUT
	@Path("{system}")
	public void create(@PathParam(value = "system") String systemIdentifier, UserAccount account) throws ServerFault;

	@POST
	@Path("{system}")
	public void update(@PathParam(value = "system") String systemIdentifier, UserAccount account) throws ServerFault;

	@GET
	@Path("{system}")
	public UserAccount get(@PathParam(value = "system") String systemIdentifier) throws ServerFault;

	@DELETE
	@Path("{system}")
	public void delete(@PathParam(value = "system") String systemIdentifier) throws ServerFault;

	@GET
	public List<UserAccountInfo> getAll() throws ServerFault;

	@DELETE
	public void deleteAll() throws ServerFault;

}
