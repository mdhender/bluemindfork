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
import java.util.Set;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;

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
	 * @param clients the client certificates {@link SmimeCertClient}
	 * @return {@link RevocationResult} lists
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("revoked_clients")
	public Set<RevocationResult> areRevoked(List<SmimeCertClient> clients) throws ServerFault;

	/**
	 * Refresh revocated certificates
	 * 
	 * @throws ServerFault
	 */
	@PUT
	@Path("refresh_domain")
	public void refreshDomainRevocations() throws ServerFault;

	/**
	 * Refresh revocated certificates
	 * 
	 * @param cacertUid {@link SmimeCacert} uid
	 * 
	 * @throws ServerFault
	 */
	@PUT
	@Path("refresh/{uid}")
	public void refreshRevocations(@PathParam("uid") String cacertUid) throws ServerFault;

	/**
	 * Get {@link SmimeRevocation} for a S/MIME CA certificate
	 * 
	 * @param cacert the S/MIME CA certificate {@link SmimeCacert}
	 * @return {@link SmimeRevocation} lists
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("fetch")
	public List<SmimeRevocation> fetch(ItemValue<SmimeCacert> cacert) throws ServerFault;

}
