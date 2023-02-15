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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

/**
 * 
 * ISmimeCRL API Used to verify end-user certificate is not revoked by
 * authorities
 * 
 */
@BMApi(version = "3")
@Path("/smime_crl/{domainUid}")
public interface ISmimeCRL {

	/**
	 * Check if a certificate serialNumber is revoked
	 * 
	 * @param serialNumber the certificate serial number
	 * @return {@link RevocationResult}
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("is_revoked")
	public RevocationResult isRevoked(@QueryParam("serial") String serialNumber) throws ServerFault;

}
