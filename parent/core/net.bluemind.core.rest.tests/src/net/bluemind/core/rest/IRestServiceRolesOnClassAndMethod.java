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
package net.bluemind.core.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.RequiredRoles;
import net.bluemind.core.api.fault.ServerFault;

@Path("/test")
@BMApi(version = "3")
@RequiredRoles({ "canExecute1", "canRead2" })
public interface IRestServiceRolesOnClassAndMethod {

	@GET
	@Path("{before}/foo")
	@RequiredRoles({ "canDo1", "canDo2" })
	public String foo(@PathParam("bar") String bar) throws ServerFault;

	@GET
	@Path("{before}/bar")
	public String bar(@PathParam("bar") String bar) throws ServerFault;

}
