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
package net.bluemind.authentication.mgmt.api;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

/**
 * SessionsMgmt service
 *
 */
@BMApi(version = "3", internal = true)
@Path("/sessionsmgmt")
public interface ISessionsMgmt {
	/**
	 * Close all Blue-Mind sessions of requested user.
	 * <p>
	 * Only token from global domain are allowed to do this.
	 * 
	 * @param latd login at domain
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}/logout")
	public void logoutUser(@PathParam("uid") String uid);

	@GET
	@Path("list")
    public List<SessionEntry> list(@QueryParam("domain") String domainUid);
}
