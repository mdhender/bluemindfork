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
package net.bluemind.authentication.api;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/auth/keys")
public interface IAPIKeys {

	/**
	 * Creates an {@link APIKey} for the given core session. This will be usable as
	 * a valid password to impersonate the user.
	 * 
	 * @param displayName the name of the {@link APIKey}
	 * @return an {@link APIKey} to impersonate the user
	 * @throws ServerFault
	 */
	@PUT
	public APIKey create(@QueryParam("displayName") String displayName) throws ServerFault;

	/**
	 * Deletes an {@link APIKey}
	 * 
	 * @param sid the {@link APIKey#sid}
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{sid}")
	public void delete(@PathParam("sid") String sid) throws ServerFault;

	/**
	 * @return a List of {@link APIKey}
	 * @throws ServerFault
	 */
	@GET
	public List<APIKey> list() throws ServerFault;

	@GET
	@Path("{sid}")
	public APIKey get(@PathParam("sid") String sid) throws ServerFault;

}
