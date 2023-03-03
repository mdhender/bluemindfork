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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.smime.cacerts.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

/**
 * 
 * ISmimeRevocation API <br/>
 * Used to verify end-user certificate is not revoked by authorities
 * 
 */
@BMApi(version = "3")
@Path("/smime_revocation/{domainUid}")
public interface ISmimeRevocation {

	/**
	 * Check if a certificate serialNumber list is revoked
	 * 
	 * @param serialNumber the certificate serial number list
	 * @return {@link RevocationResult} lists
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("is_revoked")
	public List<RevocationResult> isRevoked(@QueryParam("serial") List<String> serialNumber) throws ServerFault;

}
