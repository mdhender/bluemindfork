/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.keycloak.api;

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
@Path("/keycloak_client/{domainId}")
public interface IKeycloakClientAdmin {

	@PUT
	public void create(@QueryParam(value = "clientId") String clientId) throws ServerFault;

	@GET
	@Path("{clientId}/secret")
	public String getSecret(@PathParam(value = "clientId") String clientId) throws ServerFault;

	@GET
	public List<OidcClient> allOidcClients() throws ServerFault;
	
	@GET
	@Path("{clientId}")
	public OidcClient getOidcClient(@PathParam(value = "clientId") String clientId) throws ServerFault;
	
	@DELETE
	@Path("{clientId}")
	public void deleteOidcClient(@PathParam(value = "clientId") String clientId) throws ServerFault;
}
