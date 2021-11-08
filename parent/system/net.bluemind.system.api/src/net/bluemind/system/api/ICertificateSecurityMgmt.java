/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskStatus.State;

@BMApi(version = "3", internal = true)
@Path("/system/security/certificate")
public interface ICertificateSecurityMgmt {

	/**
	 * Renew domain certificate using let's encrypt
	 * 
	 * @param domainUid
	 * @param externalUrl
	 * @param contactEmail
	 */
	@PUT
	@Path("_renew/{uid}")
	public State renewLetsEncryptCertificate(@PathParam("uid") String domainUid, @QueryParam("url") String externalUrl,
			@QueryParam("email") String contactEmail);
}
