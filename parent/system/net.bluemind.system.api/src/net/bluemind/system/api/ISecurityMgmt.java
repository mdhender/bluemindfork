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
package net.bluemind.system.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/system/security")
public interface ISecurityMgmt {
	/**
	 * Update firewall rules
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_updatefirewallrules")
	public TaskRef updateFirewallRules() throws ServerFault;

	/**
	 * Update external certificate/private key
	 * 
	 * @param certData Certificate data
	 * @throws ServerFault
	 */
	@POST
	void updateCertificate(CertData certData) throws ServerFault;

	/**
	 * Get the URL to Let's Encrypt terms of service document
	 * 
	 * @return URL
	 * @throws ServerFault
	 */
	@GET
	@Path("lets_encrypt_tos")
	String getLetsEncryptTos() throws ServerFault;

	/**
	 * Accept Let's Encrypt terms of service document
	 * 
	 * @param domainUid the domain uid
	 * @throws ServerFault
	 */
	@PUT
	@Path("lets_encrypt_tos/{uid}")
	void approveLetsEncryptTos(@PathParam("uid") String domainUid) throws ServerFault;

	/**
	 * Generate Let's Encrypt certificate
	 * 
	 * @param certData Certificate data
	 * @throws ServerFault
	 */
	@POST
	@Path("lets_encrypt")
	TaskRef generateLetsEncrypt(CertData certData) throws ServerFault;
}
