/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.user.api.UserAccount;

@BMApi(version = "3")
@Path("/system/external")
public interface IExternalSystem {

	@GET
	public List<ExternalSystem> getExternalSystems() throws ServerFault;

	@GET
	@Path("{systemIdentifier}")
	public ExternalSystem getExternalSystem(@PathParam(value = "systemIdentifier") String systemIdentifier)
			throws ServerFault;

	@GET
	@Path("{systemIdentifier}/_logo")
	public byte[] getLogo(@PathParam(value = "systemIdentifier") String systemIdentifier) throws ServerFault;

	@POST
	@Path("{systemIdentifier}/_test_connection")
	public ConnectionTestStatus testConnection(@PathParam(value = "systemIdentifier") String systemIdentifier,
			UserAccount account);

}
