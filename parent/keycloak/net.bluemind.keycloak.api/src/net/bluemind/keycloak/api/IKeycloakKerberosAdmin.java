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
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/keycloak_kerberos/{domainId}")
public interface IKeycloakKerberosAdmin {
	@PUT
	public void create(KerberosComponent component) throws ServerFault;
	
	@GET
	public List<KerberosComponent> allKerberosProviders() throws ServerFault;
	
	@GET
	@Path("{componentName}")
	public KerberosComponent getKerberosProvider(@PathParam(value = "componentName") String componentName) throws ServerFault;
	
	@DELETE
	@Path("{componentName}")
	public void deleteKerberosProvider(@PathParam(value = "componentName") String componentName) throws ServerFault;
}
