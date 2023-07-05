/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/keycloak")
public interface IKeycloakAdmin {

	@GET
	public List<Realm> allRealms() throws ServerFault;

	@GET
	@Path("{domainId}")
	public Realm getRealm(@PathParam(value = "domainId") String domainId) throws ServerFault;

	@PUT
	@Path("{domainId}")
	public void createRealm(@PathParam(value = "domainId") String domainId) throws ServerFault;

	@DELETE
	@Path("{domainId}")
	public void deleteRealm(@PathParam(value = "domainId") String domainId) throws ServerFault;

	@POST
	@Path("{domainId}")
	public void initForDomain(@PathParam(value = "domainId") String domainId) throws ServerFault;

}
