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
package net.bluemind.system.ldap.importation.api;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/ldapimport")
public interface ILdapImport {

	/**
	 * Test LDAP parameters from core service
	 * 
	 * @param hostname
	 * @param protocol
	 * @param allCertificate
	 *            accept all certificate if true
	 * @param baseDn
	 * @param loginDn
	 * @param password
	 * @param userFilter
	 * @param groupFilter
	 * @throws ServerFault
	 */
	@GET
	@Path("_testparameters")
	public void testParameters(@QueryParam(value = "hostname") String hostname,
			@QueryParam(value = "protocol") String protocol,
			@QueryParam(value = "allCertificate") String allCertificate, @QueryParam(value = "basedn") String baseDn,
			@QueryParam(value = "logindn") String loginDn, @QueryParam(value = "password") String password,
			@QueryParam(value = "userfilter") String userFilter, @QueryParam(value = "groupfilter") String groupFilter)
					throws ServerFault;

	@POST
	@Path("{uid}/_fullsync")
	public void fullSync(@PathParam("uid") String domainUid) throws ServerFault;
}
