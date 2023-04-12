/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.keycloak.api;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/keycloak_flow/{domainId}")
public interface IKeycloakFlowAdmin {

	@PUT
	public void createByCopying(@QueryParam(value = "flowToCopyAlias") String flowToCopyAlias,
			@QueryParam(value = "newFlowAlias") String newFlowAlias) throws ServerFault;

	@GET
	@Path("{flowAlias}")
	public AuthenticationFlow getAuthenticationFlow(@PathParam(value = "flowAlias") String flowAlias)
			throws ServerFault;

	@DELETE
	@Path("{flowAlias}")
	public void deleteFlow(@PathParam(value = "flowAlias") String flowAlias) throws ServerFault;
}